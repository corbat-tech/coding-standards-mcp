# Plan de Mejora: Corbat MCP hacia el 9.5/10

## Resumen Ejecutivo

Este plan detalla las mejoras necesarias para elevar Corbat MCP de **8.6/10** a **9.5/10**. Las mejoras están organizadas por prioridad e impacto, diseñadas para ser ejecutadas de forma incremental sin sobrecargar el desarrollo.

---

## Puntuacion Actual vs Objetivo

| Categoria          | Actual | Objetivo | Delta |
|--------------------|--------|----------|-------|
| Arquitectura       | 8.5    | 9.5      | +1.0  |
| Calidad de Codigo  | 8.0    | 9.0      | +1.0  |
| Diseno MCP         | 9.0    | 9.5      | +0.5  |
| Utilidad/Valor     | 9.5    | 9.8      | +0.3  |
| Extensibilidad     | 8.5    | 9.5      | +1.0  |
| Testing            | 7.5    | 9.5      | +2.0  |
| Documentacion      | 8.0    | 9.0      | +1.0  |

---

## FASE 1: Fundamentos de Calidad (Prioridad Alta)

### 1.1 Refactorizar Arquitectura de Handlers

**Problema:** `src/tools.ts` mezcla definiciones de tools con logica de handlers (475 lineas).

**Solucion:** Separar en modulos especializados.

```
src/
  tools/
    definitions.ts    # Solo definiciones de tools (schemas)
    handlers/
      index.ts        # Re-exporta handlers
      get-context.ts  # Handler para get_context
      validate.ts     # Handler para validate
      search.ts       # Handler para search
      profiles.ts     # Handler para profiles
      health.ts       # Handler para health
    schemas.ts        # Zod schemas compartidos
```

**Beneficios:**
- Cada handler es testeable de forma aislada
- Mejor cohesion (un archivo = una responsabilidad)
- Facilita agregar nuevos handlers sin tocar los existentes

**Archivos a crear:**
- `src/tools/definitions.ts`
- `src/tools/schemas.ts`
- `src/tools/handlers/get-context.ts`
- `src/tools/handlers/validate.ts`
- `src/tools/handlers/search.ts`
- `src/tools/handlers/profiles.ts`
- `src/tools/handlers/health.ts`
- `src/tools/handlers/index.ts`
- `src/tools/index.ts` (re-exporta todo)

**Impacto:** Arquitectura +0.5, Testing +0.3

---

### 1.2 Sistema de Herencia de Profiles

**Problema:** Los profiles no pueden extender otros profiles, causando duplicacion.

**Solucion:** Agregar soporte para `extends` en profiles.

```yaml
# profiles/templates/my-custom.yaml
name: "My Custom Profile"
extends: "java-spring-backend"  # Hereda todo de java-spring-backend

# Solo sobrescribe lo que necesita
codeQuality:
  maxMethodLines: 15  # Mas estricto que el padre (20)
  minimumTestCoverage: 90  # Mas estricto que el padre (80)
```

**Implementacion en `src/profiles.ts`:**
```typescript
async function resolveProfileInheritance(profile: Profile, allProfiles: Map<string, Profile>): Promise<Profile> {
  if (!profile.extends) return profile;

  const parent = allProfiles.get(profile.extends);
  if (!parent) throw new Error(`Parent profile "${profile.extends}" not found`);

  // Merge recursivo: hijo sobrescribe padre
  return deepMerge(await resolveProfileInheritance(parent, allProfiles), profile);
}
```

**Impacto:** Extensibilidad +0.5, Utilidad +0.2

---

### 1.3 Mejorar Cobertura de Tests

**Problema:**
- No hay badge de coverage en README
- Tests son mayormente de integracion, faltan unitarios aislados
- No hay tests para edge cases en deteccion de stack

**Solucion:**

**A) Agregar tests unitarios aislados:**

```typescript
// tests/unit/agent.test.ts
describe('classifyTaskType', () => {
  it.each([
    ['fix login bug', 'bugfix'],
    ['Fix the broken API', 'bugfix'],
    ['refactor user service', 'refactor'],
    ['add new payment feature', 'feature'],
    ['improve performance of queries', 'performance'],
    ['secure the authentication flow', 'security'],
    ['deploy to kubernetes', 'infrastructure'],
    ['write unit tests for OrderService', 'test'],
    ['document the API endpoints', 'documentation'],
  ])('classifies "%s" as %s', (input, expected) => {
    expect(classifyTaskType(input)).toBe(expected);
  });
});
```

```typescript
// tests/unit/profiles.test.ts
describe('Profile Inheritance', () => {
  it('should merge child profile over parent', async () => {
    // Test que el hijo sobrescribe al padre
  });

  it('should handle deep nested inheritance', async () => {
    // Test herencia de multiples niveles
  });

  it('should throw error if parent profile not found', async () => {
    // Test error handling
  });
});
```

**B) Agregar configuracion de coverage:**

```typescript
// vitest.config.ts
export default defineConfig({
  test: {
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html', 'lcov'],
      exclude: ['tests/**', 'dist/**', '*.config.*'],
      thresholds: {
        lines: 80,
        branches: 75,
        functions: 80,
        statements: 80,
      },
    },
  },
});
```

**C) Badge en README:**
```markdown
![Coverage](https://img.shields.io/badge/coverage-85%25-brightgreen)
```

**Impacto:** Testing +1.5

---

## FASE 2: Mejoras de Usabilidad (Prioridad Media)

### 2.1 Mejorar Descripciones de Tools para LLMs

**Problema:** Las descripciones actuales son buenas pero pueden ser mas claras para LLMs.

**Mejora:**

```typescript
// ANTES
{
  name: 'get_context',
  description: 'Get COMPLETE coding standards context for a task...',
}

// DESPUES
{
  name: 'get_context',
  description: `Returns coding standards, guardrails, and workflow for implementing a task.

WHEN TO USE:
- ALWAYS call this FIRST before writing any code
- When starting a new feature, bugfix, or refactor
- When unsure about project conventions

RETURNS:
- Detected stack (Java/Python/TypeScript/etc)
- Task type classification (feature/bugfix/refactor/test)
- MUST rules (mandatory guidelines)
- AVOID rules (anti-patterns to prevent)
- Code quality thresholds (max lines, coverage)
- Naming conventions
- Recommended workflow

EXAMPLE: get_context({ task: "Create payment service" })`,
}
```

**Impacto:** Diseno MCP +0.3, Utilidad +0.1

---

### 2.2 Agregar Tool `init` para Inicializacion Guiada

**Problema:** El CLI `corbat-init` existe pero no hay una tool MCP equivalente.

**Solucion:** Agregar tool `init` que genera `.corbat.json` interactivamente.

```typescript
{
  name: 'init',
  description: `Generate a .corbat.json configuration file for a project.

Analyzes the project directory and suggests optimal configuration based on detected stack.

RETURNS: Suggested .corbat.json content that can be saved to the project root.`,
  inputSchema: {
    type: 'object',
    properties: {
      project_dir: {
        type: 'string',
        description: 'Project directory to analyze',
      },
    },
    required: ['project_dir'],
  },
}
```

**Handler:**
```typescript
async function handleInit(args: { project_dir: string }) {
  const stack = await detectProjectStack(args.project_dir);
  const suggestedConfig = generateSuggestedConfig(stack);

  return {
    content: [{
      type: 'text',
      text: `# Suggested .corbat.json for your project

Based on detected stack: ${stack?.language} ${stack?.framework || ''}

\`\`\`json
${JSON.stringify(suggestedConfig, null, 2)}
\`\`\`

Save this to \`${args.project_dir}/.corbat.json\` to customize standards.`
    }]
  };
}
```

**Impacto:** Utilidad +0.2, Diseno MCP +0.2

---

### 2.3 Agregar Ejemplos de Custom Profiles

**Problema:** No hay ejemplos de como crear profiles custom.

**Solucion:** Crear `profiles/examples/` con casos comunes.

**Archivos a crear:**

```yaml
# profiles/examples/strict-enterprise.yaml
# Para proyectos enterprise con requisitos estrictos
name: "Strict Enterprise"
extends: "java-spring-backend"
codeQuality:
  maxMethodLines: 15
  maxClassLines: 150
  minimumTestCoverage: 90
  maxCyclomaticComplexity: 8
```

```yaml
# profiles/examples/startup-fast.yaml
# Para startups que priorizan velocidad
name: "Startup Fast"
extends: "nodejs"
codeQuality:
  maxMethodLines: 30
  minimumTestCoverage: 60
testing:
  types:
    unit:
      coverage: 60
```

```yaml
# profiles/examples/microservice-kafka.yaml
# Para microservicios event-driven
name: "Microservice Kafka"
extends: "java-spring-backend"
eventDriven:
  enabled: true
  approach: "event-sourcing"
  patterns:
    messaging:
      broker: "kafka"
```

**Impacto:** Documentacion +0.5, Extensibilidad +0.3

---

## FASE 3: Robustez y Observabilidad (Prioridad Media)

### 3.1 Agregar Logging Estructurado

**Problema:** Solo hay `console.error` para errores fatales, no hay visibilidad de operaciones.

**Solucion:** Agregar logging estructurado opcional.

```typescript
// src/logger.ts
import { config } from './config.js';

type LogLevel = 'debug' | 'info' | 'warn' | 'error';

interface LogEntry {
  level: LogLevel;
  message: string;
  timestamp: string;
  context?: Record<string, unknown>;
}

function shouldLog(level: LogLevel): boolean {
  const levels: LogLevel[] = ['debug', 'info', 'warn', 'error'];
  return levels.indexOf(level) >= levels.indexOf(config.logLevel);
}

export function log(level: LogLevel, message: string, context?: Record<string, unknown>): void {
  if (!shouldLog(level)) return;

  const entry: LogEntry = {
    level,
    message,
    timestamp: new Date().toISOString(),
    context,
  };

  // Siempre a stderr para no interferir con stdio MCP
  console.error(JSON.stringify(entry));
}

export const logger = {
  debug: (msg: string, ctx?: Record<string, unknown>) => log('debug', msg, ctx),
  info: (msg: string, ctx?: Record<string, unknown>) => log('info', msg, ctx),
  warn: (msg: string, ctx?: Record<string, unknown>) => log('warn', msg, ctx),
  error: (msg: string, ctx?: Record<string, unknown>) => log('error', msg, ctx),
};
```

**Uso:**
```typescript
// En handlers
logger.info('get_context called', { task, taskType, profile: profileId });
logger.debug('Stack detected', { stack: detectedStack });
```

**Impacto:** Calidad +0.3

---

### 3.2 Mejorar Manejo de Errores

**Problema:** Los errores no son consistentes ni informativos.

**Solucion:** Crear sistema de errores tipados.

```typescript
// src/errors.ts
export class CorbatError extends Error {
  constructor(
    message: string,
    public readonly code: string,
    public readonly details?: Record<string, unknown>
  ) {
    super(message);
    this.name = 'CorbatError';
  }
}

export class ProfileNotFoundError extends CorbatError {
  constructor(profileId: string, availableProfiles: string[]) {
    super(
      `Profile "${profileId}" not found`,
      'PROFILE_NOT_FOUND',
      { profileId, availableProfiles }
    );
  }
}

export class InvalidConfigError extends CorbatError {
  constructor(path: string, validationErrors: string[]) {
    super(
      `Invalid configuration at ${path}`,
      'INVALID_CONFIG',
      { path, validationErrors }
    );
  }
}

export class StackDetectionError extends CorbatError {
  constructor(projectDir: string, reason: string) {
    super(
      `Could not detect stack in ${projectDir}: ${reason}`,
      'STACK_DETECTION_FAILED',
      { projectDir, reason }
    );
  }
}
```

**Impacto:** Calidad +0.2, Arquitectura +0.2

---

### 3.3 Metricas de Uso en Health

**Problema:** El health check no muestra metricas de uso.

**Mejora:**

```typescript
// src/metrics.ts
interface Metrics {
  toolCalls: Record<string, number>;
  profilesUsed: Record<string, number>;
  taskTypes: Record<string, number>;
  errors: number;
  startTime: number;
}

const metrics: Metrics = {
  toolCalls: {},
  profilesUsed: {},
  taskTypes: {},
  errors: 0,
  startTime: Date.now(),
};

export function recordToolCall(toolName: string): void {
  metrics.toolCalls[toolName] = (metrics.toolCalls[toolName] || 0) + 1;
}

export function recordProfileUsed(profileId: string): void {
  metrics.profilesUsed[profileId] = (metrics.profilesUsed[profileId] || 0) + 1;
}

export function recordTaskType(taskType: string): void {
  metrics.taskTypes[taskType] = (metrics.taskTypes[taskType] || 0) + 1;
}

export function recordError(): void {
  metrics.errors++;
}

export function getMetrics(): Metrics & { uptimeMs: number } {
  return {
    ...metrics,
    uptimeMs: Date.now() - metrics.startTime,
  };
}
```

**Health mejorado:**
```typescript
async function handleHealth() {
  const m = getMetrics();

  return {
    content: [{
      type: 'text',
      text: `# Corbat MCP Health

**Status:** OK
**Version:** ${config.serverVersion}
**Uptime:** ${formatDuration(m.uptimeMs)}

## Metrics
- Total tool calls: ${Object.values(m.toolCalls).reduce((a, b) => a + b, 0)}
- Most used tool: ${getMostUsed(m.toolCalls)}
- Most used profile: ${getMostUsed(m.profilesUsed)}
- Task type distribution: ${formatDistribution(m.taskTypes)}
- Errors: ${m.errors}

## Resources
- Profiles loaded: ${profiles.length}
- Standards documents: ${standards.length}`
    }]
  };
}
```

**Impacto:** Diseno MCP +0.2

---

## FASE 4: Documentacion de Primera Clase (Prioridad Media)

### 4.1 Crear CONTRIBUTING.md

```markdown
# Contributing to Corbat MCP

## Development Setup

1. Clone the repository
2. Install dependencies: `npm install`
3. Run tests: `npm test`
4. Build: `npm run build`

## Project Structure

\`\`\`
src/
  index.ts          # Entry point
  config.ts         # Configuration
  tools/            # Tool definitions and handlers
  profiles.ts       # Profile loading
  guardrails.ts     # Guardrails loading
  agent.ts          # Stack detection and classification
\`\`\`

## Adding a New Profile

1. Create `profiles/templates/my-profile.yaml`
2. Follow the schema in `src/types.ts`
3. Add tests in `tests/profiles.test.ts`

## Adding a New Tool

1. Add definition in `src/tools/definitions.ts`
2. Create handler in `src/tools/handlers/my-tool.ts`
3. Export from `src/tools/handlers/index.ts`
4. Add tests in `tests/handlers.test.ts`

## Code Style

- Use Biome for formatting: `npm run format`
- Run linter: `npm run lint`
- Follow existing patterns

## Testing

- Unit tests: `tests/unit/`
- Integration tests: `tests/`
- Run all: `npm test`
- Coverage: `npm run test:coverage`

## Pull Request Process

1. Create feature branch
2. Make changes
3. Ensure tests pass
4. Update documentation if needed
5. Submit PR
```

**Impacto:** Documentacion +0.3

---

### 4.2 Documentar API de Tools (OpenAPI-like)

Crear `docs/api-reference.md`:

```markdown
# API Reference

## Tools

### get_context

Returns coding standards context for a task.

**Input Schema:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| task | string | Yes | Description of what to implement |
| project_dir | string | No | Project directory for auto-detection |

**Output:** Markdown with stack, guardrails, naming conventions, workflow.

**Example:**
\`\`\`json
{
  "task": "Create payment service",
  "project_dir": "/path/to/project"
}
\`\`\`

### validate

Validates code against standards.

**Input Schema:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| code | string | Yes | Code to validate |
| task_type | enum | No | One of: feature, bugfix, refactor, test |

...
```

**Impacto:** Documentacion +0.3

---

## FASE 5: Pulido Final (Prioridad Baja)

### 5.1 Optimizar Tamano de Respuestas

**Problema:** Las respuestas pueden ser muy largas para contextos LLM limitados.

**Solucion:** Agregar parametro `verbosity` opcional.

```typescript
{
  name: 'get_context',
  inputSchema: {
    properties: {
      task: { type: 'string' },
      project_dir: { type: 'string' },
      verbosity: {
        type: 'string',
        enum: ['minimal', 'standard', 'full'],
        description: 'Level of detail in response. Default: standard',
      },
    },
  },
}
```

- `minimal`: Solo MUST rules y workflow (para contextos limitados)
- `standard`: Lo actual (default)
- `full`: Todo incluyendo ejemplos de codigo

**Impacto:** Utilidad +0.1

---

### 5.2 Validacion de .corbat.json con Schema JSON

Publicar JSON Schema para validacion en editores:

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "https://corbat.tech/schemas/corbat-config.json",
  "title": "Corbat MCP Configuration",
  "type": "object",
  "properties": {
    "profile": {
      "type": "string",
      "description": "Profile ID to use"
    },
    "rules": {
      "type": "object",
      "properties": {
        "always": { "type": "array", "items": { "type": "string" } },
        "onNewFile": { "type": "array", "items": { "type": "string" } },
        "onTest": { "type": "array", "items": { "type": "string" } },
        "onRefactor": { "type": "array", "items": { "type": "string" } }
      }
    }
  }
}
```

Agregar a package.json:
```json
{
  "contributes": {
    "jsonValidation": [{
      "fileMatch": ".corbat.json",
      "url": "./schemas/corbat-config.json"
    }]
  }
}
```

**Impacto:** Extensibilidad +0.2

---

## Resumen de Tareas por Archivo

| Archivo | Accion | Fase |
|---------|--------|------|
| `src/tools.ts` | Dividir en `src/tools/*` | 1.1 |
| `src/tools/definitions.ts` | Crear | 1.1 |
| `src/tools/schemas.ts` | Crear | 1.1 |
| `src/tools/handlers/*.ts` | Crear (5 archivos) | 1.1 |
| `src/profiles.ts` | Agregar herencia | 1.2 |
| `src/types.ts` | Agregar `extends` a ProfileSchema | 1.2 |
| `tests/unit/agent.test.ts` | Crear | 1.3 |
| `tests/unit/profiles.test.ts` | Crear | 1.3 |
| `vitest.config.ts` | Agregar thresholds | 1.3 |
| `README.md` | Agregar badge coverage | 1.3 |
| `src/tools/definitions.ts` | Mejorar descripciones | 2.1 |
| `src/tools/handlers/init.ts` | Crear | 2.2 |
| `profiles/examples/*.yaml` | Crear (3 archivos) | 2.3 |
| `src/logger.ts` | Crear | 3.1 |
| `src/errors.ts` | Crear | 3.2 |
| `src/metrics.ts` | Crear | 3.3 |
| `CONTRIBUTING.md` | Crear | 4.1 |
| `docs/api-reference.md` | Crear | 4.2 |
| `schemas/corbat-config.json` | Crear | 5.2 |

---

## Orden de Ejecucion Recomendado

### Sprint 1: Fundamentos (Fase 1)
1. Refactorizar `src/tools.ts` en modulos
2. Agregar tests unitarios para `agent.ts`
3. Configurar coverage thresholds
4. Agregar badge a README

### Sprint 2: Extensibilidad (Fase 1 + 2)
1. Implementar herencia de profiles
2. Agregar tool `init`
3. Crear profiles de ejemplo

### Sprint 3: Robustez (Fase 3)
1. Agregar logging estructurado
2. Crear sistema de errores tipados
3. Agregar metricas a health

### Sprint 4: Documentacion (Fase 4 + 5)
1. Crear CONTRIBUTING.md
2. Crear API reference
3. Publicar JSON Schema
4. Agregar parametro verbosity

---

## Estimacion de Impacto Final

| Categoria          | Actual | Post-Mejoras | Ponderado |
|--------------------|--------|--------------|-----------|
| Arquitectura       | 8.5    | 9.5          | 1.90      |
| Calidad de Codigo  | 8.0    | 9.0          | 1.35      |
| Diseno MCP         | 9.0    | 9.7          | 1.94      |
| Utilidad/Valor     | 9.5    | 9.8          | 1.96      |
| Extensibilidad     | 8.5    | 9.5          | 0.95      |
| Testing            | 7.5    | 9.5          | 0.95      |
| Documentacion      | 8.0    | 9.0          | 0.45      |

**TOTAL PROYECTADO: 9.5/10**

---

## Notas de Implementacion

1. **No romper compatibilidad:** Todas las mejoras deben ser backwards-compatible
2. **Tests primero:** Antes de refactorizar, asegurar que hay tests que cubren el comportamiento actual
3. **Incrementos pequenos:** Cada cambio debe ser deployable independientemente
4. **Documentar mientras se implementa:** No dejar la documentacion para el final

---

## ESTADO DE EJECUCION

### Fase 1.1: Refactorizar Handlers - COMPLETADO

**Fecha:** 2026-01-28
**Estado:** COMPLETADO

**Archivos creados:**
- `src/tools/schemas.ts` - Zod schemas centralizados
- `src/tools/definitions.ts` - Definiciones de tools con descripciones mejoradas
- `src/tools/handlers/get-context.ts` - Handler get_context
- `src/tools/handlers/validate.ts` - Handler validate
- `src/tools/handlers/search.ts` - Handler search
- `src/tools/handlers/profiles.ts` - Handler profiles
- `src/tools/handlers/health.ts` - Handler health
- `src/tools/handlers/index.ts` - Re-exportaciones
- `src/tools/index.ts` - Indice principal del modulo

**Archivos modificados:**
- `src/index.ts` - Actualizado import a `./tools/index.js`

**Verificacion:**
- Build: OK
- Tests: 100 passed (9 suites)

**Nota:** El archivo original `src/tools.ts` se mantiene temporalmente para compatibilidad. Se eliminara al final del proceso.

---

### Fase 1.3: Tests y Coverage - COMPLETADO

**Fecha:** 2026-01-28
**Estado:** COMPLETADO

**Archivos creados:**
- `tests/unit/agent.test.ts` - 52 tests unitarios para clasificacion de tareas
- `tests/unit/schemas.test.ts` - 14 tests para validacion de schemas
- `tests/unit/profile-inheritance.test.ts` - 8 tests para deep merge

**Archivos modificados:**
- `vitest.config.ts` - Thresholds actualizados (80% statements, 75% branches)
- `README.md` - Badge de coverage agregado

**Verificacion:**
- Tests: 174 passed
- Coverage: 82% lines

---

### Fase 1.2: Herencia de Profiles - COMPLETADO

**Fecha:** 2026-01-28
**Estado:** COMPLETADO

**Archivos modificados:**
- `src/types.ts` - Agregado campo `extends` a ProfileSchema
- `src/profiles.ts` - Funciones `deepMerge` y `resolveProfileInheritance`

**Funcionalidad:**
- Profiles pueden usar `extends: "parent-profile-id"`
- Merge recursivo con hijo sobrescribiendo padre
- Deteccion de herencia circular
- Arrays reemplazados (no mergeados)

---

### Fase 2.1: Descripciones de Tools - COMPLETADO (incluido en 1.1)

---

### Fase 2.2: Tool init - COMPLETADO

**Fecha:** 2026-01-28
**Estado:** COMPLETADO

**Archivos creados:**
- `src/tools/handlers/init.ts` - Handler para generar .corbat.json

**Archivos modificados:**
- `src/tools/definitions.ts` - Definicion de tool init
- `src/tools/handlers/index.ts` - Export de handleInit
- `src/tools/index.ts` - Case para init en dispatcher

---

### Fase 2.3: Profiles de Ejemplo - COMPLETADO

**Fecha:** 2026-01-28
**Estado:** COMPLETADO

**Archivos creados:**
- `profiles/examples/strict-enterprise.yaml` - Profile enterprise estricto
- `profiles/examples/startup-fast.yaml` - Profile para startups
- `profiles/examples/microservice-kafka.yaml` - Profile event-driven con Kafka

---

### Fase 3.1: Logging - COMPLETADO

**Fecha:** 2026-01-28
**Estado:** COMPLETADO

**Archivos creados:**
- `src/logger.ts` - Logger estructurado con niveles y JSON output

---

### Fase 3.2: Errores Tipados - COMPLETADO

**Fecha:** 2026-01-28
**Estado:** COMPLETADO

**Archivos creados:**
- `src/errors.ts` - Clases de error tipadas (CorbatError, ProfileNotFoundError, etc.)

---

### Fase 3.3: Metricas - COMPLETADO

**Fecha:** 2026-01-28
**Estado:** COMPLETADO

**Archivos creados:**
- `src/metrics.ts` - Sistema de metricas in-memory

**Archivos modificados:**
- `src/tools/handlers/health.ts` - Incluye metricas en respuesta
- `src/tools/index.ts` - Registra metricas en cada tool call

---

### Fase 4.1: CONTRIBUTING.md - COMPLETADO

**Fecha:** 2026-01-28
**Estado:** COMPLETADO

**Archivos creados:**
- `CONTRIBUTING.md` - Guia completa de contribucion

---

### Fase 4.2: API Reference - COMPLETADO

**Fecha:** 2026-01-28
**Estado:** COMPLETADO

**Archivos creados:**
- `docs/api-reference.md` - Referencia completa de API

---

### Fase 5.1: Verbosity - OMITIDO

**Razon:** Tras analisis, la respuesta actual ya es concisa. Agregar verbosity anadiria complejidad sin beneficio claro. Se puede agregar en el futuro si hay demanda.

---

### Fase 5.2: JSON Schema - COMPLETADO

**Fecha:** 2026-01-28
**Estado:** COMPLETADO

**Archivos creados:**
- `schemas/corbat-config.json` - JSON Schema para .corbat.json

---

## RESUMEN FINAL DE EJECUCION

**Total de archivos creados:** 18
**Total de archivos modificados:** 8
**Tests totales:** 174 (todos pasando)
**Coverage:** 82% lines

**Mejoras implementadas:**
1. Arquitectura modular de handlers
2. Sistema de herencia de profiles
3. Tests unitarios exhaustivos
4. Logger estructurado
5. Errores tipados
6. Metricas de uso
7. Tool init para setup
8. 3 profiles de ejemplo
9. Documentacion completa (CONTRIBUTING, API Reference)
10. JSON Schema para validacion
