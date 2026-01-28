import { config } from '../../config.js';
import { listProfiles } from '../../profiles.js';

/**
 * Handler for the profiles tool.
 * Lists all available coding standards profiles.
 */
export async function handleProfiles(): Promise<{ content: Array<{ type: 'text'; text: string }> }> {
  const profiles = await listProfiles();

  if (profiles.length === 0) {
    return {
      content: [{ type: 'text', text: 'No profiles found. Run `corbat-init` to create one.' }],
    };
  }

  const lines = ['# Available Profiles', ''];

  for (const { id, profile } of profiles) {
    const isDefault = id === config.defaultProfile ? ' (default)' : '';
    lines.push(`**${id}**${isDefault}`);
    lines.push(`${profile.description || 'No description'}`);
    lines.push('');
  }

  lines.push('---', '');
  lines.push('Use with: `get_context` tool or specify profile in `.corbat.json`');

  return { content: [{ type: 'text', text: lines.join('\n') }] };
}
