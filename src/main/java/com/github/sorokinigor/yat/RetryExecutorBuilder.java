package com.github.sorokinigor.yat;

import com.github.sorokinigor.yat.backoff.Backoff;
import com.github.sorokinigor.yat.backoff.Backoffs;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * @author Igor Sorokin
 */
public final class RetryExecutorBuilder {

  static final long NO_TIMEOUT = -1L;

  private ScheduledExecutorService executorService;
  private int maxAttempts = 3;
  private long firstDelayNanos;
  private boolean firstAttemptInInvocationThread;
  private long timeoutNanos = NO_TIMEOUT;
  private ScheduledExecutorService timeoutExecutorService;
  private Predicate<Exception> retryPredicate;
  private Predicate<Exception> terminatePredicate;
  private Backoff backOff = Backoffs.defaultBackoff();

  public RetryExecutor build() {
    Predicate<Exception> retryPredicate = this.retryPredicate == null ? e -> true
        : this.retryPredicate;
    Predicate<Exception> terminatePredicate = this.terminatePredicate == null ? e -> false
        : this.terminatePredicate;
    Policy policy = new Policy(
        exception -> retryPredicate.test(exception) && !terminatePredicate.test(exception),
        backOff,
        firstDelayNanos,
        maxAttempts,
        firstAttemptInInvocationThread
    );
    RetryExecutor executor = new AsyncRetryExecutor(executorService, policy);
    if (timeoutNanos > 0L) {
      timeoutExecutorService = timeoutExecutorService == null ? executorService
          : timeoutExecutorService;
      return new TimeoutExecutor(executor, timeoutExecutorService, timeoutNanos);
    } else {
      return executor;
    }
  }

  public ScheduledExecutorService executorService() {
    return executorService;
  }

  public RetryExecutorBuilder executorService(ScheduledExecutorService executorService) {
    this.executorService = executorService;
    return this;
  }

  public int maxAttempts() {
    return maxAttempts;
  }

  public RetryExecutorBuilder maxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
    return this;
  }

  public RetryExecutorBuilder retryOnce() {
    return maxAttempts(2);
  }

  public RetryExecutorBuilder doNotRetry() {
    return maxAttempts(1);
  }

  public long firstDelayNanos() {
    return firstDelayNanos;
  }

  public long firstDelay(TimeUnit timeUnit) {
    return timeUnit.convert(firstDelayNanos, TimeUnit.NANOSECONDS);
  }

  public RetryExecutorBuilder firstDelayNanos(long firstDelayNanos) {
    this.firstDelayNanos = firstDelayNanos;
    return this;
  }

  public RetryExecutorBuilder firstDelay(long amount, TimeUnit timeUnit) {
    return firstDelayNanos(timeUnit.toNanos(amount));
  }

  public RetryExecutorBuilder noFirstDelay() {
    return firstDelayNanos(0L);
  }

  public boolean firstAttemptInInvocationThread() {
    return firstAttemptInInvocationThread;
  }

  public RetryExecutorBuilder runFirstAttemptInInvocationThread() {
    return firstAttemptInInvocationThread(true);
  }

  public RetryExecutorBuilder firstAttemptInInvocationThread(boolean firstAttemptInInvocationThread) {
    this.firstAttemptInInvocationThread = firstAttemptInInvocationThread;
    return this;
  }

  public long timeoutNanos() {
    return timeoutNanos;
  }

  public long timeout(TimeUnit timeUnit) {
    return timeUnit.convert(timeoutNanos, TimeUnit.NANOSECONDS);
  }

  public RetryExecutorBuilder timeoutNanos(long timeoutNanos) {
    this.timeoutNanos = timeoutNanos;
    return this;
  }

  public RetryExecutorBuilder timeout(long timeout, TimeUnit timeUnit) {
    return timeoutNanos(timeUnit.toNanos(timeout));
  }

  public RetryExecutorBuilder noTimeout() {
    return timeoutNanos(NO_TIMEOUT);
  }

  public ScheduledExecutorService timeoutExecutorService() {
    return timeoutExecutorService;
  }

  public RetryExecutorBuilder timeoutExecutorService(ScheduledExecutorService timeoutExecutorService) {
    this.timeoutExecutorService = timeoutExecutorService;
    return this;
  }

  public Predicate<Exception> retryPredicate() {
    return retryPredicate;
  }

  public RetryExecutorBuilder retryPredicate(Predicate<Exception> retryPredicate) {
    if (this.retryPredicate != null) {
      this.retryPredicate = this.retryPredicate.or(retryPredicate);
    } else {
      this.retryPredicate = retryPredicate;
    }
    return this;
  }

  public RetryExecutorBuilder retryOn(Class<? extends Exception> exceptionClass) {
    Objects.requireNonNull(exceptionClass, "'exceptionClass' should not be 'null'.");
    return retryPredicate(exceptionClass::isInstance);
  }

  public Predicate<Exception> terminatePredicate() {
    return terminatePredicate;
  }

  public RetryExecutorBuilder terminatePredicate(Predicate<Exception> terminatePredicate) {
    if (this.terminatePredicate != null) {
      this.terminatePredicate = this.terminatePredicate.or(terminatePredicate);
    } else {
      this.terminatePredicate = terminatePredicate;
    }
    return this;
  }

  public RetryExecutorBuilder terminateOn(Class<? extends Exception> exceptionClass) {
    Objects.requireNonNull(exceptionClass, "'exceptionClass' should not be 'null'.");
    return terminatePredicate(exceptionClass::isInstance);
  }

  public Backoff backOff() {
    return backOff;
  }

  public RetryExecutorBuilder backOff(Backoff backOff) {
    this.backOff = backOff;
    return this;
  }

  public RetryExecutorBuilder withoutDelay() {
    return backOff(Backoffs.noBackoff())
        .noFirstDelay();
  }

}
