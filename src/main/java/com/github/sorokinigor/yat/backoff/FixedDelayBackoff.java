package com.github.sorokinigor.yat.backoff;

/**
 * @author Igor Sorokin
 */
final class FixedDelayBackoff implements Backoff {

  private final long durationNanos;

  FixedDelayBackoff(long durationNanos) {
    if (durationNanos < 0) {
      throw new IllegalArgumentException("'durationNanos' must be >= 0.");
    }
    this.durationNanos = durationNanos;
  }

  @Override
  public long calculateDelayNanos(int attempt, long executionDurationNanos) {
    return durationNanos;
  }

  @Override
  public String toString() {
    return "FixedDelayBackoff{durationNanos=" + durationNanos + '}';
  }
}
