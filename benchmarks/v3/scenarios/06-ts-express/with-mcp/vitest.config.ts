import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    environment: 'node',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      include: ['services/**/*.ts', 'middleware/**/*.ts', 'routes/**/*.ts'],
      exclude: ['**/*.test.ts', '**/index.ts'],
    },
  },
});
