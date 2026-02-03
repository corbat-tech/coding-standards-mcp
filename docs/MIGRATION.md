# Migration Guide

This document describes breaking changes and migration steps between major versions of Corbat MCP.

## Table of Contents

- [v1.x to v2.x](#v1x-to-v2x)
- [Local Testing Commands](#local-testing-commands)
- [Troubleshooting](#troubleshooting)

---

## v1.x to v2.x

### Breaking Changes

#### 1. Coverage Requirements Enforced

**What changed:** CI/CD pipeline now enforces minimum coverage thresholds. Builds will fail if coverage drops below:
- Lines: 70%
- Branches: 60%

**Migration steps:**
1. Run `npm run test:coverage` locally before pushing
2. Add tests for untested code paths
3. Check coverage report in `coverage/lcov-report/index.html`

#### 2. Security Scanning is Blocking

**What changed:** `npm audit` and Snyk scans now block the build on high/critical vulnerabilities (previously non-blocking).

**Migration steps:**
1. Run `npm audit --audit-level=high` locally
2. Fix or document any vulnerabilities before merging
3. For false positives, add to `.snyk` file with justification

#### 3. Input Size Limits

**What changed:** `analyzeCode()` now throws an error if code exceeds 1MB (1,000,000 characters).

**Migration steps:**
1. If analyzing large files, split them into smaller chunks
2. Handle the error case in your code:
   ```typescript
   try {
     const result = analyzeCode(code);
   } catch (error) {
     if (error.message.includes('exceeds maximum size')) {
       // Handle oversized input
     }
   }
   ```

#### 4. Path Validation in Resources

**What changed:** Profile IDs and category names are now validated. Only alphanumeric characters, underscores, and hyphens are allowed.

**Migration steps:**
1. Ensure profile IDs match pattern: `^[a-zA-Z0-9_-]+$`
2. Update any custom profiles with invalid characters

#### 5. Logger Correlation IDs

**What changed:** Logger now supports request-scoped correlation IDs via AsyncLocalStorage.

**New exports:**
- `withCorrelationId(fn)` - Execute sync function with correlation ID
- `withCorrelationIdAsync(fn)` - Execute async function with correlation ID
- `getCorrelationId()` - Get current correlation ID
- `runWithCorrelationId(id, fn)` - Use specific correlation ID

**Usage:**
```typescript
import { withCorrelationIdAsync, logger } from './logger.js';

await withCorrelationIdAsync(async () => {
  logger.info('Processing request'); // Logs include correlationId
  await processRequest();
});
```

#### 6. Environment Variable Validation

**What changed:** Environment variables are now validated more strictly:
- `CORBAT_ENV` / `NODE_ENV`: Case-insensitive comparison
- `CORBAT_CACHE_TTL_MS`: Invalid integers fall back to default
- `CORBAT_LOG_LEVEL`: Invalid values fall back to environment default

**Migration steps:**
- No action required if using valid values
- Invalid values now use fallbacks instead of causing errors

---

## Local Testing Commands

Before pushing changes, run these commands to validate your code:

### Full Validation Suite

```bash
# Run all checks (recommended before PR)
npm run build && npm test && npm run check && npm run validate:arch
```

### Individual Commands

```bash
# Unit tests
npm test

# Tests with coverage report
npm run test:coverage

# Type checking
npx tsc --noEmit

# Linting and formatting
npm run check

# Architecture validation
npm run validate:arch

# Security audit
npm audit --audit-level=high
```

### Coverage Report

After running `npm run test:coverage`, open the HTML report:

```bash
open coverage/lcov-report/index.html
```

### Watch Mode (Development)

```bash
npm run test:watch
```

---

## Troubleshooting

### Coverage Threshold Failures

**Error:** `Lines coverage X% is below threshold of 70%`

**Solution:**
1. Identify uncovered code: `npm run test:coverage`
2. Open `coverage/lcov-report/index.html`
3. Add tests for red-highlighted lines
4. Focus on critical paths first

### Security Scan Failures

**Error:** `npm audit found high severity vulnerabilities`

**Solution:**
1. Run `npm audit` to see details
2. Update vulnerable packages: `npm update <package>`
3. If no fix available, document in `.snyk`:
   ```yaml
   ignore:
     SNYK-JS-XXX:
       - '*':
           reason: 'No fix available, low risk in this context'
           expires: 2025-03-01
   ```

### Invalid Profile ID

**Error:** Resource returns null for custom profile

**Solution:**
1. Rename profile to use only: `a-z`, `A-Z`, `0-9`, `_`, `-`
2. Example: `my-custom_profile-v2` (valid)
3. Invalid: `my/profile` or `profile@v2`

### Correlation ID Not Appearing in Logs

**Issue:** Logs don't show `correlationId` field

**Solution:**
Wrap your code in the correlation context:
```typescript
import { withCorrelationIdAsync, logger } from './logger.js';

// Without context - no correlationId
logger.info('No correlation');

// With context - includes correlationId
await withCorrelationIdAsync(async () => {
  logger.info('Has correlation'); // Includes correlationId
});
```

---

## Version Compatibility

| Corbat MCP | Node.js | MCP SDK |
|------------|---------|---------|
| 2.x        | 18, 20, 22 | ^1.0.0 |
| 1.x        | 18, 20     | ^0.9.0 |

---

## Getting Help

- GitHub Issues: [corbat-mcp/issues](https://github.com/corbat/corbat-mcp/issues)
- Documentation: See `/docs` folder
- Examples: See `/examples` folder
