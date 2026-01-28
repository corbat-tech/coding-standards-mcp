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
  lines.push('');

  // === STRATEGY 1: CHECKPOINT VERIFICATION ===
  lines.push('---', '', '## MANDATORY CHECKPOINT', '');
  lines.push(`
⚠️ **BEFORE writing ANY code**, respond with this JSON:

\`\`\`json
{
  "checkpoint": {
    "task_understood": true,
    "clarifications_needed": [],
    "approach": "Brief description of implementation approach"
  },
  "architecture": {
    "layers_affected": ["domain", "application", "infrastructure"],
    "interfaces_to_create": ["InterfaceName1", "InterfaceName2"],
    "classes_to_create": ["ClassName1", "ClassName2"]
  },
  "tdd_plan": {
    "tests_to_write": [
      "should_do_X_when_Y",
      "should_fail_when_Z",
      "should_handle_edge_case"
    ]
  },
  "quality_commitments": {
    "max_method_lines": 20,
    "max_class_lines": 200,
    "dependency_injection": true,
    "custom_errors": true,
    "test_coverage_target": 80
  }
}
\`\`\`

**Only proceed to code generation after completing this checkpoint.**
`);

  // === STRATEGY 4: CONTRACTUAL RESPONSE FORMAT ===
  lines.push('---', '', '## REQUIRED RESPONSE STRUCTURE', '');
  lines.push(`
Structure your response in this **exact order**:

### 1. CHECKPOINT
Complete the checkpoint JSON above first.

### 2. INTERFACES / TYPES
\`\`\`
// Define ALL interfaces and types FIRST
interface ServiceName { ... }
type ResultType = { ... }
\`\`\`

### 3. TESTS
\`\`\`
// Write tests BEFORE implementation (TDD)
describe('ServiceName', () => {
  it('should do X when Y', () => { ... });
  it('should fail when Z', () => { ... });
});
\`\`\`

### 4. IMPLEMENTATION
\`\`\`
// NOW implement to make tests pass
class ServiceNameImpl implements ServiceName { ... }
\`\`\`

### 5. SELF-REVIEW
Complete the self-review JSON below.

⚠️ **Code not following this structure will not meet quality standards.**
`);

  // === STRATEGY 3: MANDATORY SELF-REVIEW ===
  lines.push('---', '', '## MANDATORY SELF-REVIEW', '');
  lines.push(`
After generating code, perform a self-review and report:

\`\`\`json
{
  "self_review": {
    "methods_over_20_lines": 0,
    "classes_over_200_lines": 0,
    "interfaces_created": 3,
    "tests_written": 5,
    "custom_errors_defined": 2,
    "dependency_injection_used": true,
    "hardcoded_values": 0,
    "todos_or_fixmes": 0
  },
  "quality_score": "8/10",
  "confidence": "high",
  "improvements_if_more_time": [
    "Add more edge case tests",
    "Extract validation logic to separate class"
  ]
}
\`\`\`

**If quality_score < 7, iterate and improve before presenting the code.**
`);

  // === VERIFY TOOL REMINDER ===
  lines.push('---', '', '## FINAL STEP', '');
  lines.push(`
After completing all code, call the \`verify\` tool with your generated code:

\`\`\`
verify({
  code: "// all implementation code",
  tests: "// all test code",
  interfaces: "// all interfaces"
})
\`\`\`

Only present code to user after verify returns **PASS**.
`);

  return { content: [{ type: 'text', text: lines.join('\n') }] };
}
