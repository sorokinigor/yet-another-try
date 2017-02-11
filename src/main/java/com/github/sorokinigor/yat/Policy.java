package com.github.sorokinigor.yat;

import com.github.sorokinigor.yat.backoff.Backoff;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author Igor Sorokin
 */
final class Policy {

  final Predicate<Exception> exceptionFilter;
  final Backoff backOff;
  final long firstDelayNanos;
  final int maxAttempts;
  final boolean firstAttemptInInvocationThread;

  Policy(
      Predicate<Exception> exceptionFilter,
      Backoff backOff,
      long firstDelayNanos,
      int maxAttempts,
      boolean firstAttemptInInvocationThread
  ) {
    this.exceptionFilter = Objects.requireNonNull(exceptionFilter, "'exceptionFilter' should not be 'null'.");
    this.backOff = Objects.requireNonNull(backOff, "'backOff' should not be 'null'.");
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("'firstDelayNanos' should be > '0'.");
    }
    this.firstDelayNanos = firstDelayNanos;
    if (firstDelayNanos < 0) {
      throw new IllegalArgumentException("'firstDelayNanos' should be >= '0'.");
    }
    this.maxAttempts = maxAttempts;
    this.firstAttemptInInvocationThread = firstAttemptInInvocationThread;
  }

  boolean shouldRetry(Exception exception) {
    return exceptionFilter.test(exception);
  }

  @Override
  public String toString() {
    return "Policy{" +
        "exceptionFilter=" + exceptionFilter +
        ", backOff=" + backOff +
        ", firstDelayNanos=" + firstDelayNanos +
        ", maxAttempts=" + maxAttempts +
        ", firstAttemptInInvocationThread=" + firstAttemptInInvocationThread +
        '}';
  }

}
