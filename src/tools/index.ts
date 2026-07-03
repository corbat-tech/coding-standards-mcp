/**
 * Tools module index.
 *
 * This module provides:
 * - Tool definitions (name, description, inputSchema)
 * - Tool handlers (business logic)
 * - handleToolCall dispatcher
 */

import { formatErrorForResponse, ToolInputError } from '../errors.js';
import { logger } from '../logger.js';
import { recordError, recordToolCall } from '../metrics.js';
import type { ToolName } from './definitions.js';
import {
  handleGetContext,
  handleHealth,
  handleInit,
  handleProfiles,
  handleSearch,
  handleValidate,
  handleVerify,
} from './handlers/index.js';

export type { ToolName } from './definitions.js';
// Re-export definitions and schemas
export { tools } from './definitions.js';
export * from './schemas.js';

/**
 * Handle tool calls by dispatching to the appropriate handler.
 */
export async function handleToolCall(
  name: string,
  args: Record<string, unknown>
): Promise<{ content: Array<{ type: 'text'; text: string }>; isError?: boolean }> {
  const toolName = name as ToolName;

  // Record metrics
  recordToolCall(name);
  logger.debug('Tool call received', { tool: name, args });

  try {
    let result: { content: Array<{ type: 'text'; text: string }>; isError?: boolean };

    switch (toolName) {
      case 'get_context':
        result = await handleGetContext(args);
        break;
      case 'validate':
        result = await handleValidate(args);
        break;
      case 'verify':
        result = await handleVerify(args);
        break;
      case 'search':
        result = await handleSearch(args);
        break;
      case 'profiles':
        result = await handleProfiles();
        break;
      case 'health':
        result = await handleHealth();
        break;
      case 'init':
        result = await handleInit(args);
        break;
      default:
        recordError();
        {
          const error = new ToolInputError(name, 'Unknown tool', {
            availableTools: ['get_context', 'validate', 'verify', 'search', 'profiles', 'health', 'init'],
          });
          return {
            content: [{ type: 'text', text: formatErrorForResponse(error) }],
            isError: true,
          };
        }
    }

    if (result.isError) {
      recordError();
      logger.warn('Tool returned error', { tool: name });
    }

    return result;
  } catch (error) {
    recordError();
    const message = error instanceof Error ? error.message : String(error);
    logger.error('Tool call failed', { tool: name, error: message });

    return {
      content: [{ type: 'text', text: `Error: ${message}` }],
      isError: true,
    };
  }
}
