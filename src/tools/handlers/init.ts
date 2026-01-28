import { detectProjectStack } from '../../agent.js';
import { listProfiles } from '../../profiles.js';
import { InitSchema } from '../schemas.js';

/**
 * Generate suggested .corbat.json configuration based on detected stack.
 */
function generateSuggestedConfig(
  stack: {
    language: string;
    framework?: string;
    suggestedProfile: string;
  } | null
): Record<string, unknown> {
  if (!stack) {
    return {
      profile: 'minimal',
      autoInject: true,
      rules: {
        always: ['Follow project coding conventions'],
        onNewFile: ['Add appropriate file header comments'],
        onTest: ['Follow AAA pattern (Arrange-Act-Assert)'],
        onRefactor: ['Ensure tests pass before and after refactoring'],
      },
    };
  }

  const config: Record<string, unknown> = {
    profile: stack.suggestedProfile,
    autoInject: true,
  };

  // Add language-specific rules
  const rules: Record<string, string[]> = {
    always: [],
    onNewFile: [],
    onTest: [],
    onRefactor: [],
  };

  if (stack.language === 'Java') {
    rules.always.push('Use constructor injection for dependencies');
    rules.onNewFile.push('Add Javadoc for public classes and methods');
    rules.onTest.push('Use @DisplayName for readable test names');
  } else if (stack.language === 'TypeScript' || stack.language === 'JavaScript') {
    rules.always.push('Use strict TypeScript configuration');
    rules.onNewFile.push('Export types alongside implementations');
    rules.onTest.push('Mock external dependencies');
  } else if (stack.language === 'Python') {
    rules.always.push('Use type hints for function signatures');
    rules.onNewFile.push('Add module docstring');
    rules.onTest.push('Use pytest fixtures for setup');
  } else if (stack.language === 'Go') {
    rules.always.push('Follow effective Go guidelines');
    rules.onNewFile.push('Add package documentation');
    rules.onTest.push('Use table-driven tests');
  }

  if (rules.always.length > 0 || rules.onNewFile.length > 0) {
    config.rules = rules;
  }

  return config;
}

/**
 * Handler for the init tool.
 * Analyzes project and generates suggested .corbat.json configuration.
 */
export async function handleInit(
  args: Record<string, unknown>
): Promise<{ content: Array<{ type: 'text'; text: string }>; isError?: boolean }> {
  const { project_dir } = InitSchema.parse(args);

  // Detect project stack
  const stack = await detectProjectStack(project_dir);

  // Get available profiles
  const profiles = await listProfiles();
  const profileIds = profiles.map((p) => p.id);

  // Generate suggested config
  const suggestedConfig = generateSuggestedConfig(stack);

  const lines: string[] = ['# Corbat MCP Configuration', '', '## Detected Stack', ''];

  if (stack) {
    lines.push(`- **Language:** ${stack.language}`);
    if (stack.framework) {
      lines.push(`- **Framework:** ${stack.framework}`);
    }
    if (stack.buildTool) {
      lines.push(`- **Build Tool:** ${stack.buildTool}`);
    }
    lines.push(`- **Suggested Profile:** ${stack.suggestedProfile}`);
    lines.push(`- **Confidence:** ${stack.confidence}`);
  } else {
    lines.push('Could not auto-detect stack. Using minimal profile.');
  }

  lines.push('', '## Suggested .corbat.json', '');
  lines.push('```json');
  lines.push(JSON.stringify(suggestedConfig, null, 2));
  lines.push('```');

  lines.push('', '## How to Use', '');
  lines.push(`1. Save the above JSON to \`${project_dir}/.corbat.json\``);
  lines.push('2. Customize rules and profile as needed');
  lines.push('3. Run `get_context` to get standards for your tasks');

  lines.push('', '## Available Profiles', '');
  for (const id of profileIds.slice(0, 10)) {
    const isSelected = id === suggestedConfig.profile ? ' **(selected)**' : '';
    lines.push(`- ${id}${isSelected}`);
  }
  if (profileIds.length > 10) {
    lines.push(`- ... and ${profileIds.length - 10} more`);
  }

  return { content: [{ type: 'text', text: lines.join('\n') }] };
}
