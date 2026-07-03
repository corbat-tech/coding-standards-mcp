import { getGuardrails } from '../../agent.js';
import { analyzeCode, formatAnalysisAsMarkdown } from '../../analysis/code-analyzer.js';
import { config } from '../../config.js';
import { getProfile } from '../../profiles.js';
import { ValidateSchema } from '../schemas.js';

/**
 * Handler for the validate tool.
 * Performs real code analysis and returns actionable feedback.
 *
 * This is a key part of the Smart Enforcement system - it actually
 * analyzes code instead of just returning a checklist.
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

  const analysis = analyzeCode({
    code,
    thresholds: {
      maxMethodLines: profile.codeQuality?.maxMethodLines,
      maxClassLines: profile.codeQuality?.maxClassLines,
    },
  });

  const guardrails = task_type ? await getGuardrails(task_type, null) : null;

  // Build output with analysis results
  const lines: string[] = [];

  // Add the formatted analysis
  lines.push(formatAnalysisAsMarkdown(analysis));
  lines.push('');

  // Add profile thresholds for reference
  lines.push('---', '', '## Profile Standards Reference', '');

  if (profile.codeQuality) {
    lines.push('**Configured Thresholds:**');
    lines.push(`- Max method lines: ${profile.codeQuality.maxMethodLines} (yours: ${analysis.metrics.maxMethodLines})`);
    lines.push(`- Max class lines: ${profile.codeQuality.maxClassLines} (yours: ${analysis.metrics.maxClassLines})`);
    lines.push(`- Min test coverage: ${profile.codeQuality.minimumTestCoverage}%`);
    lines.push('');
  }

  // Add guardrails reminder if task type specified
  if (guardrails) {
    lines.push('---', '', `## ${task_type?.toUpperCase()} Guardrails Reminder`, '');
    lines.push('**Must:**');
    for (const rule of guardrails.mandatory.slice(0, 3)) {
      lines.push(`- ${rule}`);
    }
    lines.push('');
    lines.push('**Avoid:**');
    for (const rule of guardrails.avoid.slice(0, 3)) {
      lines.push(`- ${rule}`);
    }
    lines.push('');
  }

  // Final verdict
  lines.push('---', '', '## Verdict', '');

  if (analysis.passed) {
    lines.push('**PASSED** - Code meets quality standards.');
    lines.push('');
    lines.push('You may present this code to the user or call `verify` for final confirmation.');
  } else {
    lines.push('**NEEDS WORK** - Address the issues above before proceeding.');
    lines.push('');
    lines.push('Fix critical issues first, then warnings, then call `validate` again.');
  }

  return {
    content: [{ type: 'text', text: lines.join('\n') }],
    isError: !analysis.passed,
  };
}
