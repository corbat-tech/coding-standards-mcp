import { describe, expect, it } from 'vitest';

/**
 * Tests for profile inheritance deep merge logic.
 * These tests verify the merge behavior independent of file loading.
 */

// Replicate the deepMerge function for testing
function deepMerge<T extends Record<string, unknown>>(parent: T, child: Partial<T>): T {
  const result = { ...parent };

  for (const key of Object.keys(child) as Array<keyof T>) {
    const childValue = child[key];
    const parentValue = parent[key];

    if (childValue === undefined) {
      continue;
    }

    if (
      childValue !== null &&
      typeof childValue === 'object' &&
      !Array.isArray(childValue) &&
      parentValue !== null &&
      typeof parentValue === 'object' &&
      !Array.isArray(parentValue)
    ) {
      result[key] = deepMerge(
        parentValue as Record<string, unknown>,
        childValue as Record<string, unknown>
      ) as T[keyof T];
    } else {
      result[key] = childValue as T[keyof T];
    }
  }

  return result;
}

describe('Profile Inheritance - Deep Merge', () => {
  it('should merge child properties over parent', () => {
    const parent = {
      name: 'Parent',
      codeQuality: {
        maxMethodLines: 20,
        maxClassLines: 200,
        minimumTestCoverage: 80,
      },
    };

    const child = {
      name: 'Child',
      codeQuality: {
        maxMethodLines: 15,
        minimumTestCoverage: 90,
      },
    };

    const merged = deepMerge(parent, child);

    expect(merged.name).toBe('Child');
    expect(merged.codeQuality.maxMethodLines).toBe(15);
    expect(merged.codeQuality.minimumTestCoverage).toBe(90);
    // Inherited from parent
    expect(merged.codeQuality.maxClassLines).toBe(200);
  });

  it('should replace arrays instead of merging them', () => {
    const parent = {
      name: 'Parent',
      technologies: ['java', 'spring', 'maven'],
    };

    const child = {
      name: 'Child',
      technologies: ['kotlin', 'spring'],
    };

    const merged = deepMerge(parent, child);

    expect(merged.technologies).toEqual(['kotlin', 'spring']);
    expect(merged.technologies).not.toContain('java');
  });

  it('should handle nested objects deeply', () => {
    const parent = {
      architecture: {
        type: 'hexagonal',
        layers: {
          domain: { name: 'domain', deps: [] as string[] },
          application: { name: 'app', deps: ['domain'] },
        },
      },
    };

    const child = {
      architecture: {
        layers: {
          domain: { name: 'core', deps: [] as string[] },
        },
      },
    };

    const merged = deepMerge(parent, child);

    expect(merged.architecture.type).toBe('hexagonal');
    expect(merged.architecture.layers.domain.name).toBe('core');
    expect(merged.architecture.layers.application.name).toBe('app');
  });

  it('should not modify original objects', () => {
    const parent = {
      codeQuality: { maxMethodLines: 20 },
    };

    const child = {
      codeQuality: { maxMethodLines: 15 },
    };

    const merged = deepMerge(parent, child);

    expect(merged.codeQuality.maxMethodLines).toBe(15);
    expect(parent.codeQuality.maxMethodLines).toBe(20);
  });

  it('should handle undefined child values by keeping parent', () => {
    const parent = {
      name: 'Parent',
      description: 'Parent description',
    };

    const child = {
      name: 'Child',
      description: undefined,
    };

    const merged = deepMerge(parent, child as typeof parent);

    expect(merged.name).toBe('Child');
    expect(merged.description).toBe('Parent description');
  });

  it('should handle null values by overwriting', () => {
    const parent = {
      name: 'Parent',
      optional: { value: 'something' },
    };

    const child = {
      name: 'Child',
      optional: null,
    };

    const merged = deepMerge(parent, child as unknown as typeof parent);

    expect(merged.optional).toBeNull();
  });

  it('should handle empty child object', () => {
    const parent = {
      name: 'Parent',
      codeQuality: { maxMethodLines: 20 },
    };

    const child = {};

    const merged = deepMerge(parent, child);

    expect(merged.name).toBe('Parent');
    expect(merged.codeQuality.maxMethodLines).toBe(20);
  });

  it('should support three-level inheritance chain', () => {
    const grandparent = {
      codeQuality: {
        maxMethodLines: 30,
        maxClassLines: 300,
        maxFileLines: 500,
      },
    };

    const parent = deepMerge(grandparent, {
      codeQuality: {
        maxMethodLines: 25,
        maxClassLines: 250,
      },
    });

    const child = deepMerge(parent, {
      codeQuality: {
        maxMethodLines: 20,
      },
    });

    expect(child.codeQuality.maxMethodLines).toBe(20);
    expect(child.codeQuality.maxClassLines).toBe(250);
    expect(child.codeQuality.maxFileLines).toBe(500);
  });
});
