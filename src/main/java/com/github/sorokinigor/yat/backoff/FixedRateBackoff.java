package com.github.sorokinigor.yat.backoff;

/**
 * INTERNAL API
 *
 * @author Igor Sorokin
 */
final class FixedRateBackoff implements Backoff {

  private final long durationNanos;

  FixedRateBackoff(long durationNanos) {
    if (durationNanos < 0L) {
      throw new IllegalArgumentException("'durationNanos' must be >= 0.");
    }
    this.durationNanos = durationNanos;
  }

  @Override
  public long calculateDelayNanos(int attempt, long executionDurationNanos) {
    Backoffs.validateBackoffInput(attempt, executionDurationNanos);
    if (executionDurationNanos >= durationNanos) {
      return 0L;
    } else {
      return durationNanos - executionDurationNanos;
    }
  }

  @Override
  public String toString() {
    return "FixedRateBackoff{durationNanos=" + durationNanos + '}';
  }
}
