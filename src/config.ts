import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { z } from 'zod';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

/**
 * Environment type for configuration.
 */
export type Environment = 'development' | 'production' | 'test';

/**
 * Environment-specific configuration overrides.
 */
const environmentConfigs: Record<Environment, Partial<EnvironmentConfig>> = {
  development: {
    cacheTtlMs: 60_000, // 1 minute - shorter for faster iteration
    logLevel: 'debug',
    enableVerboseErrors: true,
  },
  production: {
    cacheTtlMs: 300_000, // 5 minutes - longer for better performance
    logLevel: 'info',
    enableVerboseErrors: false,
  },
  test: {
    cacheTtlMs: 0, // No cache in tests for predictability
    logLevel: 'error',
    enableVerboseErrors: true,
  },
};

/**
 * Environment-specific configuration schema.
 */
const EnvironmentConfigSchema = z.object({
  cacheTtlMs: z.number().min(0).default(60_000),
  logLevel: z.enum(['debug', 'info', 'warn', 'error']).default('info'),
  enableVerboseErrors: z.boolean().default(false),
});

type EnvironmentConfig = z.infer<typeof EnvironmentConfigSchema>;

/**
 * Application configuration schema with Zod validation.
 */
const ConfigSchema = z.object({
  profilesDir: z.string().min(1),
  standardsDir: z.string().min(1),
  defaultProfile: z.string().default('java-spring-backend'),
  serverName: z.string().default('coding-standards-mcp'),
  serverVersion: z.string().default('1.0.0'),
  environment: z.enum(['development', 'production', 'test']).default('development'),
  cacheTtlMs: z.number().min(0).default(60_000),
  logLevel: z.enum(['debug', 'info', 'warn', 'error']).default('info'),
  enableVerboseErrors: z.boolean().default(false),
});

export type Config = z.infer<typeof ConfigSchema>;

/** Valid log levels for validation */
const VALID_LOG_LEVELS = ['debug', 'info', 'warn', 'error'] as const;

/**
 * Detect current environment from NODE_ENV or CORBAT_ENV.
 * Case-insensitive comparison for robustness.
 */
function detectEnvironment(): Environment {
  const env = (process.env.CORBAT_ENV || process.env.NODE_ENV || 'development').toLowerCase();
  if (env === 'production' || env === 'prod') return 'production';
  if (env === 'test') return 'test';
  return 'development';
}

/**
 * Safely parse an integer from environment variable with fallback.
 */
function parseIntWithFallback(value: string | undefined, fallback: number): number {
  if (!value) return fallback;
  const parsed = Number.parseInt(value, 10);
  return Number.isNaN(parsed) ? fallback : parsed;
}

/**
 * Validate and return log level from environment variable.
 */
function validateLogLevel(value: string | undefined, fallback: Config['logLevel']): Config['logLevel'] {
  if (!value) return fallback;
  const normalized = value.toLowerCase() as Config['logLevel'];
  return VALID_LOG_LEVELS.includes(normalized) ? normalized : fallback;
}

/**
 * Load and validate configuration with environment-specific overrides.
 */
export function loadConfig(): Config {
  const rootDir = join(__dirname, '..');
  const environment = detectEnvironment();
  const envConfig = environmentConfigs[environment];

  return ConfigSchema.parse({
    profilesDir: process.env.CORBAT_PROFILES_DIR || join(rootDir, 'profiles'),
    standardsDir: process.env.CORBAT_STANDARDS_DIR || join(rootDir, 'standards'),
    defaultProfile: process.env.CORBAT_DEFAULT_PROFILE || 'java-spring-backend',
    serverName: 'coding-standards-mcp',
    serverVersion: '3.0.0',
    environment,
    cacheTtlMs: parseIntWithFallback(process.env.CORBAT_CACHE_TTL_MS, envConfig.cacheTtlMs ?? 60_000),
    logLevel: validateLogLevel(process.env.CORBAT_LOG_LEVEL, envConfig.logLevel ?? 'info'),
    enableVerboseErrors: process.env.CORBAT_VERBOSE_ERRORS === 'true' || envConfig.enableVerboseErrors,
  });
}

export const config = loadConfig();

/**
 * Get current environment.
 */
export function getEnvironment(): Environment {
  return config.environment;
}

/**
 * Check if running in production.
 */
export function isProduction(): boolean {
  return config.environment === 'production';
}

/**
 * Check if running in development.
 */
export function isDevelopment(): boolean {
  return config.environment === 'development';
}

/**
 * Check if running in test environment.
 */
export function isTest(): boolean {
  return config.environment === 'test';
}
