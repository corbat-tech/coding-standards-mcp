import { describe, it, expect, beforeEach, vi } from 'vitest';
import { CircuitBreaker } from './CircuitBreaker';
import { CircuitOpenError } from './types';
import { Clock } from './Clock';

class StubClock implements Clock {
  private time = 0;

  now(): number {
    return this.time;
  }

  advance(ms: number): void {
    this.time += ms;
  }

  setTime(time: number): void {
    this.time = time;
  }
}

describe('CircuitBreaker', () => {
  let clock: StubClock;
  let breaker: CircuitBreaker;

  beforeEach(() => {
    clock = new StubClock();
    breaker = new CircuitBreaker(
      { failureThreshold: 3, successThreshold: 2, timeout: 5000 },
      clock
    );
  });

  describe('closed state', () => {
    it('should_execute_function_when_closed', async () => {
      // Arrange
      const fn = vi.fn().mockResolvedValue('success');

      // Act
      const result = await breaker.execute(fn);

      // Assert
      expect(result).toBe('success');
      expect(breaker.getState()).toBe('closed');
    });

    it('should_remain_closed_when_failures_below_threshold', async () => {
      // Arrange
      const fn = vi.fn().mockRejectedValue(new Error('fail'));

      // Act
      await expect(breaker.execute(fn)).rejects.toThrow();
      await expect(breaker.execute(fn)).rejects.toThrow();

      // Assert
      expect(breaker.getState()).toBe('closed');
    });

    it('should_open_when_failures_reach_threshold', async () => {
      // Arrange
      const fn = vi.fn().mockRejectedValue(new Error('fail'));

      // Act
      for (let i = 0; i < 3; i++) {
        await expect(breaker.execute(fn)).rejects.toThrow();
      }

      // Assert
      expect(breaker.getState()).toBe('open');
    });

    it('should_reset_failure_count_on_success', async () => {
      // Arrange
      const failFn = vi.fn().mockRejectedValue(new Error('fail'));
      const successFn = vi.fn().mockResolvedValue('success');

      // Act
      await expect(breaker.execute(failFn)).rejects.toThrow();
      await expect(breaker.execute(failFn)).rejects.toThrow();
      await breaker.execute(successFn);
      await expect(breaker.execute(failFn)).rejects.toThrow();
      await expect(breaker.execute(failFn)).rejects.toThrow();

      // Assert
      expect(breaker.getState()).toBe('closed');
    });
  });

  describe('open state', () => {
    beforeEach(async () => {
      const fn = vi.fn().mockRejectedValue(new Error('fail'));
      for (let i = 0; i < 3; i++) {
        await expect(breaker.execute(fn)).rejects.toThrow(Error);
      }
    });

    it('should_throw_CircuitOpenError_when_open', async () => {
      // Arrange
      const fn = vi.fn().mockResolvedValue('success');

      // Act & Assert
      await expect(breaker.execute(fn)).rejects.toThrow(CircuitOpenError);
      expect(fn).not.toHaveBeenCalled();
    });

    it('should_include_retry_after_in_error', async () => {
      // Arrange
      const fn = vi.fn().mockResolvedValue('success');
      clock.advance(2000);

      // Act & Assert
      try {
        await breaker.execute(fn);
      } catch (e) {
        expect(e).toBeInstanceOf(CircuitOpenError);
        expect((e as CircuitOpenError).retryAfter).toBe(3000);
      }
    });

    it('should_transition_to_half_open_after_timeout', async () => {
      // Arrange
      clock.advance(5000);

      // Act
      const state = breaker.getState();

      // Assert
      expect(state).toBe('half-open');
    });
  });

  describe('half-open state', () => {
    beforeEach(async () => {
      const fn = vi.fn().mockRejectedValue(new Error('fail'));
      for (let i = 0; i < 3; i++) {
        await expect(breaker.execute(fn)).rejects.toThrow(Error);
      }
      clock.advance(5000);
    });

    it('should_allow_request_when_half_open', async () => {
      // Arrange
      const fn = vi.fn().mockResolvedValue('success');

      // Act
      const result = await breaker.execute(fn);

      // Assert
      expect(result).toBe('success');
    });

    it('should_close_after_success_threshold_reached', async () => {
      // Arrange
      const fn = vi.fn().mockResolvedValue('success');

      // Act
      await breaker.execute(fn);
      await breaker.execute(fn);

      // Assert
      expect(breaker.getState()).toBe('closed');
    });

    it('should_open_on_failure_in_half_open', async () => {
      // Arrange
      const fn = vi.fn().mockRejectedValue(new Error('fail'));

      // Act
      await expect(breaker.execute(fn)).rejects.toThrow();

      // Assert
      expect(breaker.getState()).toBe('open');
    });
  });

  describe('metrics', () => {
    it('should_track_total_requests', async () => {
      // Arrange
      const fn = vi.fn().mockResolvedValue('success');

      // Act
      await breaker.execute(fn);
      await breaker.execute(fn);
      await breaker.execute(fn);

      // Assert
      expect(breaker.getMetrics().totalRequests).toBe(3);
    });

    it('should_track_failures_and_successes', async () => {
      // Arrange
      const successFn = vi.fn().mockResolvedValue('success');
      const failFn = vi.fn().mockRejectedValue(new Error('fail'));

      // Act
      await breaker.execute(successFn);
      await expect(breaker.execute(failFn)).rejects.toThrow();
      await breaker.execute(successFn);

      // Assert
      const metrics = breaker.getMetrics();
      expect(metrics.totalSuccesses).toBe(2);
      expect(metrics.totalFailures).toBe(1);
    });
  });

  describe('reset', () => {
    it('should_close_circuit_on_reset', async () => {
      // Arrange
      const fn = vi.fn().mockRejectedValue(new Error('fail'));
      for (let i = 0; i < 3; i++) {
        await expect(breaker.execute(fn)).rejects.toThrow(Error);
      }

      // Act
      breaker.reset();

      // Assert
      expect(breaker.getState()).toBe('closed');
    });

    it('should_clear_failure_count_on_reset', async () => {
      // Arrange
      const fn = vi.fn().mockRejectedValue(new Error('fail'));
      for (let i = 0; i < 3; i++) {
        await expect(breaker.execute(fn)).rejects.toThrow(Error);
      }
      breaker.reset();

      // Act
      await expect(breaker.execute(fn)).rejects.toThrow(Error);
      await expect(breaker.execute(fn)).rejects.toThrow(Error);

      // Assert
      expect(breaker.getState()).toBe('closed');
    });
  });
});
