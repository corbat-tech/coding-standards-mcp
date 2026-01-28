/**
 * Simple in-memory metrics for Corbat MCP.
 * Tracks tool calls, profiles used, and errors.
 */

interface Metrics {
  toolCalls: Record<string, number>;
  profilesUsed: Record<string, number>;
  taskTypes: Record<string, number>;
  errors: number;
  startTime: number;
}

/**
 * Global metrics state.
 */
const metrics: Metrics = {
  toolCalls: {},
  profilesUsed: {},
  taskTypes: {},
  errors: 0,
  startTime: Date.now(),
};

/**
 * Record a tool call.
 */
export function recordToolCall(toolName: string): void {
  metrics.toolCalls[toolName] = (metrics.toolCalls[toolName] || 0) + 1;
}

/**
 * Record a profile being used.
 */
export function recordProfileUsed(profileId: string): void {
  metrics.profilesUsed[profileId] = (metrics.profilesUsed[profileId] || 0) + 1;
}

/**
 * Record a task type being processed.
 */
export function recordTaskType(taskType: string): void {
  metrics.taskTypes[taskType] = (metrics.taskTypes[taskType] || 0) + 1;
}

/**
 * Record an error.
 */
export function recordError(): void {
  metrics.errors++;
}

/**
 * Get the most used item from a record.
 */
function getMostUsed(record: Record<string, number>): string | null {
  let maxKey: string | null = null;
  let maxValue = 0;

  for (const [key, value] of Object.entries(record)) {
    if (value > maxValue) {
      maxValue = value;
      maxKey = key;
    }
  }

  return maxKey;
}

/**
 * Format duration in human-readable format.
 */
function formatDuration(ms: number): string {
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);

  if (hours > 0) {
    return `${hours}h ${minutes % 60}m`;
  }
  if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`;
  }
  return `${seconds}s`;
}

/**
 * Get current metrics with computed values.
 */
export function getMetrics(): {
  toolCalls: Record<string, number>;
  profilesUsed: Record<string, number>;
  taskTypes: Record<string, number>;
  errors: number;
  uptimeMs: number;
  uptimeFormatted: string;
  totalToolCalls: number;
  mostUsedTool: string | null;
  mostUsedProfile: string | null;
  mostCommonTaskType: string | null;
} {
  const uptimeMs = Date.now() - metrics.startTime;
  const totalToolCalls = Object.values(metrics.toolCalls).reduce((a, b) => a + b, 0);

  return {
    toolCalls: { ...metrics.toolCalls },
    profilesUsed: { ...metrics.profilesUsed },
    taskTypes: { ...metrics.taskTypes },
    errors: metrics.errors,
    uptimeMs,
    uptimeFormatted: formatDuration(uptimeMs),
    totalToolCalls,
    mostUsedTool: getMostUsed(metrics.toolCalls),
    mostUsedProfile: getMostUsed(metrics.profilesUsed),
    mostCommonTaskType: getMostUsed(metrics.taskTypes),
  };
}

/**
 * Reset all metrics (useful for testing).
 */
export function resetMetrics(): void {
  metrics.toolCalls = {};
  metrics.profilesUsed = {};
  metrics.taskTypes = {};
  metrics.errors = 0;
  metrics.startTime = Date.now();
}
