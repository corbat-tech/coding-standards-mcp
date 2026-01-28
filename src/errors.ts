/**
 * Base error class for Corbat MCP errors.
 * All custom errors should extend this class.
 */
export class CorbatError extends Error {
  constructor(
    message: string,
    public readonly code: string,
    public readonly details?: Record<string, unknown>
  ) {
    super(message);
    this.name = 'CorbatError';

    // Maintains proper stack trace for V8
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this, this.constructor);
    }
  }

  /**
   * Convert to a plain object for serialization.
   */
  toJSON(): Record<string, unknown> {
    return {
      name: this.name,
      code: this.code,
      message: this.message,
      details: this.details,
    };
  }
}

/**
 * Error when a profile is not found.
 */
export class ProfileNotFoundError extends CorbatError {
  constructor(profileId: string, availableProfiles: string[] = []) {
    super(
      `Profile "${profileId}" not found`,
      'PROFILE_NOT_FOUND',
      { profileId, availableProfiles }
    );
    this.name = 'ProfileNotFoundError';
  }
}

/**
 * Error when configuration is invalid.
 */
export class InvalidConfigError extends CorbatError {
  constructor(path: string, validationErrors: string[]) {
    super(
      `Invalid configuration at ${path}`,
      'INVALID_CONFIG',
      { path, validationErrors }
    );
    this.name = 'InvalidConfigError';
  }
}

/**
 * Error when project stack detection fails.
 */
export class StackDetectionError extends CorbatError {
  constructor(projectDir: string, reason: string) {
    super(
      `Could not detect stack in ${projectDir}: ${reason}`,
      'STACK_DETECTION_FAILED',
      { projectDir, reason }
    );
    this.name = 'StackDetectionError';
  }
}

/**
 * Error when a guardrail file is invalid.
 */
export class InvalidGuardrailError extends CorbatError {
  constructor(taskType: string, reason: string) {
    super(
      `Invalid guardrail for task type "${taskType}": ${reason}`,
      'INVALID_GUARDRAIL',
      { taskType, reason }
    );
    this.name = 'InvalidGuardrailError';
  }
}

/**
 * Error when a tool receives invalid input.
 */
export class ToolInputError extends CorbatError {
  constructor(toolName: string, reason: string, input?: unknown) {
    super(
      `Invalid input for tool "${toolName}": ${reason}`,
      'TOOL_INPUT_ERROR',
      { toolName, reason, input }
    );
    this.name = 'ToolInputError';
  }
}

/**
 * Error when a resource is not found.
 */
export class ResourceNotFoundError extends CorbatError {
  constructor(resourceUri: string) {
    super(
      `Resource not found: ${resourceUri}`,
      'RESOURCE_NOT_FOUND',
      { resourceUri }
    );
    this.name = 'ResourceNotFoundError';
  }
}

/**
 * Check if an error is a CorbatError.
 */
export function isCorbatError(error: unknown): error is CorbatError {
  return error instanceof CorbatError;
}

/**
 * Format an error for MCP response.
 */
export function formatErrorForResponse(error: unknown): string {
  if (isCorbatError(error)) {
    let message = `[${error.code}] ${error.message}`;
    if (error.details) {
      const detailsStr = Object.entries(error.details)
        .filter(([key]) => key !== 'input') // Don't include potentially large input
        .map(([key, value]) => `${key}: ${JSON.stringify(value)}`)
        .join(', ');
      if (detailsStr) {
        message += `\n\nDetails: ${detailsStr}`;
      }
    }
    return message;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return String(error);
}
