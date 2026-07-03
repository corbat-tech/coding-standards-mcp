import { access, readFile } from 'node:fs/promises';
import { join } from 'node:path';
import {
  type ExtendedGuardrails,
  formatGuardrailsAsMarkdown,
  getGuardrails as getGuardrailsFromFiles,
  loadGuardrails,
} from './guardrails.js';
import { type DetectedStack, type ProjectConfig, ProjectConfigSchema, type TaskType } from './types.js';

// Re-export guardrails utilities for external use
export { type ExtendedGuardrails, formatGuardrailsAsMarkdown, loadGuardrails };

/**
 * Stack detection patterns.
 */
interface StackPattern {
  files: string[];
  language: string;
  framework?: string;
  buildTool?: string;
  testFramework?: string;
  profile: string;
  confidence: 'high' | 'medium' | 'low';
}

const STACK_PATTERNS: StackPattern[] = [
  // Java Spring (check for Spring-specific files)
  {
    files: ['pom.xml', 'build.gradle', 'build.gradle.kts'],
    language: 'Java',
    framework: 'Spring Boot',
    buildTool: 'Maven/Gradle',
    testFramework: 'JUnit5',
    profile: 'java-spring-backend',
    confidence: 'high',
  },
  // Kotlin Spring (check for Kotlin + Spring)
  {
    files: ['build.gradle.kts', 'settings.gradle.kts'],
    language: 'Kotlin',
    framework: 'Spring Boot',
    buildTool: 'Gradle',
    testFramework: 'JUnit5/Kotest',
    profile: 'kotlin-spring',
    confidence: 'medium',
  },
  // Go
  {
    files: ['go.mod', 'go.sum'],
    language: 'Go',
    buildTool: 'go',
    testFramework: 'testing',
    profile: 'go',
    confidence: 'high',
  },
  // Rust
  {
    files: ['Cargo.toml', 'Cargo.lock'],
    language: 'Rust',
    buildTool: 'cargo',
    testFramework: 'built-in',
    profile: 'rust',
    confidence: 'high',
  },
  // C# / .NET
  {
    files: ['*.csproj', '*.sln'],
    language: 'C#',
    framework: 'ASP.NET Core',
    buildTool: 'dotnet',
    testFramework: 'xUnit',
    profile: 'csharp-dotnet',
    confidence: 'high',
  },
  // Flutter / Dart
  {
    files: ['pubspec.yaml', 'pubspec.lock'],
    language: 'Dart',
    framework: 'Flutter',
    buildTool: 'flutter',
    testFramework: 'flutter_test',
    profile: 'flutter',
    confidence: 'high',
  },
  // Next.js (must come before generic React)
  {
    files: ['next.config.js', 'next.config.mjs', 'next.config.ts'],
    language: 'TypeScript',
    framework: 'Next.js',
    buildTool: 'npm/pnpm',
    testFramework: 'Vitest/Jest',
    profile: 'nextjs',
    confidence: 'high',
  },
  // Angular (must come before generic Node.js)
  {
    files: ['angular.json'],
    language: 'TypeScript',
    framework: 'Angular',
    buildTool: 'Angular CLI',
    testFramework: 'Jest/Vitest',
    profile: 'angular',
    confidence: 'high',
  },
  // Vue.js
  {
    files: ['vue.config.js', 'vite.config.ts', 'nuxt.config.ts'],
    language: 'TypeScript',
    framework: 'Vue',
    buildTool: 'Vite',
    testFramework: 'Vitest',
    profile: 'vue',
    confidence: 'medium',
  },
  // React (Vite)
  {
    files: ['package.json', 'vite.config.ts', 'vite.config.js'],
    language: 'TypeScript',
    framework: 'React',
    buildTool: 'Vite',
    testFramework: 'Vitest',
    profile: 'react',
    confidence: 'medium',
  },
  // Node.js/TypeScript
  {
    files: ['package.json', 'tsconfig.json'],
    language: 'TypeScript',
    framework: 'Node.js',
    buildTool: 'npm/pnpm',
    testFramework: 'Vitest/Jest',
    profile: 'nodejs',
    confidence: 'high',
  },
  // Python
  {
    files: ['pyproject.toml', 'requirements.txt', 'setup.py'],
    language: 'Python',
    framework: 'FastAPI/Django',
    buildTool: 'pip/poetry',
    testFramework: 'pytest',
    profile: 'python',
    confidence: 'high',
  },
  // Generic JavaScript
  {
    files: ['package.json'],
    language: 'JavaScript',
    buildTool: 'npm',
    profile: 'nodejs',
    confidence: 'low',
  },
];

/**
 * Load project configuration from .corbat.json
 */
export async function loadProjectConfig(projectDir: string): Promise<ProjectConfig | null> {
  const configPath = join(projectDir, '.corbat.json');

  try {
    await access(configPath);
    const content = await readFile(configPath, 'utf-8');
    const rawConfig = JSON.parse(content);
    return ProjectConfigSchema.parse(rawConfig);
  } catch {
    return null;
  }
}

/**
 * Detect project stack from file system.
 */
export async function detectProjectStack(projectDir: string): Promise<DetectedStack | null> {
  const detectedFiles: string[] = [];

  for (const pattern of STACK_PATTERNS) {
    for (const file of pattern.files) {
      const filePath = join(projectDir, file);
      try {
        await access(filePath);
        detectedFiles.push(file);
      } catch {
        // File doesn't exist, continue
      }
    }

    if (detectedFiles.length > 0) {
      // Additional detection for more specific frameworks
      let framework = pattern.framework;
      let testFramework = pattern.testFramework;
      let suggestedProfile = pattern.profile;

      // Check for specific framework indicators
      if (detectedFiles.includes('package.json')) {
        try {
          const packageJson = JSON.parse(await readFile(join(projectDir, 'package.json'), 'utf-8'));
          const deps = { ...packageJson.dependencies, ...packageJson.devDependencies };

          // Frontend frameworks (set profile accordingly)
          if (deps.react) {
            framework = 'React';
            suggestedProfile = 'react';
          } else if (deps.vue) {
            framework = 'Vue';
            suggestedProfile = 'vue';
          } else if (deps['@angular/core']) {
            framework = 'Angular';
            suggestedProfile = 'angular';
          } else if (deps.express) {
            framework = 'Express';
          } else if (deps.fastify) {
            framework = 'Fastify';
          } else if (deps.nestjs || deps['@nestjs/core']) {
            framework = 'NestJS';
          }

          if (deps.vitest) testFramework = 'Vitest';
          else if (deps.jest) testFramework = 'Jest';
          else if (deps.mocha) testFramework = 'Mocha';
        } catch {
          // Unable to parse package.json
        }
      }

      // Check for Spring Boot specific indicators
      if (detectedFiles.includes('pom.xml')) {
        try {
          const pomContent = await readFile(join(projectDir, 'pom.xml'), 'utf-8');
          if (pomContent.includes('spring-boot')) {
            framework = 'Spring Boot';
          }
        } catch {
          // Unable to read pom.xml
        }
      }

      return {
        language: pattern.language,
        framework,
        buildTool: pattern.buildTool,
        testFramework,
        suggestedProfile,
        confidence: pattern.confidence,
        detectedFiles,
      };
    }
  }

  return null;
}

/**
 * Get guardrails for a specific task type.
 * Loads from YAML files and merges with project-specific overrides.
 */
export async function getGuardrails(
  taskType: TaskType,
  projectConfig?: ProjectConfig | null
): Promise<ExtendedGuardrails> {
  // Load from YAML files
  const baseGuardrails = await getGuardrailsFromFiles(taskType);

  // Create a copy to avoid mutating the cached version
  const guardrails: ExtendedGuardrails = {
    ...baseGuardrails,
    mandatory: [...baseGuardrails.mandatory],
    recommended: [...baseGuardrails.recommended],
    avoid: [...baseGuardrails.avoid],
  };

  // Override with project-specific guardrails if available
  if (projectConfig?.guardrails?.[taskType]) {
    const projectGuardrails = projectConfig.guardrails[taskType];
    guardrails.mandatory = [...guardrails.mandatory, ...projectGuardrails.mandatory];
    guardrails.recommended = [...guardrails.recommended, ...projectGuardrails.recommended];
    guardrails.avoid = [...guardrails.avoid, ...projectGuardrails.avoid];
  }

  return guardrails;
}

/**
 * Get project rules (always rules + task-specific rules).
 */
export function getProjectRules(taskType: TaskType, projectConfig?: ProjectConfig | null): string[] {
  if (!projectConfig?.rules) return [];

  const rules: string[] = [...(projectConfig.rules.always || [])];

  switch (taskType) {
    case 'feature':
      rules.push(...(projectConfig.rules.onNewFile || []));
      break;
    case 'test':
      rules.push(...(projectConfig.rules.onTest || []));
      break;
    case 'refactor':
      rules.push(...(projectConfig.rules.onRefactor || []));
      break;
  }

  return rules;
}

/**
 * Classify task type from description.
 */
export function classifyTaskType(description: string): TaskType {
  const desc = description.toLowerCase();

  // Bug/fix patterns
  if (
    desc.includes('fix') ||
    desc.includes('bug') ||
    desc.includes('error') ||
    desc.includes('issue') ||
    desc.includes('problem') ||
    desc.includes('broken')
  ) {
    return 'bugfix';
  }

  // Refactor patterns
  if (
    desc.includes('refactor') ||
    desc.includes('cleanup') ||
    desc.includes('clean up') ||
    desc.includes('reorganize') ||
    desc.includes('restructure') ||
    desc.includes('improve structure')
  ) {
    return 'refactor';
  }

  // Test patterns
  if (
    desc.includes('test') ||
    desc.includes('spec') ||
    desc.includes('coverage') ||
    desc.includes('unit test') ||
    desc.includes('integration test')
  ) {
    return 'test';
  }

  // Documentation patterns
  if (
    desc.includes('document') ||
    desc.includes('readme') ||
    desc.includes('comment') ||
    desc.includes('jsdoc') ||
    desc.includes('javadoc')
  ) {
    return 'documentation';
  }

  // Performance patterns
  if (
    desc.includes('performance') ||
    desc.includes('optimize') ||
    desc.includes('speed') ||
    desc.includes('slow') ||
    desc.includes('memory') ||
    desc.includes('cache')
  ) {
    return 'performance';
  }

  // Security patterns
  if (
    desc.includes('security') ||
    desc.includes('auth') ||
    desc.includes('permission') ||
    desc.includes('vulnerability') ||
    desc.includes('secure') ||
    desc.includes('encrypt')
  ) {
    return 'security';
  }

  // Infrastructure patterns
  if (
    desc.includes('deploy') ||
    desc.includes('docker') ||
    desc.includes('kubernetes') ||
    desc.includes('ci/cd') ||
    desc.includes('pipeline') ||
    desc.includes('infrastructure')
  ) {
    return 'infrastructure';
  }

  // Default to feature
  return 'feature';
}

/**
 * Technical decision templates by category.
 */
export const TECHNICAL_DECISIONS: Record<
  string,
  {
    options: Array<{
      name: string;
      description: string;
      pros: string[];
      cons: string[];
      useWhen: string[];
    }>;
    defaultRecommendation: string;
  }
> = {
  database: {
    options: [
      {
        name: 'PostgreSQL',
        description: 'Advanced open-source relational database',
        pros: ['ACID compliant', 'Rich feature set', 'Excellent JSON support', 'Strong community'],
        cons: ['More complex setup', 'Higher resource usage'],
        useWhen: ['Complex queries needed', 'Data integrity critical', 'JSON flexibility needed'],
      },
      {
        name: 'MySQL',
        description: 'Popular open-source relational database',
        pros: ['Simple to use', 'Wide adoption', 'Good performance for reads'],
        cons: ['Less advanced features', 'Replication complexity'],
        useWhen: ['Simple CRUD operations', 'Read-heavy workloads', 'Team familiarity'],
      },
      {
        name: 'MongoDB',
        description: 'Document-oriented NoSQL database',
        pros: ['Flexible schema', 'Horizontal scaling', 'Developer friendly'],
        cons: ['No ACID by default', 'Memory intensive', 'Complex aggregations'],
        useWhen: ['Schema evolution expected', 'Document-oriented data', 'Rapid prototyping'],
      },
    ],
    defaultRecommendation: 'PostgreSQL',
  },
  cache: {
    options: [
      {
        name: 'Redis',
        description: 'In-memory data structure store',
        pros: ['Very fast', 'Rich data structures', 'Pub/sub support', 'Persistence options'],
        cons: ['Memory bound', 'Single-threaded'],
        useWhen: ['High-performance caching', 'Session storage', 'Real-time features'],
      },
      {
        name: 'In-memory (local)',
        description: 'Application-level caching',
        pros: ['No external dependency', 'Fastest access', 'Simple setup'],
        cons: ['Not shared across instances', 'Limited by heap size', 'Lost on restart'],
        useWhen: ['Single instance deployment', 'Small cache size', 'Local reference data'],
      },
      {
        name: 'Memcached',
        description: 'Distributed memory caching',
        pros: ['Simple', 'Multi-threaded', 'Predictable performance'],
        cons: ['Limited data types', 'No persistence', 'No pub/sub'],
        useWhen: ['Simple key-value caching', 'Multiple servers', 'Volatile data only'],
      },
    ],
    defaultRecommendation: 'Redis',
  },
  messaging: {
    options: [
      {
        name: 'Apache Kafka',
        description: 'Distributed streaming platform',
        pros: ['High throughput', 'Durable', 'Replay capability', 'Strong ordering'],
        cons: ['Complex setup', 'Higher latency', 'Operational overhead'],
        useWhen: ['Event sourcing', 'High volume', 'Data pipeline', 'Audit requirements'],
      },
      {
        name: 'RabbitMQ',
        description: 'Message broker with routing',
        pros: ['Flexible routing', 'Lower latency', 'Easier setup', 'Good for RPC'],
        cons: ['Lower throughput', 'Less durable by default'],
        useWhen: ['Complex routing', 'Request-reply patterns', 'Lower volume'],
      },
      {
        name: 'AWS SQS',
        description: 'Managed message queue service',
        pros: ['Fully managed', 'Highly available', 'Pay per use', 'Simple'],
        cons: ['AWS lock-in', 'Limited features', 'Higher latency'],
        useWhen: ['AWS infrastructure', 'Simple queuing', 'Minimal ops'],
      },
    ],
    defaultRecommendation: 'Apache Kafka',
  },
  authentication: {
    options: [
      {
        name: 'JWT (JSON Web Tokens)',
        description: 'Stateless token-based authentication',
        pros: ['Stateless', 'Scalable', 'Cross-domain support', 'Self-contained'],
        cons: ['Cannot revoke easily', 'Token size', 'Key management'],
        useWhen: ['Microservices', 'API authentication', 'Cross-domain auth'],
      },
      {
        name: 'Session-based',
        description: 'Server-side session storage',
        pros: ['Easy revocation', 'Simple implementation', 'Smaller payload'],
        cons: ['Server state', 'Scaling challenges', 'CSRF concerns'],
        useWhen: ['Monolithic apps', 'Web applications', 'High security needs'],
      },
      {
        name: 'OAuth 2.0 / OIDC',
        description: 'Delegated authorization protocol',
        pros: ['Standard protocol', 'Third-party auth', 'Fine-grained scopes'],
        cons: ['Complex implementation', 'Multiple flows'],
        useWhen: ['Third-party integration', 'SSO requirements', 'API access delegation'],
      },
    ],
    defaultRecommendation: 'JWT (JSON Web Tokens)',
  },
  testing: {
    options: [
      {
        name: 'Unit + Integration + E2E',
        description: 'Full testing pyramid',
        pros: ['Comprehensive coverage', 'Fast feedback', 'Confidence in changes'],
        cons: ['Time investment', 'Maintenance overhead'],
        useWhen: ['Production systems', 'Team projects', 'Critical business logic'],
      },
      {
        name: 'Unit + Integration only',
        description: 'Focus on unit and integration tests',
        pros: ['Good balance', 'Faster execution', 'Less flaky'],
        cons: ['May miss UI issues', 'Less end-to-end confidence'],
        useWhen: ['API services', 'Libraries', 'Time constraints'],
      },
      {
        name: 'TDD (Test-Driven Development)',
        description: 'Write tests before implementation',
        pros: ['Better design', 'High coverage', 'Living documentation'],
        cons: ['Learning curve', 'Initial slowdown'],
        useWhen: ['Complex business logic', 'Quality-critical code', 'New features'],
      },
    ],
    defaultRecommendation: 'Unit + Integration + E2E',
  },
};

/**
 * Get a technical decision recommendation.
 */
export function getTechnicalDecision(
  category: string,
  _context: string,
  projectConfig?: ProjectConfig | null
): {
  options: Array<{
    name: string;
    description: string;
    pros: string[];
    cons: string[];
    useWhen: string[];
  }>;
  recommendation: string;
  reasoning: string;
} | null {
  const decision = TECHNICAL_DECISIONS[category];
  if (!decision) return null;

  // Check if project has a predefined decision
  const predefinedDecision = projectConfig?.decisions?.[category];
  if (predefinedDecision) {
    const option = decision.options.find((o) => o.name.toLowerCase() === predefinedDecision.toLowerCase());
    if (option) {
      return {
        options: decision.options,
        recommendation: option.name,
        reasoning: `Project configuration specifies ${option.name} for ${category}. This aligns with the team's architectural decisions.`,
      };
    }
  }

  return {
    options: decision.options,
    recommendation: decision.defaultRecommendation,
    reasoning: `${decision.defaultRecommendation} is recommended as the default choice for ${category} based on industry best practices and versatility.`,
  };
}

// formatGuardrailsAsMarkdown is now exported from guardrails.ts
