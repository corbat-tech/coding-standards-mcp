# Corbat MCP Benchmarks v2.0

## Metodología

Esta versión de benchmarks utiliza una metodología mejorada para medir el impacto real del Corbat MCP en la calidad del código generado por LLMs.

### Principios de Diseño

1. **Prompts minimalistas** - Solo funcionalidad, sin pistas de arquitectura
2. **Patrones avanzados** - Saga, Event-driven, Circuit Breaker, State Machine
3. **Métricas objetivas** - LOC, tests count, patrones detectados automáticamente
4. **Aislamiento total** - Sesión limpia sin MCP debe ser REALMENTE sin guías

### Categorías de Scenarios

| Categoría | Scenarios | Descripción |
|-----------|-----------|-------------|
| **Basic** | 01-03 | CRUD, REST API, React Form (LLM ya es capaz) |
| **Intermediate** | 04-06 | Kafka, FastAPI async, Go patterns (MCP mejora) |
| **Advanced** | 07-10 | Saga, Circuit Breaker, Event Sourcing, State Machine (MCP brilla) |

### Estructura de Archivos

```
v2/
├── README.md                 # Este archivo
├── prompts/                  # 10 prompts minimalistas
│   ├── 01-user-service.md
│   ├── 02-rest-api.md
│   └── ...
├── WITH_MCP.md               # Instrucciones ejecución con MCP
├── WITHOUT_MCP.md            # Instrucciones ejecución sin MCP
├── results/
│   ├── with-mcp/             # Resultados con MCP activo
│   └── without-mcp/          # Resultados sin MCP
├── ANALYSIS.md               # Comparación detallada
└── metrics.json              # Datos crudos
```

## Métricas

### Cuantitativas (Automáticas)

| Métrica | Cálculo |
|---------|---------|
| LOC | `wc -l` de archivos generados |
| Tests | Contar funciones `test_*` / `it()` / `@Test` |
| Files | Número de archivos generados |
| Patterns | Regex para detectar: interface, Repository, Error extends, @Inject |

### Cualitativas (Manual)

| Categoría | Peso | Criterio |
|-----------|------|----------|
| Architecture | 25% | Hexagonal/Clean separación |
| Testing | 20% | AAA pattern, edge cases |
| Error Handling | 20% | Custom types, validation |
| DI | 20% | Interface-based, testable |
| SOLID | 15% | Single responsibility, etc |

### Scoring

```
Total = (Architecture × 0.25) + (Testing × 0.20) +
        (Error × 0.20) + (DI × 0.20) + (SOLID × 0.15)
```

## Ejecución

1. **Con MCP**: Ver [WITH_MCP.md](./WITH_MCP.md)
2. **Sin MCP**: Ver [WITHOUT_MCP.md](./WITHOUT_MCP.md)
3. **Análisis**: Ver [ANALYSIS.md](./ANALYSIS.md)

## Resultados Esperados

| Categoría | Mejora Esperada |
|-----------|-----------------|
| Basic | +10-15% |
| Intermediate | +25-35% |
| Advanced | +50-80% |
| **Overall** | **+35-45%** |
