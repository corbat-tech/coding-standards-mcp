import { config } from '../../config.js';
import { loadGuardrails } from '../../guardrails.js';
import { getMetrics } from '../../metrics.js';
import { listProfiles, loadStandards } from '../../profiles.js';
import { tools } from '../definitions.js';

/**
 * Handler for the health tool.
 * Returns server status, loaded configuration, and usage metrics.
 */
export async function handleHealth(): Promise<{ content: Array<{ type: 'text'; text: string }> }> {
  const startTime = Date.now();

  try {
    const profiles = await listProfiles();
    const standards = await loadStandards();
    const guardrails = await loadGuardrails();
    const loadTimeMs = Date.now() - startTime;
    const metrics = getMetrics();
    const capabilities = ['tools', 'resources', 'prompts'];

    const lines = [
      '# Corbat MCP Health',
      '',
      '**Status:** OK',
      `**Version:** ${config.serverVersion}`,
      `**Package version:** ${config.serverVersion}`,
      `**Standards version:** ${config.serverVersion}`,
      `**Runtime Node:** ${process.version}`,
      `**Uptime:** ${metrics.uptimeFormatted}`,
      `**Load time:** ${loadTimeMs}ms`,
      '',
      '## Resources',
      '',
      `- **Profiles:** ${profiles.length}`,
      `- **Standards:** ${standards.length} documents`,
      `- **Guardrails:** ${Object.keys(guardrails).length}`,
      `- **Default profile:** ${config.defaultProfile}`,
      '',
      '## Capabilities',
      '',
      `- **Protocol capabilities:** ${capabilities.join(', ')}`,
      `- **Tools:** ${tools.map((tool) => tool.name).join(', ')}`,
      '',
    ];

    // Only show metrics section if there have been calls
    if (metrics.totalToolCalls > 0) {
      lines.push('## Metrics', '');
      lines.push(`- **Total tool calls:** ${metrics.totalToolCalls}`);

      if (metrics.mostUsedTool) {
        lines.push(`- **Most used tool:** ${metrics.mostUsedTool}`);
      }

      if (metrics.mostUsedProfile) {
        lines.push(`- **Most used profile:** ${metrics.mostUsedProfile}`);
      }

      if (metrics.mostCommonTaskType) {
        lines.push(`- **Most common task:** ${metrics.mostCommonTaskType}`);
      }

      if (metrics.errors > 0) {
        lines.push(`- **Errors:** ${metrics.errors}`);
      }

      lines.push('');
    }

    return { content: [{ type: 'text', text: lines.join('\n') }] };
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : String(error);
    return {
      content: [{ type: 'text', text: `# Corbat MCP Health\n\n**Status:** ERROR\n**Error:** ${errorMessage}` }],
    };
  }
}
