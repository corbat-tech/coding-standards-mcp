import { describe, expect, it } from 'vitest';
import { handleVerify } from '../../src/tools/handlers/verify.js';

describe('verify tool', () => {
  describe('Passing scenarios', () => {
    it('should pass for code with tests and interfaces', async () => {
      const result = await handleVerify({
        code: `
          class UserServiceImpl implements UserService {
            constructor(private readonly repo: UserRepository) {}

            getUser(id: string): User {
              return this.repo.findById(id);
            }
          }
        `,
        tests: `
          describe('UserService', () => {
            it('should get user by id', () => {
              const user = service.getUser('1');
              expect(user).toBeDefined();
            });

            it('should throw when user not found', () => {
              expect(() => service.getUser('invalid')).toThrow();
            });
          });
        `,
        interfaces: `
          interface UserService {
            getUser(id: string): User;
          }

          interface UserRepository {
            findById(id: string): User | null;
          }
        `,
      });

      expect(result.content[0].text).toContain('VERIFICATION PASSED');
      expect(result.isError).toBeFalsy();
    });

    it('should pass for code with inline interfaces', async () => {
      const result = await handleVerify({
        code: `
          interface UserService {
            getUser(id: string): User;
          }

          class UserServiceImpl implements UserService {
            getUser(id: string): User {
              return { id, name: 'Test' };
            }
          }
        `,
        tests: `
          describe('UserService', () => {
            it('test 1', () => { expect(true).toBe(true); });
            it('test 2', () => { expect(true).toBe(true); });
          });
        `,
      });

      expect(result.content[0].text).toContain('VERIFICATION PASSED');
    });
  });

  describe('Failing scenarios', () => {
    it('should fail for code without tests', async () => {
      const result = await handleVerify({
        code: `
          class UserService {
            getUser(id: string): User {
              return null;
            }
          }
        `,
      });

      expect(result.content[0].text).toContain('VERIFICATION FAILED');
      expect(result.content[0].text).toContain('No tests provided');
      expect(result.isError).toBe(true);
    });

    it('should fail for empty tests string', async () => {
      const result = await handleVerify({
        code: `class Service { }`,
        tests: '   ',
      });

      expect(result.content[0].text).toContain('VERIFICATION FAILED');
      expect(result.content[0].text).toContain('No tests provided');
    });

    it('should fail for test code without actual test cases', async () => {
      const result = await handleVerify({
        code: `class Service { method() { return 1; } }`,
        tests: `// This is supposed to be tests but has no actual tests`,
      });

      expect(result.content[0].text).toContain('VERIFICATION FAILED');
      expect(result.content[0].text).toContain('no test cases detected');
    });

    it('should fail for code with critical issues', async () => {
      const result = await handleVerify({
        code: `
          class BadService {
            password = "secret123";

            doStuff() {
              try {
                process();
              } catch (e) { }
            }
          }
        `,
        tests: `
          describe('Service', () => {
            it('test', () => { expect(true).toBe(true); });
            it('test2', () => { expect(true).toBe(true); });
          });
        `,
      });

      expect(result.content[0].text).toContain('VERIFICATION FAILED');
      expect(result.isError).toBe(true);
    });

    it('should fail for very low quality score', async () => {
      const code = `
        class Bad {
          var x = "test";
          password = "secret";
          eval("code");
          doStuff() {
            console.log("x");
            try { x(); } catch (e) { }
          }
        }
      `;
      const result = await handleVerify({
        code,
        tests: `it('t', () => {}); it('t2', () => {});`,
      });

      expect(result.content[0].text).toContain('VERIFICATION FAILED');
    });
  });

  describe('Warnings', () => {
    it('should warn about missing interfaces for classes', async () => {
      const result = await handleVerify({
        code: `
          class UserService {
            getUser(id: string): User {
              return null;
            }
          }
        `,
        tests: `
          describe('UserService', () => {
            it('test 1', () => { expect(true).toBe(true); });
            it('test 2', () => { expect(true).toBe(true); });
          });
        `,
      });

      const text = result.content[0].text;
      expect(text).toContain('interface');
    });

    it('should show test count in summary', async () => {
      const result = await handleVerify({
        code: `
          interface Service { method(): void; }
          class ServiceImpl implements Service { method() {} }
        `,
        tests: `
          describe('Service', () => {
            it('test 1', () => { expect(true).toBe(true); });
            it('test 2', () => { expect(true).toBe(true); });
          });
        `,
        interfaces: `interface Service { method(): void; }`,
      });

      const text = result.content[0].text;
      // Should show test count in summary
      expect(text).toContain('Test count:');
    });

    it('should warn about any type usage', async () => {
      const result = await handleVerify({
        code: `
          interface Service { process(data: any): void; }
          class ServiceImpl implements Service {
            process(data: any): void { }
          }
        `,
        tests: `
          describe('Test', () => {
            it('t1', () => {});
            it('t2', () => {});
          });
        `,
        interfaces: `interface Service { process(data: any): void; }`,
      });

      expect(result.content[0].text).toContain('any');
    });
  });

  describe('Verification summary', () => {
    it('should include verification summary for passed code', async () => {
      const result = await handleVerify({
        code: `class Impl implements Service { }`,
        tests: `describe('x', () => { it('t', () => {}); it('t2', () => {}); });`,
        interfaces: `interface Service { }`,
      });

      const text = result.content[0].text;
      expect(text).toContain('Verification Summary');
      expect(text).toContain('Tests provided: Yes');
      expect(text).toContain('Interfaces provided: Yes');
    });

    it('should show critical issue details for failed code', async () => {
      const result = await handleVerify({
        code: `const password = "secret123";`,
        tests: `it('t', () => {}); it('t2', () => {});`,
      });

      const text = result.content[0].text;
      expect(text).toContain('Critical Code Issues');
      expect(text).toContain('secret');
    });
  });

  describe('Task type handling', () => {
    it('should accept valid task types', async () => {
      const taskTypes = ['feature', 'bugfix', 'refactor', 'test', 'security', 'performance'];

      for (const taskType of taskTypes) {
        const result = await handleVerify({
          code: `class X { }`,
          tests: `it('t', () => {}); it('t2', () => {});`,
          task_type: taskType,
        });

        // Should not throw
        expect(result.content).toBeDefined();
      }
    });
  });

  describe('Edge cases', () => {
    it('should handle code with mixed good and bad patterns', async () => {
      const result = await handleVerify({
        code: `
          interface GoodService {
            process(): void;
          }

          class GoodServiceImpl implements GoodService {
            constructor(private readonly dep: Dependency) {}

            process(): void {
              console.log("debug"); // Warning but not critical
            }
          }
        `,
        tests: `
          describe('GoodService', () => {
            it('should process', () => { expect(true).toBe(true); });
            it('should handle errors', () => { expect(true).toBe(true); });
          });
        `,
        interfaces: `interface GoodService { process(): void; }`,
      });

      // Should pass despite warning (console.log is not critical)
      expect(result.content[0].text).toContain('PASSED');
    });

    it('should handle very large code input', async () => {
      const largeCode = Array(100)
        .fill(`
          class Service${Math.random()} {
            method() { return 1; }
          }
        `)
        .join('\n');

      const result = await handleVerify({
        code: largeCode,
        tests: `
          describe('Tests', () => {
            it('test 1', () => {});
            it('test 2', () => {});
          });
        `,
      });

      // Should complete without timeout
      expect(result.content).toBeDefined();
    });
  });
});
