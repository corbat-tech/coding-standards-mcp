import { getGuardrails } from '../../agent.js';
import { config } from '../../config.js';
import { getProfile } from '../../profiles.js';
import { ValidateSchema } from '../schemas.js';

/**
 * Handler for the validate tool.
 * Returns validation criteria for code against standards.
 */
export async function handleValidate(
  args: Record<string, unknown>
): Promise<{ content: Array<{ type: 'text'; text: string }>; isError?: boolean }> {
  const { code, task_type } = ValidateSchema.parse(args);

  const profileId = config.defaultProfile;
  const profile = await getProfile(profileId);

  if (!profile) {
    return {
      content: [{ type: 'text', text: `Profile not found: ${profileId}` }],
      isError: true,
    };
  }

  const guardrails = task_type ? await getGuardrails(task_type, null) : null;

  const lines: string[] = [
    '# Code Validation',
    '',
    '## Code',
    '```',
    code.slice(0, 2000) + (code.length > 2000 ? '\n...(truncated)' : ''),
    '```',
    '',
    '---',
    '',
    '## Validation Criteria',
    '',
  ];

  // Code quality thresholds
  if (profile.codeQuality) {
    lines.push('**Thresholds:**');
    lines.push(`- Max method lines: ${profile.codeQuality.maxMethodLines}`);
    lines.push(`- Max class lines: ${profile.codeQuality.maxClassLines}`);
    lines.push(`- Max parameters: ${profile.codeQuality.maxMethodParameters}`);
    lines.push(`- Min coverage: ${profile.codeQuality.minimumTestCoverage}%`);
    lines.push('');
  }

  // Guardrails if task type specified
  if (guardrails) {
    lines.push(`**${task_type?.toUpperCase()} Guardrails:**`);
    lines.push('');
    lines.push('Must:');
    for (const rule of guardrails.mandatory.slice(0, 4)) {
      lines.push(`- ${rule}`);
    }
    lines.push('');
    lines.push('Avoid:');
    for (const rule of guardrails.avoid.slice(0, 3)) {
      lines.push(`- ${rule}`);
    }
    lines.push('');
  }

  // Naming conventions
  if (profile.naming) {
    lines.push('**Naming:**');
    const naming = profile.naming as Record<string, unknown>;
    if (naming.general && typeof naming.general === 'object') {
      for (const [key, value] of Object.entries(naming.general as Record<string, string>)) {
        lines.push(`- ${key}: ${value}`);
      }
    }
    lines.push('');
  }

  lines.push('---', '');
  lines.push('## Review Checklist', '');
  lines.push('Analyze the code and report:', '');
  lines.push('1. **CRITICAL** - Must fix (bugs, security, violations)');
  lines.push('2. **WARNINGS** - Should fix (style, best practices)');
  lines.push('3. **Score** - Compliance 0-100 with justification');

  return { content: [{ type: 'text', text: lines.join('\n') }] };
}
