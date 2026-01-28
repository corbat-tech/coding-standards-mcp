/**
 * Lightweight code analyzer using regex and heuristics.
 * No AST parsing to keep it fast and dependency-free.
 *
 * This module provides real code analysis instead of just returning checklists.
 * It detects anti-patterns, measures code quality metrics, and provides
 * actionable feedback.
 */

export interface AnalysisIssue {
  type: 'CRITICAL' | 'WARNING' | 'INFO';
  rule: string;
  message: string;
  line?: number;
  suggestion?: string;
}

export interface CodeMetrics {
  totalLines: number;
  codeLines: number;
  commentLines: number;
  methodCount: number;
  classCount: number;
  interfaceCount: number;
  testCount: number;
  maxMethodLines: number;
  maxClassLines: number;
  customErrorCount: number;
  importCount: number;
}

export interface AnalysisResult {
  issues: AnalysisIssue[];
  score: number;
  summary: string;
  metrics: CodeMetrics;
  passed: boolean;
}

// Anti-patterns to detect with their severity and suggestions
const ANTI_PATTERNS: Array<{
  pattern: RegExp;
  message: string;
  rule: string;
  type: 'CRITICAL' | 'WARNING' | 'INFO';
  suggestion?: string;
}> = [
  // Critical issues
  {
    pattern: /catch\s*\(\s*\w*\s*\)\s*\{\s*\}/g,
    message: 'Empty catch block - errors are silently swallowed',
    rule: 'no-empty-catch',
    type: 'CRITICAL',
    suggestion: 'Handle the error, log it, or rethrow with context',
  },
  {
    pattern: /(password|secret|api_key|apikey|api[-_]?secret|token|credential)\s*[:=]\s*["'][^"']{3,}["']/gi,
    message: 'Potential hardcoded secret detected',
    rule: 'no-hardcoded-secrets',
    type: 'CRITICAL',
    suggestion: 'Use environment variables or a secrets manager',
  },
  {
    pattern: /eval\s*\(/g,
    message: 'eval() is dangerous and can lead to code injection',
    rule: 'no-eval',
    type: 'CRITICAL',
    suggestion: 'Use safer alternatives like JSON.parse() or specific parsers',
  },
  {
    pattern: /innerHTML\s*=/g,
    message: 'innerHTML can lead to XSS vulnerabilities',
    rule: 'no-inner-html',
    type: 'CRITICAL',
    suggestion: 'Use textContent or sanitize HTML before insertion',
  },

  // Warnings
  {
    pattern: /catch\s*\(\s*(Exception|Error|Throwable|any|unknown)\s+\w+\s*\)/gi,
    message: 'Generic exception catch - use specific error types',
    rule: 'no-generic-catch',
    type: 'WARNING',
    suggestion: 'Catch specific error types for proper error handling',
  },
  {
    pattern: /new\s+Date\(\s*\)/g,
    message: 'Hardcoded current time - not testable',
    rule: 'no-hardcoded-time',
    type: 'WARNING',
    suggestion: 'Inject a Clock/TimeProvider for testability',
  },
  {
    pattern: /console\.(log|error|warn|info|debug|trace)\s*\(/g,
    message: 'Console statement found',
    rule: 'no-console',
    type: 'WARNING',
    suggestion: 'Use a proper logging framework in production code',
  },
  {
    pattern: /System\.(out|err)\.(print|println)/g,
    message: 'System.out/err usage - use proper logging',
    rule: 'no-system-out',
    type: 'WARNING',
    suggestion: 'Use SLF4J, Log4j, or another logging framework',
  },
  {
    pattern: /@Autowired\s+(private|protected)/g,
    message: 'Field injection - harder to test',
    rule: 'no-field-injection',
    type: 'WARNING',
    suggestion: 'Use constructor injection instead',
  },
  {
    pattern: /:\s*any\b(?!\s*\()/g,
    message: 'TypeScript "any" type - loses type safety',
    rule: 'no-any-type',
    type: 'WARNING',
    suggestion: 'Use specific types or unknown with type guards',
  },
  {
    pattern: /==(?!=)/g,
    message: 'Loose equality operator',
    rule: 'strict-equality',
    type: 'WARNING',
    suggestion: 'Use === for strict comparison',
  },
  {
    pattern: /!=(?!=)/g,
    message: 'Loose inequality operator',
    rule: 'strict-inequality',
    type: 'WARNING',
    suggestion: 'Use !== for strict comparison',
  },
  {
    pattern: /public\s+\w+\s+\w+\s*;(?!\s*\/\/.*final|static)/g,
    message: 'Public mutable field - breaks encapsulation',
    rule: 'no-public-fields',
    type: 'WARNING',
    suggestion: 'Use private fields with getters/setters or records',
  },
  {
    pattern: /throw\s+new\s+(Error|Exception)\s*\(\s*["'][^"']*["']\s*\)/g,
    message: 'Generic error thrown - use custom error types',
    rule: 'no-generic-throw',
    type: 'WARNING',
    suggestion: 'Create specific error classes for different failure modes',
  },
  {
    pattern: /\.then\s*\([^)]*\)\s*(?!\.catch|\.finally)/g,
    message: 'Promise without error handling',
    rule: 'unhandled-promise',
    type: 'WARNING',
    suggestion: 'Add .catch() or use try/catch with async/await',
  },
  {
    pattern: /setTimeout\s*\(\s*[^,]+,\s*0\s*\)/g,
    message: 'setTimeout with 0ms - likely a hack',
    rule: 'no-settimeout-zero',
    type: 'WARNING',
    suggestion: 'Use queueMicrotask() or review the async flow',
  },
  {
    pattern: /magic\s*number|[^a-zA-Z](?:86400|3600|60000|1000)\b/gi,
    message: 'Magic number detected - use named constants',
    rule: 'no-magic-numbers',
    type: 'WARNING',
    suggestion: 'Extract to named constants (e.g., SECONDS_PER_DAY)',
  },

  // Info
  {
    pattern: /TODO|FIXME|HACK|XXX|BUG/g,
    message: 'Unresolved TODO/FIXME comment',
    rule: 'no-todo',
    type: 'INFO',
    suggestion: 'Track these in an issue tracker instead',
  },
  {
    pattern: /@deprecated/gi,
    message: 'Deprecated code usage',
    rule: 'deprecated-usage',
    type: 'INFO',
    suggestion: 'Update to use the recommended alternative',
  },
  {
    pattern: /\bvar\s+/g,
    message: '"var" keyword used',
    rule: 'no-var',
    type: 'INFO',
    suggestion: 'Use const or let instead for better scoping',
  },
];

// Good practices to detect (presence = positive contribution to score)
const GOOD_PRACTICES: Array<{
  pattern: RegExp;
  name: string;
  weight: number;
}> = [
  { pattern: /interface\s+\w+/g, name: 'interfaces', weight: 10 },
  { pattern: /type\s+\w+\s*=/g, name: 'type_aliases', weight: 5 },
  { pattern: /@Test|describe\s*\(|it\s*\(|test\s*\(|#\[test\]/g, name: 'tests', weight: 15 },
  { pattern: /class\s+\w*Error|class\s+\w*Exception|struct\s+\w*Error/g, name: 'custom_errors', weight: 10 },
  { pattern: /constructor\s*\([^)]*(?:private|readonly|protected)/g, name: 'constructor_injection', weight: 10 },
  { pattern: /implements\s+\w+/g, name: 'implements_interface', weight: 8 },
  { pattern: /readonly\s+|final\s+|const\s+\w+:/g, name: 'immutability', weight: 5 },
  { pattern: /@Injectable|@Service|@Component|@Repository/g, name: 'di_annotations', weight: 5 },
  { pattern: /async\s+\w+|Promise\s*</g, name: 'async_handling', weight: 3 },
  { pattern: /expect\s*\(|assert|should\.|toBe|toEqual|toHaveBeenCalled/g, name: 'assertions', weight: 5 },
  { pattern: /@BeforeEach|@AfterEach|beforeEach|afterEach|setUp|tearDown/g, name: 'test_setup', weight: 3 },
  { pattern: /private\s+readonly|private\s+final/g, name: 'encapsulation', weight: 5 },
  { pattern: /Result<|Either<|Option<|Maybe</g, name: 'result_types', weight: 8 },
  { pattern: /\.map\s*\(|\.filter\s*\(|\.reduce\s*\(/g, name: 'functional_style', weight: 3 },
];

/**
 * Analyze code and return issues, metrics, and score.
 */
export function analyzeCode(code: string): AnalysisResult {
  const lines = code.split('\n');
  const issues: AnalysisIssue[] = [];

  // 1. Detect anti-patterns
  for (const ap of ANTI_PATTERNS) {
    const regex = new RegExp(ap.pattern.source, ap.pattern.flags);
    let match: RegExpExecArray | null;

    while ((match = regex.exec(code)) !== null) {
      const lineNum = code.substring(0, match.index).split('\n').length;

      // Avoid duplicate issues at the same line for the same rule
      const existingIssue = issues.find((i) => i.rule === ap.rule && i.line === lineNum);
      if (!existingIssue) {
        issues.push({
          type: ap.type,
          rule: ap.rule,
          message: ap.message,
          line: lineNum,
          suggestion: ap.suggestion,
        });
      }
    }
  }

  // 2. Analyze method/class lengths
  const methodLengths = analyzeMethodLengths(code);
  const classLengths = analyzeClassLengths(code);

  for (const method of methodLengths) {
    if (method.lines > 20) {
      issues.push({
        type: 'WARNING',
        rule: 'max-method-lines',
        message: `Method "${method.name}" is ${method.lines} lines (max: 20)`,
        line: method.startLine,
        suggestion: 'Extract smaller methods with single responsibilities',
      });
    }
  }

  for (const cls of classLengths) {
    if (cls.lines > 200) {
      issues.push({
        type: 'WARNING',
        rule: 'max-class-lines',
        message: `Class "${cls.name}" is ${cls.lines} lines (max: 200)`,
        line: cls.startLine,
        suggestion: 'Split into smaller, focused classes',
      });
    }
  }

  // 3. Check for missing tests
  const hasImplementation = /class\s+\w+|function\s+\w+|const\s+\w+\s*=\s*(?:async\s*)?\(/.test(code);
  const hasTests = /@Test|describe\s*\(|it\s*\(|test\s*\(|#\[test\]/.test(code);

  if (hasImplementation && !hasTests) {
    issues.push({
      type: 'CRITICAL',
      rule: 'missing-tests',
      message: 'No tests found - TDD requires tests before implementation',
      suggestion: 'Write tests first, then implement to make them pass',
    });
  }

  // 4. Check for missing interfaces (DI)
  const hasClasses = /class\s+\w+/.test(code);
  const hasInterfaces = /interface\s+\w+|type\s+\w+\s*=/.test(code);

  if (hasClasses && !hasInterfaces) {
    issues.push({
      type: 'WARNING',
      rule: 'missing-interfaces',
      message: 'No interfaces found - use interfaces for dependency injection',
      suggestion: 'Define interfaces for services and repositories to enable testing and flexibility',
    });
  }

  // 5. Calculate metrics
  const metrics = calculateMetrics(code, lines, methodLengths, classLengths);

  // 6. Calculate score
  const score = calculateScore(code, issues);

  // 7. Generate summary
  const summary = generateSummary(issues, score);

  // 8. Determine if passed
  const criticalCount = issues.filter((i) => i.type === 'CRITICAL').length;
  const passed = criticalCount === 0 && score >= 60;

  return { issues, score, summary, metrics, passed };
}

/**
 * Analyze method lengths in the code.
 */
function analyzeMethodLengths(code: string): Array<{ name: string; lines: number; startLine: number }> {
  const results: Array<{ name: string; lines: number; startLine: number }> = [];
  const lines = code.split('\n');

  // Patterns for method/function declarations in various languages
  const methodPatterns = [
    // TypeScript/JavaScript: function name(), async function name(), name() {, async name() {
    /(?:export\s+)?(?:async\s+)?function\s+(\w+)\s*\(/g,
    // Method in class: public/private/protected name() or async name()
    /(?:public|private|protected)\s+(?:static\s+)?(?:async\s+)?(\w+)\s*\([^)]*\)\s*(?::\s*[\w<>[\]|,\s]+)?\s*\{/g,
    // Arrow function assigned to const: const name = () => or const name = async () =>
    /(?:const|let)\s+(\w+)\s*=\s*(?:async\s*)?\([^)]*\)\s*(?::\s*[\w<>[\]|,\s]+)?\s*=>/g,
    // Java/Kotlin: public void name(), private String name()
    /(?:public|private|protected)\s+(?:static\s+)?(?:final\s+)?(?:\w+\s+)?(\w+)\s*\([^)]*\)\s*(?:throws\s+[\w,\s]+)?\s*\{/g,
    // Python: def name(
    /def\s+(\w+)\s*\([^)]*\)\s*(?:->\s*[\w\[\],\s]+)?\s*:/g,
    // Go: func name( or func (receiver) name(
    /func\s+(?:\([^)]+\)\s+)?(\w+)\s*\([^)]*\)\s*(?:\([^)]*\)|[\w\s*]+)?\s*\{/g,
    // Rust: fn name(
    /fn\s+(\w+)\s*(?:<[^>]+>)?\s*\([^)]*\)\s*(?:->\s*[\w<>]+)?\s*\{/g,
  ];

  for (const pattern of methodPatterns) {
    const regex = new RegExp(pattern.source, pattern.flags);
    let match: RegExpExecArray | null;

    while ((match = regex.exec(code)) !== null) {
      const startIndex = match.index;
      const startLine = code.substring(0, startIndex).split('\n').length;
      const methodName = match[1] || 'anonymous';

      // Skip if we already have this method (avoid duplicates from overlapping patterns)
      if (results.some((r) => r.startLine === startLine)) continue;

      // Count lines until matching closing brace
      let braceCount = 0;
      let endLine = startLine;
      let started = false;

      for (let i = startLine - 1; i < lines.length; i++) {
        const line = lines[i];

        // Count braces (simple approach, doesn't handle strings/comments perfectly)
        for (const char of line) {
          if (char === '{') {
            braceCount++;
            started = true;
          }
          if (char === '}') braceCount--;
        }

        if (started && braceCount === 0) {
          endLine = i + 1;
          break;
        }
      }

      const methodLines = endLine - startLine + 1;
      if (methodLines > 1) {
        // Only count if it's a real method, not just a declaration
        results.push({
          name: methodName,
          lines: methodLines,
          startLine,
        });
      }
    }
  }

  return results;
}

/**
 * Analyze class lengths in the code.
 */
function analyzeClassLengths(code: string): Array<{ name: string; lines: number; startLine: number }> {
  const results: Array<{ name: string; lines: number; startLine: number }> = [];
  const lines = code.split('\n');

  // Pattern for class declarations
  const classPattern =
    /(?:export\s+)?(?:abstract\s+)?class\s+(\w+)(?:\s+extends\s+\w+)?(?:\s+implements\s+[\w,\s]+)?\s*\{/g;

  let match: RegExpExecArray | null;
  while ((match = classPattern.exec(code)) !== null) {
    const startIndex = match.index;
    const startLine = code.substring(0, startIndex).split('\n').length;
    const className = match[1];

    let braceCount = 0;
    let endLine = startLine;
    let started = false;

    for (let i = startLine - 1; i < lines.length; i++) {
      const line = lines[i];

      for (const char of line) {
        if (char === '{') {
          braceCount++;
          started = true;
        }
        if (char === '}') braceCount--;
      }

      if (started && braceCount === 0) {
        endLine = i + 1;
        break;
      }
    }

    results.push({
      name: className,
      lines: endLine - startLine + 1,
      startLine,
    });
  }

  return results;
}

/**
 * Calculate code metrics.
 */
function calculateMetrics(
  code: string,
  lines: string[],
  methodLengths: Array<{ name: string; lines: number; startLine: number }>,
  classLengths: Array<{ name: string; lines: number; startLine: number }>
): CodeMetrics {
  // Count comment lines (simple heuristic)
  const commentLines = lines.filter((l) => {
    const trimmed = l.trim();
    return (
      trimmed.startsWith('//') ||
      trimmed.startsWith('/*') ||
      trimmed.startsWith('*') ||
      trimmed.startsWith('#') ||
      trimmed.startsWith('"""') ||
      trimmed.startsWith("'''")
    );
  }).length;

  // Count empty lines
  const emptyLines = lines.filter((l) => l.trim() === '').length;

  return {
    totalLines: lines.length,
    codeLines: lines.length - commentLines - emptyLines,
    commentLines,
    methodCount: methodLengths.length,
    classCount: classLengths.length,
    interfaceCount: (code.match(/interface\s+\w+/g) || []).length,
    testCount: (code.match(/@Test|it\s*\(|test\s*\(|describe\s*\(/g) || []).length,
    maxMethodLines: Math.max(...methodLengths.map((m) => m.lines), 0),
    maxClassLines: Math.max(...classLengths.map((c) => c.lines), 0),
    customErrorCount: (code.match(/class\s+\w*Error|class\s+\w*Exception/g) || []).length,
    importCount: (code.match(/^import\s+|^from\s+\w+\s+import/gm) || []).length,
  };
}

/**
 * Calculate quality score based on issues and good practices.
 */
function calculateScore(code: string, issues: AnalysisIssue[]): number {
  let score = 100;

  // Deduct for issues
  for (const issue of issues) {
    switch (issue.type) {
      case 'CRITICAL':
        score -= 15;
        break;
      case 'WARNING':
        score -= 5;
        break;
      case 'INFO':
        score -= 1;
        break;
    }
  }

  // Add for good practices
  for (const gp of GOOD_PRACTICES) {
    const matches = (code.match(gp.pattern) || []).length;
    if (matches > 0) {
      // Add points but cap at the weight to avoid gaming
      score += Math.min(gp.weight, matches * 2);
    }
  }

  // Normalize score to 0-100 range
  return Math.max(0, Math.min(100, score));
}

/**
 * Generate a human-readable summary.
 */
function generateSummary(issues: AnalysisIssue[], score: number): string {
  const criticals = issues.filter((i) => i.type === 'CRITICAL').length;
  const warnings = issues.filter((i) => i.type === 'WARNING').length;

  if (criticals > 0) {
    return `NEEDS WORK: ${criticals} critical issue(s) must be fixed before proceeding`;
  }

  if (warnings > 5) {
    return `ACCEPTABLE: ${warnings} warnings should be addressed to improve quality`;
  }

  if (score >= 85) {
    return `EXCELLENT: Code follows best practices with high quality`;
  }

  if (score >= 70) {
    return `GOOD: Code follows most best practices`;
  }

  if (score >= 60) {
    return `FAIR: Some improvements recommended`;
  }

  return `NEEDS IMPROVEMENT: Multiple issues detected that should be addressed`;
}

/**
 * Format analysis result as markdown.
 */
export function formatAnalysisAsMarkdown(result: AnalysisResult): string {
  const lines: string[] = [];

  // Header with summary
  const statusEmoji = result.passed ? '✅' : '❌';
  lines.push(`# ${statusEmoji} Code Analysis Results`, '');
  lines.push(`**${result.summary}**`, '');
  lines.push(`**Score: ${result.score}/100**`, '');
  lines.push('');

  // Metrics section
  lines.push('---', '', '## Metrics', '');
  lines.push(`| Metric | Value |`);
  lines.push(`|--------|-------|`);
  lines.push(`| Total Lines | ${result.metrics.totalLines} |`);
  lines.push(`| Code Lines | ${result.metrics.codeLines} |`);
  lines.push(`| Methods | ${result.metrics.methodCount} |`);
  lines.push(`| Classes | ${result.metrics.classCount} |`);
  lines.push(`| Interfaces | ${result.metrics.interfaceCount} |`);
  lines.push(`| Tests | ${result.metrics.testCount} |`);
  lines.push(`| Custom Errors | ${result.metrics.customErrorCount} |`);
  lines.push(`| Max Method Lines | ${result.metrics.maxMethodLines} |`);
  lines.push(`| Max Class Lines | ${result.metrics.maxClassLines} |`);
  lines.push('');

  // Issues by type
  const criticals = result.issues.filter((i) => i.type === 'CRITICAL');
  const warnings = result.issues.filter((i) => i.type === 'WARNING');
  const infos = result.issues.filter((i) => i.type === 'INFO');

  if (criticals.length > 0) {
    lines.push('---', '', '## CRITICAL Issues (must fix)', '');
    for (const issue of criticals) {
      lines.push(`- **Line ${issue.line || '?'}:** ${issue.message}`);
      if (issue.suggestion) {
        lines.push(`  - Suggestion: ${issue.suggestion}`);
      }
    }
    lines.push('');
  }

  if (warnings.length > 0) {
    lines.push('---', '', '## Warnings (should fix)', '');
    for (const issue of warnings) {
      lines.push(`- **Line ${issue.line || '?'}:** ${issue.message}`);
      if (issue.suggestion) {
        lines.push(`  - Suggestion: ${issue.suggestion}`);
      }
    }
    lines.push('');
  }

  if (infos.length > 0) {
    lines.push('---', '', '## Info', '');
    for (const issue of infos) {
      lines.push(`- Line ${issue.line || '?'}: ${issue.message}`);
    }
    lines.push('');
  }

  // Required actions
  lines.push('---', '', '## Required Actions', '');

  if (criticals.length > 0) {
    lines.push(`1. Fix ${criticals.length} critical issue(s) - these are blocking`);
  }

  if (warnings.length > 3) {
    lines.push(`2. Address ${warnings.length} warnings to improve maintainability`);
  }

  if (result.metrics.testCount === 0) {
    lines.push('3. Add tests - TDD requires tests before implementation');
  }

  if (result.metrics.interfaceCount === 0 && result.metrics.classCount > 0) {
    lines.push('4. Add interfaces for dependency injection');
  }

  if (result.passed) {
    lines.push('');
    lines.push('**Code meets quality standards. Ready for review.**');
  } else {
    lines.push('');
    lines.push('**Fix critical issues before proceeding.**');
  }

  return lines.join('\n');
}
