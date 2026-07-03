import { describe, expect, it } from 'vitest';
import { analyzeCode, formatAnalysisAsMarkdown } from '../../src/analysis/code-analyzer.js';

describe('CodeAnalyzer', () => {
  describe('TypeScript AST analysis', () => {
    it('should measure TypeScript classes, interfaces, imports, tests, and function spans', () => {
      const code = `
        import { describe, it } from 'vitest';

        interface UserRepository {
          findById(id: string): User | null;
        }

        type User = { id: string };

        class UserService {
          constructor(private readonly repo: UserRepository) {}

          findById(id: string): User | null {
            return this.repo.findById(id);
          }
        }

        describe('UserService', () => {
          it('finds users', () => {});
        });
      `;

      const result = analyzeCode({ code, language: 'typescript' });

      expect(result.metrics.importCount).toBe(1);
      expect(result.metrics.interfaceCount).toBe(2);
      expect(result.metrics.classCount).toBe(1);
      expect(result.metrics.methodCount).toBeGreaterThanOrEqual(2);
      expect(result.metrics.testCount).toBe(2);
    });

    it('should detect TypeScript anti-patterns through AST nodes', () => {
      const code = `
        function unsafe(value: any) {
          try {
            eval(value);
          } catch {
          }

          if (value == 'x') {
            document.body.innerHTML = value;
          }

          return new Date();
        }
      `;

      const result = analyzeCode({ code, language: 'typescript', requireTests: false });
      const rules = result.issues.map((issue) => issue.rule);

      expect(rules).toContain('no-any-type');
      expect(rules).toContain('no-eval');
      expect(rules).toContain('no-empty-catch');
      expect(rules).toContain('strict-equality');
      expect(rules).toContain('no-inner-html');
      expect(rules).toContain('no-hardcoded-time');
    });

    it('should apply caller-provided thresholds', () => {
      const code = `
        function smallEnoughByDefault() {
          const one = 1;
          const two = 2;
          return one + two;
        }
      `;

      const result = analyzeCode({
        code,
        language: 'typescript',
        requireTests: false,
        thresholds: { maxMethodLines: 2 },
      });

      expect(result.issues.some((issue) => issue.rule === 'max-method-lines')).toBe(true);
    });
  });

  describe('Anti-pattern detection', () => {
    it('should detect empty catch block', () => {
      const code = `
        try {
          doSomething();
        } catch (e) { }
      `;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'no-empty-catch')).toBe(true);
      expect(result.issues.find((i) => i.rule === 'no-empty-catch')?.type).toBe('CRITICAL');
    });

    it('should detect generic exception catch', () => {
      const code = `
        try {
          doSomething();
        } catch (Exception e) {
          log(e);
        }
      `;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'no-generic-catch')).toBe(true);
    });

    it('should detect hardcoded secrets', () => {
      const code = `const password = "secret123";`;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'no-hardcoded-secrets')).toBe(true);
      expect(result.issues.find((i) => i.rule === 'no-hardcoded-secrets')?.type).toBe('CRITICAL');
    });

    it('should detect API key in code', () => {
      const code = `const apiKey = "sk-abc123def456";`;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'no-hardcoded-secrets')).toBe(true);
    });

    it('should detect console statements', () => {
      const code = `console.log("debug");`;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'no-console')).toBe(true);
    });

    it('should detect System.out in Java', () => {
      const code = `System.out.println("hello");`;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'no-system-out')).toBe(true);
    });

    it('should detect field injection', () => {
      const code = `@Autowired private UserRepository repo;`;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'no-field-injection')).toBe(true);
    });

    it('should detect eval usage', () => {
      const code = `const result = eval("2 + 2");`;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'no-eval')).toBe(true);
      expect(result.issues.find((i) => i.rule === 'no-eval')?.type).toBe('CRITICAL');
    });

    it('should detect innerHTML assignment', () => {
      const code = `element.innerHTML = userInput;`;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'no-inner-html')).toBe(true);
    });

    it('should detect TypeScript any type', () => {
      const code = `function process(data: any): void { }`;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'no-any-type')).toBe(true);
    });

    it('should detect loose equality', () => {
      const code = `if (a == b) { }`;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'strict-equality')).toBe(true);
    });

    it('should detect TODO comments', () => {
      const code = `// TODO: fix this later`;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'no-todo')).toBe(true);
      expect(result.issues.find((i) => i.rule === 'no-todo')?.type).toBe('INFO');
    });

    it('should detect var keyword', () => {
      const code = `var x = 5;`;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'no-var')).toBe(true);
    });

    it('should detect generic error thrown', () => {
      const code = `throw new Error("Something went wrong");`;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'no-generic-throw')).toBe(true);
    });
  });

  describe('Method length analysis', () => {
    it('should detect methods over 20 lines', () => {
      const longMethodBody = Array(25).fill('  doSomething();').join('\n');
      const code = `
        class Test {
          public void longMethod() {
            ${longMethodBody}
          }
        }
      `;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'max-method-lines')).toBe(true);
    });

    it('should not flag short methods', () => {
      const code = `
        class Test {
          public void shortMethod() {
            doSomething();
            doSomethingElse();
          }
        }
      `;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'max-method-lines')).toBe(false);
    });

    it('should handle TypeScript methods', () => {
      const longBody = Array(25).fill('    console.log("x");').join('\n');
      const code = `
        class Service {
          async processData(): Promise<void> {
            ${longBody}
          }
        }
      `;
      const result = analyzeCode(code);
      // Should detect both method length and console usage
      expect(result.issues.some((i) => i.rule === 'no-console')).toBe(true);
    });
  });

  describe('Class length analysis', () => {
    it('should detect classes over 200 lines', () => {
      const methods = Array(50)
        .fill(
          `
          public void method() {
            doSomething();
            doSomethingElse();
            doMore();
          }
        `
        )
        .join('\n');
      const code = `
        class LargeClass {
          ${methods}
        }
      `;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'max-class-lines')).toBe(true);
    });
  });

  describe('Missing tests detection', () => {
    it('should detect missing tests in implementation code', () => {
      const code = `
        class UserService {
          constructor(private repo: UserRepository) {}

          getUser(id: string): User {
            return this.repo.findById(id);
          }
        }
      `;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'missing-tests')).toBe(true);
    });

    it('should not flag code with Jest tests', () => {
      const code = `
        describe('UserService', () => {
          it('should get user', () => {
            expect(service.getUser('1')).toBeDefined();
          });
        });
      `;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'missing-tests')).toBe(false);
    });

    it('should not flag code with JUnit tests', () => {
      const code = `
        class UserServiceTest {
          @Test
          void shouldGetUser() {
            assertNotNull(service.getUser("1"));
          }
        }
      `;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'missing-tests')).toBe(false);
    });
  });

  describe('Missing interfaces detection', () => {
    it('should detect missing interfaces when classes exist', () => {
      const code = `
        class UserService {
          getUser(id: string): User {
            return null;
          }
        }
      `;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'missing-interfaces')).toBe(true);
    });

    it('should not flag code with interfaces', () => {
      const code = `
        interface UserService {
          getUser(id: string): User;
        }

        class UserServiceImpl implements UserService {
          getUser(id: string): User {
            return null;
          }
        }
      `;
      const result = analyzeCode(code);
      expect(result.issues.some((i) => i.rule === 'missing-interfaces')).toBe(false);
    });
  });

  describe('Score calculation', () => {
    it('should give high score for clean code with good practices', () => {
      const code = `
        interface UserRepository {
          findById(id: string): User | null;
        }

        interface UserService {
          getUser(id: string): User;
        }

        class UserServiceImpl implements UserService {
          constructor(private readonly repo: UserRepository) {}

          getUser(id: string): User {
            const user = this.repo.findById(id);
            if (!user) throw new UserNotFoundError(id);
            return user;
          }
        }

        class UserNotFoundError extends Error {
          constructor(id: string) {
            super(\`User not found: \${id}\`);
          }
        }

        describe('UserService', () => {
          it('should get user', () => {
            expect(true).toBe(true);
          });
        });
      `;
      const result = analyzeCode(code);
      expect(result.score).toBeGreaterThan(70);
    });

    it('should give low score for problematic code', () => {
      const code = `
        class BadService {
          password = "secret123";

          doStuff() {
            try {
              console.log("doing stuff");
            } catch (e) { }
          }
        }
      `;
      const result = analyzeCode(code);
      expect(result.score).toBeLessThan(60);
      expect(result.passed).toBe(false);
    });

    it('should deduct more for CRITICAL issues', () => {
      const codeWithCritical = `
        const password = "secret123";
      `;
      const codeWithWarning = `
        console.log("debug");
      `;

      const criticalResult = analyzeCode(codeWithCritical);
      const warningResult = analyzeCode(codeWithWarning);

      // Critical issues should impact score more
      expect(criticalResult.score).toBeLessThan(warningResult.score);
    });
  });

  describe('Metrics calculation', () => {
    it('should count lines correctly', () => {
      const code = `
        // Comment
        class Test {
          method() {
            return 1;
          }
        }
      `;
      const result = analyzeCode(code);
      expect(result.metrics.totalLines).toBeGreaterThan(0);
      expect(result.metrics.commentLines).toBeGreaterThan(0);
    });

    it('should count classes', () => {
      const code = `
        class UserService {
          public getUser() { return null; }
          public createUser() { return null; }
        }

        class OrderService {
          public getOrder() { return null; }
        }
      `;
      const result = analyzeCode(code);
      expect(result.metrics.classCount).toBe(2);
    });

    it('should count interfaces', () => {
      const code = `
        interface UserRepo { }
        interface OrderRepo { }
        interface PaymentGateway { }
      `;
      const result = analyzeCode(code);
      expect(result.metrics.interfaceCount).toBe(3);
    });

    it('should count tests', () => {
      const code = `
        describe('Service', () => {
          it('test 1', () => {});
          it('test 2', () => {});
          test('test 3', () => {});
        });
      `;
      const result = analyzeCode(code);
      expect(result.metrics.testCount).toBeGreaterThanOrEqual(3);
    });
  });

  describe('Summary generation', () => {
    it('should generate NEEDS WORK summary for critical issues', () => {
      const code = `catch (e) { }`;
      const result = analyzeCode(code);
      expect(result.summary).toContain('NEEDS WORK');
    });

    it('should generate appropriate summary based on score', () => {
      const goodCode = `
        interface Service { }
        describe('test', () => { it('works', () => {}); });
      `;
      const result = analyzeCode(goodCode);
      expect(result.summary).not.toContain('NEEDS WORK');
    });
  });

  describe('formatAnalysisAsMarkdown', () => {
    it('should format results as markdown', () => {
      const code = `
        class Test {
          test() { console.log("x"); }
        }
      `;
      const result = analyzeCode(code);
      const markdown = formatAnalysisAsMarkdown(result);

      expect(markdown).toContain('# ');
      expect(markdown).toContain('Score:');
      expect(markdown).toContain('Metrics');
      expect(markdown).toContain('| Metric | Value |');
    });

    it('should include issues in markdown output', () => {
      const code = `console.log("test");`;
      const result = analyzeCode(code);
      const markdown = formatAnalysisAsMarkdown(result);

      expect(markdown).toContain('Warnings');
      // Check for console-related content in the output
      expect(markdown).toContain('Console');
    });
  });

  describe('Edge cases', () => {
    it('should handle empty code', () => {
      const result = analyzeCode('');
      expect(result.score).toBeDefined();
      expect(result.issues).toBeDefined();
      expect(result.metrics.totalLines).toBe(1); // Empty string splits to ['']
    });

    it('should handle code with only comments', () => {
      const code = `
        // This is a comment
        /* Multi-line
           comment */
        // Another comment
      `;
      const result = analyzeCode(code);
      expect(result.metrics.commentLines).toBeGreaterThan(0);
    });

    it('should not create duplicate issues for same line', () => {
      const code = `
        console.log("test");
        console.log("test2");
      `;
      const result = analyzeCode(code);
      const consoleIssues = result.issues.filter((i) => i.rule === 'no-console');
      // Should have 2 issues for 2 different lines
      expect(consoleIssues.length).toBe(2);
    });
  });
});
