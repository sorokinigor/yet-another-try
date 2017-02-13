package com.github.sorokinigor.yat.backoff;

/**
 * A simple strategy interface which is intended to be used in order
 * to calculate the delay between attempts.
 *
 * @author Igor Sorokin
 */
@FunctionalInterface
public interface Backoff {

  /**
   * @param attempt the non negative attempt number
   * @param executionDurationNanos the non negative duration of task execution in nanoseconds
   * @return non negative delay in nanoseconds
   * @throws IllegalArgumentException if either {@code attempt} or {@code executionDurationNanos} is less than `0`
   */
  long calculateDelayNanos(int attempt, long executionDurationNanos);

}
