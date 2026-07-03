# Corbat MCP Benchmarks

This directory contains benchmarks comparing code quality generated **with** and **without** Corbat MCP active.

## Versions

| Version | Scenarios | Focus | Status |
|---------|-----------|-------|--------|
| [v1.0](./scenarios/) | 6 | Basic/Medium tasks | Complete |
| [v2.0](./v2/) | 10 | Basic to Advanced patterns | Historical |
| [v3.0](./v3/) | 15 | Multi-language quality and value analysis | **Current** |

> **Recommended:** Use [v3.0](./v3/) for the latest dataset. Read both the primary benchmark report and the value analysis; they answer different questions and should not be merged into a single headline claim.

## Purpose

These benchmarks evaluate how code changes when LLMs have access to Corbat's coding standards and guardrails. Results are mixed: v3 shows weaker aggregate quality scores but stronger compactness and maintainability signals under the alternative value analysis.

## Methodology

### Test Design

1. **Identical Prompts**: Same prompts used for both with and without MCP tests
2. **Fresh Conversations**: Each test starts with no prior context
3. **No Modifications**: Generated code saved exactly as produced
4. **Multiple Stacks**: 6 different technology stacks tested

### Scenarios

| # | Scenario | Stack | Complexity |
|---|----------|-------|------------|
| 1 | CRUD Service | Java Spring | Medium |
| 2 | Kafka Consumer | Java Spring | High |
| 3 | REST API | Node.js/TypeScript | Medium |
| 4 | Form Component | React/TypeScript | Medium |
| 5 | Async API | Python FastAPI | Medium |
| 6 | HTTP Handler | Go | Medium |

### Evaluation Criteria

Each scenario is scored on 6 dimensions (1-10 scale):

| Criteria | Weight | Description |
|----------|--------|-------------|
| Architecture | 20% | Pattern adherence, layer separation |
| Tests | 20% | Test presence, quality, coverage |
| Error Handling | 15% | Robustness, validation, edge cases |
| Naming | 15% | Convention compliance, clarity |
| Code Quality | 20% | Method size, complexity, SOLID |
| Documentation | 10% | Comments, self-documenting code |

## How to Run

### Phase 1: Without MCP

1. Disable Corbat MCP in your LLM configuration
2. Follow instructions in [TEST_WITHOUT_MCP.md](./TEST_WITHOUT_MCP.md)
3. Save results to `results/without-mcp/`

### Phase 2: With MCP

1. Enable Corbat MCP in your LLM configuration
2. Follow instructions in [TEST_WITH_MCP.md](./TEST_WITH_MCP.md)
3. Save results to `results/with-mcp/`

### Phase 3: Analysis

1. Ensure all results are saved
2. Follow instructions in [COMPARE_RESULTS.md](./COMPARE_RESULTS.md)
3. Review generated report in `reports/comparison-report.md`

## Directory Structure

```
benchmarks/
├── README.md                 # This file
├── TEST_WITHOUT_MCP.md       # Instructions for baseline tests
├── TEST_WITH_MCP.md          # Instructions for MCP-enabled tests
├── COMPARE_RESULTS.md        # Instructions for analysis
├── scenarios/                # Detailed scenario definitions
│   ├── 01-java-spring-crud.md
│   ├── 02-java-spring-kafka.md
│   ├── 03-nodejs-rest-api.md
│   ├── 04-react-form-component.md
│   ├── 05-python-fastapi-endpoint.md
│   └── 06-go-http-handler.md
├── results/                  # Generated code (gitignored)
│   ├── without-mcp/
│   └── with-mcp/
└── reports/                  # Analysis reports
    └── comparison-report.md
```

## Results

> **Note**: Run the benchmarks to generate results. The comparison report will be saved to `reports/comparison-report.md`.

<!-- BENCHMARK_RESULTS_START -->
*Results pending - run benchmarks to populate*
<!-- BENCHMARK_RESULTS_END -->

## Interpretation Guide

### Score Interpretation

| Score | Quality Level |
|-------|---------------|
| 9-10 | Production-ready, follows all best practices |
| 7-8 | Good quality, minor improvements possible |
| 5-6 | Acceptable, needs some refactoring |
| 3-4 | Below standard, significant issues |
| 1-2 | Poor quality, major refactoring needed |

### Expected Improvement

Based on Corbat's guidelines, expected improvements with MCP:

- **Architecture**: +2-3 points (enforces hexagonal/clean architecture)
- **Tests**: +2-3 points (TDD workflow, AAA pattern)
- **Error Handling**: +1-2 points (proper exceptions, validation)
- **Naming**: +1 point (enforces conventions)
- **Code Quality**: +1-2 points (enforces thresholds)
- **Documentation**: +1 point (encourages clear code)

## v2.0 Improvements

The v2.0 methodology addresses issues identified in v1.0:

| Improvement | v1.0 | v2.0 |
|-------------|------|------|
| **Prompts** | Detailed with architectural hints | Minimalist, no hints |
| **Scenarios** | 6 basic/medium complexity | 10 including advanced patterns |
| **Patterns** | Standard CRUD | Saga, Circuit Breaker, Event Sourcing |
| **Isolation** | Partial MCP removal | Complete isolation |
| **Metrics** | Manual scoring only | Automated + qualitative |

### Historical Expected Results

| Category | Expected Improvement |
|----------|---------------------|
| Basic (3 scenarios) | +10-15% |
| Intermediate (3 scenarios) | +25-35% |
| Advanced (4 scenarios) | +50-80% |
| **Overall** | **+35-45%** |

## Contributing

To add new benchmark scenarios:

1. Create scenario definition in `scenarios/` (v1) or `v2/prompts/` (v2)
2. Add prompts to the appropriate test files
3. Update evaluation criteria if needed
4. Run benchmarks and update report
