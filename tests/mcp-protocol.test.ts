/**
 * MCP Protocol E2E Tests
 *
 * These tests validate the MCP server works correctly through the protocol,
 * simulating how a real MCP client would interact with the server.
 */
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js';
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import {
  CallToolRequestSchema,
  GetPromptRequestSchema,
  ListPromptsRequestSchema,
  ListResourcesRequestSchema,
  ListToolsRequestSchema,
  McpError,
  ReadResourceRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { config } from '../src/config.js';
import { handleGetPrompt, prompts } from '../src/prompts.js';
import { listResources, readResource } from '../src/resources.js';
import { handleToolCall, tools } from '../src/tools.js';

/**
 * Create a test server instance with all handlers registered.
 */
function createTestServer(): Server {
  const server = new Server(
    {
      name: config.serverName,
      version: config.serverVersion,
    },
    {
      capabilities: {
        resources: {},
        tools: {},
        prompts: {},
      },
    }
  );

  // Register tool handlers
  server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools,
  }));

  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;
    return handleToolCall(name, args ?? {});
  });

  // Register resource handlers
  server.setRequestHandler(ListResourcesRequestSchema, async () => ({
    resources: await listResources(),
  }));

  server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
    const { uri } = request.params;
    const resource = await readResource(uri);

    if (!resource) {
      throw new McpError(-32602, `Resource not found: ${uri}`);
    }

    return {
      contents: [resource],
    };
  });

  // Register prompt handlers
  server.setRequestHandler(ListPromptsRequestSchema, async () => ({
    prompts,
  }));

  server.setRequestHandler(GetPromptRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;
    const result = await handleGetPrompt(name, args);

    if (!result) {
      throw new McpError(-32602, `Prompt not found: ${name}`);
    }

    return result;
  });

  return server;
}

describe('MCP Protocol E2E Tests', () => {
  let server: Server;
  let client: Client;
  let clientTransport: InMemoryTransport;
  let serverTransport: InMemoryTransport;

  beforeEach(async () => {
    server = createTestServer();
    client = new Client(
      {
        name: 'test-client',
        version: '1.0.0',
      },
      {
        capabilities: {},
      }
    );

    // Create linked in-memory transports
    [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();

    // Connect both
    await Promise.all([client.connect(clientTransport), server.connect(serverTransport)]);
  });

  afterEach(async () => {
    await client.close();
    await server.close();
  });

  describe('Server Initialization', () => {
    it('should connect successfully', () => {
      expect(client).toBeDefined();
      expect(server).toBeDefined();
    });

    it('should report correct server info', async () => {
      const serverInfo = client.getServerVersion();
      expect(serverInfo?.name).toBe(config.serverName);
      expect(serverInfo?.version).toBe(config.serverVersion);
    });
  });

  describe('Tools', () => {
    it('should list all available tools', async () => {
      const { tools: availableTools } = await client.listTools();

      expect(availableTools).toHaveLength(5);
      expect(availableTools.map((t) => t.name)).toEqual(['get_context', 'validate', 'search', 'profiles', 'health']);
    });

    it('should call get_context tool successfully', async () => {
      const result = await client.callTool({
        name: 'get_context',
        arguments: { task: 'Create a REST API endpoint' },
      });

      expect(result.isError).toBeUndefined();
      expect(result.content).toHaveLength(1);

      const textContent = result.content[0];
      expect(textContent.type).toBe('text');
      if (textContent.type === 'text') {
        expect(textContent.text).toContain('Context for:');
        expect(textContent.text).toContain('Guardrails');
        expect(textContent.text).toContain('MUST:');
        expect(textContent.text).toContain('AVOID:');
      }
    });

    it('should call validate tool successfully', async () => {
      const result = await client.callTool({
        name: 'validate',
        arguments: {
          code: 'public class UserService { private final UserRepository repo; }',
          task_type: 'feature',
        },
      });

      expect(result.isError).toBeUndefined();
      expect(result.content).toHaveLength(1);

      const textContent = result.content[0];
      if (textContent.type === 'text') {
        expect(textContent.text).toContain('Validation');
        expect(textContent.text).toContain('FEATURE');
      }
    });

    it('should call search tool successfully', async () => {
      const result = await client.callTool({
        name: 'search',
        arguments: { query: 'testing' },
      });

      expect(result.isError).toBeUndefined();

      const textContent = result.content[0];
      if (textContent.type === 'text') {
        expect(textContent.text).toContain('Results for');
      }
    });

    it('should call profiles tool successfully', async () => {
      const result = await client.callTool({
        name: 'profiles',
        arguments: {},
      });

      expect(result.isError).toBeUndefined();

      const textContent = result.content[0];
      if (textContent.type === 'text') {
        expect(textContent.text).toContain('Available Profiles');
        expect(textContent.text).toContain('java-spring-backend');
      }
    });

    it('should call health tool successfully', async () => {
      const result = await client.callTool({
        name: 'health',
        arguments: {},
      });

      expect(result.isError).toBeUndefined();

      const textContent = result.content[0];
      if (textContent.type === 'text') {
        expect(textContent.text).toContain('Health');
        expect(textContent.text).toContain('OK');
      }
    });

    it('should handle unknown tool gracefully', async () => {
      const result = await client.callTool({
        name: 'nonexistent_tool',
        arguments: {},
      });

      expect(result.isError).toBe(true);

      const textContent = result.content[0];
      if (textContent.type === 'text') {
        expect(textContent.text).toContain('Unknown tool');
      }
    });
  });

  describe('Resources', () => {
    it('should list available resources', async () => {
      const { resources } = await client.listResources();

      expect(resources.length).toBeGreaterThan(0);

      // Should include profiles
      const profileResources = resources.filter((r) => r.uri.startsWith('corbat://profiles/'));
      expect(profileResources.length).toBeGreaterThan(0);
    });

    it('should read a profile resource', async () => {
      const { resources } = await client.listResources();

      // Find the java-spring-backend profile
      const javaProfile = resources.find((r) => r.uri.includes('java-spring-backend'));
      expect(javaProfile).toBeDefined();

      if (javaProfile) {
        const { contents } = await client.readResource({ uri: javaProfile.uri });

        expect(contents).toHaveLength(1);
        expect(contents[0].uri).toBe(javaProfile.uri);

        if ('text' in contents[0]) {
          expect(contents[0].text).toContain('Java');
        }
      }
    });

    it('should handle unknown resource gracefully', async () => {
      await expect(client.readResource({ uri: 'corbat://profiles/nonexistent' })).rejects.toThrow();
    });
  });

  describe('Prompts', () => {
    it('should list available prompts', async () => {
      const { prompts: availablePrompts } = await client.listPrompts();

      expect(availablePrompts.length).toBeGreaterThan(0);
      expect(availablePrompts.map((p) => p.name)).toContain('implement');
      expect(availablePrompts.map((p) => p.name)).toContain('review');
    });

    it('should get implement prompt', async () => {
      const result = await client.getPrompt({
        name: 'implement',
        arguments: {
          task: 'Create a new microservice',
        },
      });

      expect(result.messages).toHaveLength(1);
      expect(result.messages[0].role).toBe('user');

      const content = result.messages[0].content;
      if (content.type === 'text') {
        expect(content.text).toContain('Create a new microservice');
        expect(content.text).toContain('FEATURE');
      }
    });

    it('should get review prompt', async () => {
      const result = await client.getPrompt({
        name: 'review',
        arguments: {
          code: 'public class Test {}',
          role: 'security',
        },
      });

      expect(result.messages).toHaveLength(1);
      expect(result.messages[0].role).toBe('user');

      const content = result.messages[0].content;
      if (content.type === 'text') {
        expect(content.text).toContain('public class Test');
      }
    });

    it('should handle unknown prompt gracefully', async () => {
      await expect(
        client.getPrompt({
          name: 'nonexistent_prompt',
          arguments: {},
        })
      ).rejects.toThrow();
    });
  });

  describe('Task Type Detection', () => {
    it('should detect feature task type', async () => {
      const result = await client.callTool({
        name: 'get_context',
        arguments: { task: 'Create a new payment module' },
      });

      const textContent = result.content[0];
      if (textContent.type === 'text') {
        expect(textContent.text).toContain('FEATURE');
      }
    });

    it('should detect bugfix task type', async () => {
      const result = await client.callTool({
        name: 'get_context',
        arguments: { task: 'Fix the null pointer exception in OrderService' },
      });

      const textContent = result.content[0];
      if (textContent.type === 'text') {
        expect(textContent.text).toContain('BUGFIX');
      }
    });

    it('should detect refactor task type', async () => {
      const result = await client.callTool({
        name: 'get_context',
        arguments: { task: 'Refactor the UserRepository to use clean architecture' },
      });

      const textContent = result.content[0];
      if (textContent.type === 'text') {
        expect(textContent.text).toContain('REFACTOR');
      }
    });

    it('should detect test task type', async () => {
      const result = await client.callTool({
        name: 'get_context',
        arguments: { task: 'Write unit tests for OrderService' },
      });

      const textContent = result.content[0];
      if (textContent.type === 'text') {
        expect(textContent.text).toContain('TEST');
      }
    });
  });

  describe('Guardrails Loading', () => {
    it('should include guardrails for feature tasks', async () => {
      const result = await client.callTool({
        name: 'get_context',
        arguments: { task: 'Add new API endpoint' },
      });

      const textContent = result.content[0];
      if (textContent.type === 'text') {
        expect(textContent.text).toContain('MUST:');
        expect(textContent.text).toContain('AVOID:');
      }
    });

    it('should include guardrails for bugfix tasks', async () => {
      const result = await client.callTool({
        name: 'get_context',
        arguments: { task: 'Fix authentication bug' },
      });

      const textContent = result.content[0];
      if (textContent.type === 'text') {
        expect(textContent.text).toContain('MUST:');
        expect(textContent.text).toContain('AVOID:');
      }
    });
  });
});
