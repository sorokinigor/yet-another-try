package com.github.sorokinigor.yat.backoff;

import java.util.concurrent.TimeUnit;

/**
 * The main entry point for built-in {@link Backoff} instantiation.
 *
 * @author Igor Sorokin
 */
public final class Backoffs {

  private static final Backoff DEFAULT = exponential(3L, 30L, TimeUnit.SECONDS, 0.2D);
  private static final Backoff NO_BACKOFF = new FixedDelayBackoff(0L);

  private Backoffs() { throw new IllegalStateException("Not expected to be initialized"); }

  /**
   * @see ExponentialBackoff
   */
  public static Backoff defaultBackoff() {
    return DEFAULT;
  }

  /**
   * @see FixedDelayBackoff
   */
  public static Backoff fixedDelay(long amount, TimeUnit unit) {
    return new FixedDelayBackoff(unit.toNanos(amount));
  }

  /**
   * @see FixedRateBackoff
   */
  public static Backoff fixedRate(long period, TimeUnit unit) {
    return new FixedRateBackoff(unit.toNanos(period));
  }

  /**
   *
   * @param min minimum (initial) delay
   * @param max the exponential back-off is capped to this duration
   * @param unit time unit of {@code min} and {@code max}
   * @param randomFactor after calculation of the exponential back-off an additional
   *   random delay based on this factor is added, e.g. `0.2` adds up to `20%` delay.
   *   In order to skip this additional delay pass in `0`.
   *
   * @see ExponentialBackoff
   */
  public static Backoff exponential(long min, long max, TimeUnit unit, double randomFactor) {
    return new ExponentialBackoff(unit.toNanos(min), unit.toNanos(max), randomFactor);
  }

  /**
   * @return singleton instance of delay back off with `0` delay.
   * @see FixedDelayBackoff
   */
  public static Backoff noBackoff() {
    return NO_BACKOFF;
  }


  /*
   * INTERNAL API
   */
  static void validateBackoffInput(long attempt, long executionDurationNanos) {
    if (attempt < 0L || executionDurationNanos < 0L) {
      throw new IllegalArgumentException("'attempt' is '" + attempt +
          "', 'executionDurationNanos' is '" + executionDurationNanos + "'.");
    }
  }

}
