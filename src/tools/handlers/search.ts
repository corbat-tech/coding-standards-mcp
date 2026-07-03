import { formatErrorForResponse, ToolInputError } from '../../errors.js';
import { loadStandards } from '../../profiles.js';
import { SearchSchema } from '../schemas.js';

/**
 * Handler for the search tool.
 * Searches standards documentation for specific topics.
 */
export async function handleSearch(
  args: Record<string, unknown>
): Promise<{ content: Array<{ type: 'text'; text: string }>; isError?: boolean }> {
  const { query } = SearchSchema.parse(args);

  if (!query.trim()) {
    const error = new ToolInputError('search', 'Search query cannot be empty');
    return {
      content: [{ type: 'text', text: formatErrorForResponse(error) }],
      isError: true,
    };
  }

  const standards = await loadStandards();
  const queryLower = query.toLowerCase();
  const results: Array<{ name: string; category: string; excerpt: string }> = [];

  for (const standard of standards) {
    const contentLower = standard.content.toLowerCase();
    if (contentLower.includes(queryLower)) {
      // Find the relevant section
      const lines = standard.content.split('\n');
      let excerpt = '';

      for (let i = 0; i < lines.length; i++) {
        if (lines[i].toLowerCase().includes(queryLower)) {
          // Get context around match
          const start = Math.max(0, i - 2);
          const end = Math.min(lines.length, i + 5);
          excerpt = lines.slice(start, end).join('\n');
          break;
        }
      }

      results.push({
        name: standard.name,
        category: standard.category,
        excerpt: excerpt.slice(0, 500),
      });
    }
  }

  if (results.length === 0) {
    return {
      content: [
        {
          type: 'text',
          text: `No results for "${query}".\n\nTry: testing, kafka, docker, kubernetes, logging, metrics, archunit, flyway`,
        },
      ],
    };
  }

  const output: string[] = [`# Results for "${query}"`, ''];

  for (const result of results.slice(0, 5)) {
    output.push(`## ${result.name}`, '');
    output.push(result.excerpt, '');
    output.push('---', '');
  }

  return { content: [{ type: 'text', text: output.join('\n') }] };
}
