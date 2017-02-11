package com.github.sorokinigor.yat.backoff;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Igor Sorokin
 */
final class ExponentialBackoff implements Backoff {

  static final int MAX_ATTEMPT = 30;
  private final long minNanos;
  private final long maxNanos;
  private final double randomFactor;

  ExponentialBackoff(long minNanos, long maxNanos, double randomFactor) {
    if (minNanos < 0) {
      throw new IllegalArgumentException("'minNanos' must be >= 0.");
    }
    if (maxNanos < minNanos) {
      throw new IllegalArgumentException("'maxNanos' must be >= 'minNanos'.");
    }
    if (randomFactor < 0.0 || randomFactor > 1.0) {
      throw new IllegalArgumentException("'randomFactor' must be between '0.0' and '1.0'.");
    }
    this.minNanos = minNanos;
    this.maxNanos = maxNanos;
    this.randomFactor = randomFactor;
  }

  @Override
  public long calculateDelayNanos(int attempt, long executionDurationNanos) {
    if (attempt > MAX_ATTEMPT) {
      return maxNanos;
    }
    double rnd = 1.0 + ThreadLocalRandom.current().nextDouble() * randomFactor;
    return (long) (Math.min(maxNanos, minNanos * Math.pow(2, attempt)) * rnd);
  }

  @Override
  public String toString() {
    return "ExponentialBackoff{" +
        "minNanos=" + minNanos +
        ", maxNanos=" + maxNanos +
        ", randomFactor=" + randomFactor +
        '}';
  }
}
