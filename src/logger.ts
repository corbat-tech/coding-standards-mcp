import { config } from './config.js';

/**
 * Log levels in order of severity.
 */
type LogLevel = 'debug' | 'info' | 'warn' | 'error';

/**
 * Structured log entry.
 */
interface LogEntry {
  timestamp: string;
  level: LogLevel;
  message: string;
  context?: Record<string, unknown>;
}

/**
 * Log level priorities for filtering.
 */
const LOG_LEVEL_PRIORITY: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
};

/**
 * Check if a log level should be output based on configured level.
 */
function shouldLog(level: LogLevel): boolean {
  const configuredLevel = config.logLevel as LogLevel;
  return LOG_LEVEL_PRIORITY[level] >= LOG_LEVEL_PRIORITY[configuredLevel];
}

/**
 * Format and output a log entry.
 * Always outputs to stderr to avoid interfering with MCP stdio transport.
 */
function log(level: LogLevel, message: string, context?: Record<string, unknown>): void {
  if (!shouldLog(level)) return;

  const entry: LogEntry = {
    timestamp: new Date().toISOString(),
    level,
    message,
    ...(context && Object.keys(context).length > 0 ? { context } : {}),
  };

  // Output as JSON for structured logging
  console.error(JSON.stringify(entry));
}

/**
 * Logger interface with level-specific methods.
 */
export const logger = {
  /**
   * Debug level - detailed information for debugging.
   */
  debug: (message: string, context?: Record<string, unknown>): void => {
    log('debug', message, context);
  },

  /**
   * Info level - general operational information.
   */
  info: (message: string, context?: Record<string, unknown>): void => {
    log('info', message, context);
  },

  /**
   * Warn level - something unexpected but not critical.
   */
  warn: (message: string, context?: Record<string, unknown>): void => {
    log('warn', message, context);
  },

  /**
   * Error level - something went wrong.
   */
  error: (message: string, context?: Record<string, unknown>): void => {
    log('error', message, context);
  },
};

export type { LogLevel, LogEntry };
