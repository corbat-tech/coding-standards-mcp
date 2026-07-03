export type AnalysisIssueType = 'CRITICAL' | 'WARNING' | 'INFO';

export type SupportedLanguage = 'typescript' | 'javascript' | 'java' | 'python' | 'go' | 'rust' | 'unknown';

export interface AnalysisThresholds {
  maxMethodLines?: number;
  maxClassLines?: number;
}

export interface AnalysisInput {
  code: string;
  language?: SupportedLanguage;
  filename?: string;
  thresholds?: AnalysisThresholds;
  requireTests?: boolean;
}

export interface AnalysisIssue {
  type: AnalysisIssueType;
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

export interface CodeSpan {
  name: string;
  lines: number;
  startLine: number;
}

export interface LanguageAnalysis {
  issues: AnalysisIssue[];
  methodLengths: CodeSpan[];
  classLengths: CodeSpan[];
  metrics: Partial<Pick<CodeMetrics, 'methodCount' | 'classCount' | 'interfaceCount' | 'testCount' | 'importCount'>>;
}

export interface LanguageAnalyzer {
  supports(input: AnalysisInput): boolean;
  analyze(input: AnalysisInput): LanguageAnalysis;
}
