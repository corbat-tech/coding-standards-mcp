import ts from 'typescript';
import type { AnalysisInput, AnalysisIssue, CodeSpan, LanguageAnalysis, LanguageAnalyzer } from '../types.js';

function lineOf(sourceFile: ts.SourceFile, position: number): number {
  return sourceFile.getLineAndCharacterOfPosition(position).line + 1;
}

function spanOf(sourceFile: ts.SourceFile, node: ts.Node, fallbackName: string): CodeSpan {
  const startLine = lineOf(sourceFile, node.getStart(sourceFile));
  const endLine = lineOf(sourceFile, node.getEnd());
  const nodeWithName = node as ts.Node & { name?: ts.Node };
  const name = nodeWithName.name && ts.isIdentifier(nodeWithName.name) ? nodeWithName.name.text : fallbackName;
  return {
    name,
    lines: Math.max(1, endLine - startLine + 1),
    startLine,
  };
}

function isTestCall(node: ts.CallExpression): boolean {
  const expression = node.expression;
  if (ts.isIdentifier(expression)) {
    return expression.text === 'describe' || expression.text === 'it' || expression.text === 'test';
  }
  if (ts.isPropertyAccessExpression(expression)) {
    return expression.name.text === 'describe' || expression.name.text === 'it' || expression.name.text === 'test';
  }
  return false;
}

function isInnerHtmlAssignment(node: ts.BinaryExpression): boolean {
  if (node.operatorToken.kind !== ts.SyntaxKind.EqualsToken) return false;
  const left = node.left;
  return ts.isPropertyAccessExpression(left) && left.name.text === 'innerHTML';
}

export class TypeScriptAnalyzer implements LanguageAnalyzer {
  supports(input: AnalysisInput): boolean {
    if (input.language === 'typescript' || input.language === 'javascript') return true;
    return Boolean(input.filename?.match(/\.[cm]?[tj]sx?$/));
  }

  analyze(input: AnalysisInput): LanguageAnalysis {
    const sourceFile = ts.createSourceFile(
      input.filename ?? 'inline.ts',
      input.code,
      ts.ScriptTarget.Latest,
      true,
      input.language === 'javascript' ? ts.ScriptKind.JS : ts.ScriptKind.TS
    );

    const issues: AnalysisIssue[] = [];
    const methodLengths: CodeSpan[] = [];
    const classLengths: CodeSpan[] = [];
    let interfaceCount = 0;
    let typeAliasCount = 0;
    let classCount = 0;
    let testCount = 0;
    let importCount = 0;

    const visit = (node: ts.Node): void => {
      if (
        ts.isFunctionDeclaration(node) ||
        ts.isFunctionExpression(node) ||
        ts.isArrowFunction(node) ||
        ts.isMethodDeclaration(node)
      ) {
        methodLengths.push(spanOf(sourceFile, node, 'anonymous'));
      }

      if (ts.isClassDeclaration(node)) {
        classCount += 1;
        classLengths.push(spanOf(sourceFile, node, 'anonymous-class'));
      }

      if (ts.isInterfaceDeclaration(node)) {
        interfaceCount += 1;
      }

      if (ts.isTypeAliasDeclaration(node)) {
        typeAliasCount += 1;
      }

      if (ts.isImportDeclaration(node) || ts.isImportEqualsDeclaration(node)) {
        importCount += 1;
      }

      if (ts.isCatchClause(node) && node.block.statements.length === 0) {
        issues.push({
          type: 'CRITICAL',
          rule: 'no-empty-catch',
          message: 'Empty catch block - errors are silently swallowed',
          line: lineOf(sourceFile, node.getStart(sourceFile)),
          suggestion: 'Handle the error, log it, or rethrow with context',
        });
      }

      if (node.kind === ts.SyntaxKind.AnyKeyword) {
        issues.push({
          type: 'WARNING',
          rule: 'no-any-type',
          message: 'TypeScript "any" type - loses type safety',
          line: lineOf(sourceFile, node.getStart(sourceFile)),
          suggestion: 'Use specific types or unknown with type guards',
        });
      }

      if (ts.isBinaryExpression(node)) {
        if (node.operatorToken.kind === ts.SyntaxKind.EqualsEqualsToken) {
          issues.push({
            type: 'WARNING',
            rule: 'strict-equality',
            message: 'Loose equality operator',
            line: lineOf(sourceFile, node.operatorToken.getStart(sourceFile)),
            suggestion: 'Use === for strict comparison',
          });
        }
        if (node.operatorToken.kind === ts.SyntaxKind.ExclamationEqualsToken) {
          issues.push({
            type: 'WARNING',
            rule: 'strict-inequality',
            message: 'Loose inequality operator',
            line: lineOf(sourceFile, node.operatorToken.getStart(sourceFile)),
            suggestion: 'Use !== for strict comparison',
          });
        }
        if (isInnerHtmlAssignment(node)) {
          issues.push({
            type: 'CRITICAL',
            rule: 'no-inner-html',
            message: 'innerHTML can lead to XSS vulnerabilities',
            line: lineOf(sourceFile, node.getStart(sourceFile)),
            suggestion: 'Use textContent or sanitize HTML before insertion',
          });
        }
      }

      if (ts.isCallExpression(node)) {
        if (isTestCall(node)) {
          testCount += 1;
        }
        if (ts.isIdentifier(node.expression) && node.expression.text === 'eval') {
          issues.push({
            type: 'CRITICAL',
            rule: 'no-eval',
            message: 'eval() is dangerous and can lead to code injection',
            line: lineOf(sourceFile, node.expression.getStart(sourceFile)),
            suggestion: 'Use safer alternatives like JSON.parse() or specific parsers',
          });
        }
      }

      if (
        ts.isNewExpression(node) &&
        ts.isIdentifier(node.expression) &&
        node.expression.text === 'Date' &&
        (node.arguments?.length ?? 0) === 0
      ) {
        issues.push({
          type: 'WARNING',
          rule: 'no-hardcoded-time',
          message: 'Hardcoded current time - not testable',
          line: lineOf(sourceFile, node.getStart(sourceFile)),
          suggestion: 'Inject a Clock/TimeProvider for testability',
        });
      }

      ts.forEachChild(node, visit);
    };

    visit(sourceFile);

    return {
      issues,
      methodLengths,
      classLengths,
      metrics: {
        methodCount: methodLengths.length,
        classCount,
        interfaceCount: interfaceCount + typeAliasCount,
        testCount,
        importCount,
      },
    };
  }
}
