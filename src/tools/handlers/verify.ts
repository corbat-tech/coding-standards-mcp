import { z } from 'zod';
import { type AnalysisResult, analyzeCode } from '../../analysis/code-analyzer.js';

/**
 * Schema for verify tool input.
 */
export const VerifySchema = z.object({
  code: z.string().describe('All generated implementation code'),
  tests: z.string().optional().describe('All test code'),
  interfaces: z.string().optional().describe('All interfaces/types'),
  task_type: z.enum(['feature', 'bugfix', 'refactor', 'test', 'security', 'performance']).optional(),
});

export type VerifyInput = z.infer<typeof VerifySchema>;

/**
 * Handler for the verify tool.
 *
 * This is the "gate" that LLMs must pass before presenting code to the user.
 * It analyzes all generated code and returns PASS/FAIL with specific feedback.
 *
 * Key principles:
 * - Tests are REQUIRED (TDD compliance)
 * - Interfaces are strongly recommended (DI)
 * - Critical issues block passing
 * - Clear feedback on what to fix
 */
export async function handleVerify(
  args: Record<string, unknown>
): Promise<{ content: Array<{ type: 'text'; text: string }>; isError?: boolean }> {
  const { code, tests, interfaces } = VerifySchema.parse(args);

  // Combine all code for comprehensive analysis
  const allCodeParts: string[] = [];

  if (interfaces) {
    allCodeParts.push('// === INTERFACES ===', interfaces);
  }

  if (tests) {
    allCodeParts.push('// === TESTS ===', tests);
  }

  allCodeParts.push('// === IMPLEMENTATION ===', code);

  const allCode = allCodeParts.join('\n\n');

  // Run analysis
  const analysis = analyzeCode(allCode);

  // Additional verification checks beyond the analyzer
  const verificationResults = runVerificationChecks(code, tests, interfaces, analysis);

  // Build response
  const lines: string[] = [];

  if (verificationResults.passed) {
    lines.push('# VERIFICATION PASSED', '');
    lines.push(`**Score: ${analysis.score}/100**`, '');
    lines.push('');
    lines.push('The code meets quality standards and is ready to present to the user.', '');

    // Show any optional improvements
    if (verificationResults.warnings.length > 0) {
      lines.push('---', '', '## Optional Improvements', '');
      for (const warning of verificationResults.warnings.slice(0, 5)) {
        lines.push(`- ${warning}`);
      }
      lines.push('');
    }

    // Summary of what was verified
    lines.push('---', '', '## Verification Summary', '');
    lines.push(`- Tests provided: ${tests ? 'Yes' : 'No'}`);
    lines.push(`- Interfaces provided: ${interfaces ? 'Yes' : 'No'}`);
    lines.push(`- Critical issues: ${analysis.issues.filter((i) => i.type === 'CRITICAL').length}`);
    lines.push(`- Warnings: ${analysis.issues.filter((i) => i.type === 'WARNING').length}`);
    lines.push(`- Test count: ${analysis.metrics.testCount}`);
    lines.push(`- Interface count: ${analysis.metrics.interfaceCount}`);
    lines.push('');

    lines.push('---', '');
    lines.push('**You may now present this code to the user.**');
  } else {
    lines.push('# VERIFICATION FAILED', '');
    lines.push(`**Score: ${analysis.score}/100**`, '');
    lines.push('');
    lines.push('The code does not meet quality standards. Fix the issues below and verify again.', '');

    // Show failures (blocking issues)
    lines.push('---', '', '## Issues to Fix (Blocking)', '');
    for (const failure of verificationResults.failures) {
      lines.push(`- ${failure}`);
    }
    lines.push('');

    // Show warnings
    if (verificationResults.warnings.length > 0) {
      lines.push('---', '', '## Warnings (Should Fix)', '');
      for (const warning of verificationResults.warnings.slice(0, 8)) {
        lines.push(`- ${warning}`);
      }
      lines.push('');
    }

    // Analysis issues
    const criticals = analysis.issues.filter((i) => i.type === 'CRITICAL');
    if (criticals.length > 0) {
      lines.push('---', '', '## Critical Code Issues', '');
      for (const issue of criticals) {
        lines.push(`- **Line ${issue.line || '?'}:** ${issue.message}`);
        if (issue.suggestion) {
          lines.push(`  - Fix: ${issue.suggestion}`);
        }
      }
      lines.push('');
    }

    lines.push('---', '');
    lines.push('**Fix these issues and call `verify` again before presenting code to user.**');
  }

  return {
    content: [{ type: 'text', text: lines.join('\n') }],
    isError: !verificationResults.passed,
  };
}

/**
 * Run additional verification checks beyond the code analyzer.
 */
function runVerificationChecks(
  code: string,
  tests: string | undefined,
  interfaces: string | undefined,
  analysis: AnalysisResult
): { passed: boolean; failures: string[]; warnings: string[] } {
  const failures: string[] = [];
  const warnings: string[] = [];

  // Check 1: Tests MUST be provided (TDD requirement)
  if (!tests || tests.trim().length === 0) {
    failures.push('No tests provided - TDD requires tests before/with implementation');
  } else {
    // Check test count
    const testCount = analysis.metrics.testCount;
    if (testCount === 0) {
      failures.push('Test code provided but no test cases detected (missing @Test, it(), test(), etc.)');
    } else if (testCount < 2) {
      warnings.push(`Only ${testCount} test(s) found - consider adding more for better coverage`);
    }
  }

  // Check 2: Interfaces strongly recommended for DI
  const hasClasses = /class\s+\w+/.test(code);
  if (hasClasses) {
    if (!interfaces || interfaces.trim().length === 0) {
      // Check if interfaces are in the main code instead
      const hasInlineInterfaces = /interface\s+\w+/.test(code);
      if (!hasInlineInterfaces) {
        warnings.push('No interfaces provided - consider adding for dependency injection');
      }
    }
  }

  // Check 3: No critical issues from analysis
  const criticalCount = analysis.issues.filter((i) => i.type === 'CRITICAL').length;
  if (criticalCount > 0) {
    failures.push(`${criticalCount} critical code issue(s) detected - see details below`);
  }

  // Check 4: Score threshold
  if (analysis.score < 50) {
    failures.push(`Quality score too low (${analysis.score}/100) - must be at least 50`);
  } else if (analysis.score < 70) {
    warnings.push(`Quality score is ${analysis.score}/100 - consider improvements to reach 70+`);
  }

  // Check 5: Too many warnings
  const warningCount = analysis.issues.filter((i) => i.type === 'WARNING').length;
  if (warningCount > 10) {
    warnings.push(`High number of warnings (${warningCount}) - consider addressing some`);
  }

  // Check 6: Method/class size
  if (analysis.metrics.maxMethodLines > 30) {
    warnings.push(`Longest method is ${analysis.metrics.maxMethodLines} lines - consider refactoring`);
  }
  if (analysis.metrics.maxClassLines > 300) {
    warnings.push(`Longest class is ${analysis.metrics.maxClassLines} lines - consider splitting`);
  }

  // Check 7: Look for common issues in the code
  if (/throw\s+new\s+Error\s*\(/.test(code) && !/class\s+\w*Error/.test(code)) {
    warnings.push('Generic Error thrown without custom error classes defined');
  }

  if (/any\s*[;,)>]/.test(code)) {
    warnings.push('TypeScript "any" type usage detected - consider specific types');
  }

  // Determine if passed
  const passed = failures.length === 0;

  return { passed, failures, warnings };
}
