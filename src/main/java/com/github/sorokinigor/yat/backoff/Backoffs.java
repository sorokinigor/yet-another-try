package com.github.sorokinigor.yat.backoff;

import java.util.concurrent.TimeUnit;

/**
 * @author Igor Sorokin
 */
public final class Backoffs {

  private static final Backoff DEFAULT = exponential(3L, 30L, TimeUnit.SECONDS, 0.2D);
  private static final Backoff NO_BACKOFF = new FixedDelayBackoff(0L);

  private Backoffs() { throw new IllegalStateException("Not expected to be initialized"); }

  public static Backoff defaultBackoff() {
    return DEFAULT;
  }

  public static Backoff fixedDelay(long amount, TimeUnit unit) {
    return new FixedDelayBackoff(unit.toNanos(amount));
  }

  public static Backoff fixedRate(long amount, TimeUnit unit) {
    return new FixedRateBackoff(unit.toNanos(amount));
  }

  public static Backoff exponential(long min, long max, TimeUnit unit, double randomFactor) {
    return new ExponentialBackoff(unit.toNanos(min), unit.toNanos(max), randomFactor);
  }

  public static Backoff noBackoff() {
    return NO_BACKOFF;
  }

}
