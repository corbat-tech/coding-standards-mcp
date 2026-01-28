import {
  classifyTaskType,
  detectProjectStack,
  getGuardrails,
  getProjectRules,
  loadProjectConfig,
} from '../../agent.js';
import { config } from '../../config.js';
import { getProfile, listProfiles } from '../../profiles.js';
import { GetContextSchema } from '../schemas.js';

/**
 * Handler for the get_context tool.
 * Returns complete coding standards context for a task.
 */
export async function handleGetContext(
  args: Record<string, unknown>
): Promise<{ content: Array<{ type: 'text'; text: string }>; isError?: boolean }> {
  const { task, project_dir } = GetContextSchema.parse(args);

  // Classify task type
  const taskType = classifyTaskType(task);

  // Auto-detect project stack if directory provided
  let detectedStack = null;
  let projectConfig = null;

  if (project_dir) {
    detectedStack = await detectProjectStack(project_dir);
    projectConfig = await loadProjectConfig(project_dir);
  }

  // Determine profile (priority: project config > detected > default)
  const profileId = projectConfig?.profile || detectedStack?.suggestedProfile || config.defaultProfile;
  const profile = await getProfile(profileId);

  if (!profile) {
    const availableProfiles = await listProfiles();
    return {
      content: [
        {
          type: 'text',
          text: `Profile "${profileId}" not found.\n\nAvailable profiles:\n${availableProfiles.map((p) => `- ${p.id}`).join('\n')}\n\nRun \`corbat-init\` in your project to create a custom profile.`,
        },
      ],
      isError: true,
    };
  }

  // Get guardrails and rules
  const guardrails = await getGuardrails(taskType, projectConfig);
  const projectRules = getProjectRules(taskType, projectConfig);

  // Build concise, scannable output
  const lines: string[] = [`# Context for: ${task}`, '', '---', ''];

  // Stack Detection (concise)
  if (detectedStack) {
    const stackParts = [detectedStack.language];
    if (detectedStack.framework) stackParts.push(detectedStack.framework);
    if (detectedStack.buildTool) stackParts.push(detectedStack.buildTool);
    lines.push(`**Stack:** ${stackParts.join(' · ')}`);
  }
  lines.push(`**Task type:** ${taskType.toUpperCase()}`);
  lines.push(`**Profile:** ${profileId}`);
  lines.push('');

  // Guardrails (essential, concise)
  lines.push('---', '', '## Guardrails', '');
  lines.push('**MUST:**');
  for (const rule of guardrails.mandatory.slice(0, 5)) {
    lines.push(`- ${rule}`);
  }
  lines.push('');
  lines.push('**AVOID:**');
  for (const rule of guardrails.avoid.slice(0, 4)) {
    lines.push(`- ${rule}`);
  }
  lines.push('');

  // Project-specific rules (if any)
  if (projectRules.length > 0) {
    lines.push('**PROJECT RULES:**');
    for (const rule of projectRules) {
      lines.push(`- ${rule}`);
    }
    lines.push('');
  }

  // Quick Reference (most important settings)
  lines.push('---', '', '## Quick Reference', '');

  if (profile.codeQuality) {
    lines.push(`- Max method lines: ${profile.codeQuality.maxMethodLines}`);
    lines.push(`- Max class lines: ${profile.codeQuality.maxClassLines}`);
    lines.push(`- Min test coverage: ${profile.codeQuality.minimumTestCoverage}%`);
  }

  if (profile.architecture) {
    lines.push(`- Architecture: ${profile.architecture.type}`);
  }

  if (profile.ddd?.enabled) {
    lines.push('- DDD: Enabled');
  }

  if (profile.testing) {
    lines.push(`- Testing: ${profile.testing.framework || 'standard'}`);
  }
  lines.push('');

  // Naming conventions (concise)
  if (profile.naming) {
    lines.push('---', '', '## Naming', '');
    const naming = profile.naming as Record<string, unknown>;
    if (naming.general && typeof naming.general === 'object') {
      for (const [key, value] of Object.entries(naming.general as Record<string, string>)) {
        lines.push(`- **${key}:** ${value}`);
      }
    }
    if (naming.suffixes && typeof naming.suffixes === 'object') {
      lines.push('');
      lines.push('**Suffixes:**');
      for (const [key, value] of Object.entries(naming.suffixes as Record<string, string>)) {
        lines.push(`- ${key}: \`${value}\``);
      }
    }
    lines.push('');
  }

  // Workflow reminder (brief)
  lines.push('---', '', '## Workflow', '');
  lines.push('```');
  lines.push('1. CLARIFY  → Ask if unclear');
  lines.push('2. PLAN     → Task checklist');
  lines.push('3. BUILD    → TDD: Test → Code → Refactor');
  lines.push('4. VERIFY   → Tests pass, linter clean');
  lines.push('5. REVIEW   → Self-check as expert');
  lines.push('```');

  return { content: [{ type: 'text', text: lines.join('\n') }] };
}
