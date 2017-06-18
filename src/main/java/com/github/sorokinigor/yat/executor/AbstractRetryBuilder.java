package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.backoff.Backoff;
import com.github.sorokinigor.yat.backoff.Backoffs;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * INTERNAL API
 *
 * @author Igor Sorokin
 */
abstract class AbstractRetryBuilder<B extends AbstractRetryBuilder<B>> {

  static final Set<Class<? extends Exception>> FATAL_EXCEPTIONS = Stream.of(InterruptedException.class)
      .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
  static final Predicate<Exception> DEFAULT_TERMINATE_PREDICATE = exception ->
      FATAL_EXCEPTIONS.contains(exception.getClass());
  static final long NO_TIMEOUT = -1L;

  private int maxAttempts = 3;
  private long firstDelayNanos;
  private long timeoutNanos = NO_TIMEOUT;
  private Predicate<Exception> retryPredicate;
  private Predicate<Exception> terminatePredicate;
  private Backoff backOff = Backoffs.defaultBackoff();

  protected final Policy buildPolicy(boolean firstAttemptInInvocationThread) {
    Predicate<Exception> retryPredicate = retryPredicate();
    Predicate<Exception> terminatePredicate = terminatePredicate();
    return new Policy(
        exception -> retryPredicate.test(exception) && !terminatePredicate.test(exception),
        backOff(),
        firstDelayNanos(),
        maxAttempts(),
        firstAttemptInInvocationThread
    );
  }

  public final int maxAttempts() {
    return maxAttempts;
  }

  public final B maxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
    return _this();
  }

  public final B retryOnce() {
    return maxAttempts(2);
  }

  public final B doNotRetry() {
    return maxAttempts(1);
  }

  public final long firstDelayNanos() {
    return firstDelayNanos;
  }

  public final long firstDelay(TimeUnit timeUnit) {
    return Objects.requireNonNull(timeUnit, "'timeUnit' should not be 'null'.")
        .convert(firstDelayNanos, TimeUnit.NANOSECONDS);
  }

  public final B firstDelayNanos(long firstDelayNanos) {
    this.firstDelayNanos = firstDelayNanos;
    return _this();
  }

  public final B firstDelay(long amount, TimeUnit timeUnit) {
    return firstDelayNanos(Objects.requireNonNull(timeUnit, "'timeUnit' should not be 'null'.").toNanos(amount));
  }

  public final B noFirstDelay() {
    return firstDelayNanos(0L);
  }

  public final long timeoutNanos() {
    return timeoutNanos;
  }

  public final long timeout(TimeUnit timeUnit) {
    return Objects.requireNonNull(timeUnit, "'timeUnit' should not be 'null'.")
        .convert(timeoutNanos, TimeUnit.NANOSECONDS);
  }

  public final B timeoutNanos(long timeoutNanos) {
    this.timeoutNanos = timeoutNanos;
    return _this();
  }

  public final B timeout(long timeout, TimeUnit timeUnit) {
    return timeoutNanos(Objects.requireNonNull(timeUnit, "'timeUnit' should not be 'null'.").toNanos(timeout));
  }

  public final B noTimeout() {
    return timeoutNanos(NO_TIMEOUT);
  }

  public final Predicate<Exception> retryPredicate() {
    return retryPredicate == null ? exception -> true : retryPredicate;
  }

  public final B retryPredicate(Predicate<Exception> retryPredicate) {
    Objects.requireNonNull(retryPredicate, "'retryPredicate' should not be 'null'.");
    if (this.retryPredicate != null) {
      this.retryPredicate = this.retryPredicate.or(retryPredicate);
    } else {
      this.retryPredicate = retryPredicate;
    }
    return _this();
  }

  public final B retryOn(Class<? extends Exception> exceptionClass) {
    Objects.requireNonNull(exceptionClass, "'exceptionClass' should not be 'null'.");
    return retryPredicate(exceptionClass::isInstance);
  }

  @SuppressWarnings("unchecked")
  public final <E extends Exception> B retryOn(Class<E> exceptionClass, Predicate<E> predicate) {
    Objects.requireNonNull(exceptionClass, "'exceptionClass' should not be 'null'.");
    Objects.requireNonNull(predicate, "'predicate' should not be 'null'.");
    return retryPredicate(exception -> exceptionClass.isInstance(exception) && predicate.test((E) exception));
  }

  public final Predicate<Exception> terminatePredicate() {
    return terminatePredicate != null ? terminatePredicate.or(DEFAULT_TERMINATE_PREDICATE)
        : DEFAULT_TERMINATE_PREDICATE;
  }

  public final B terminatePredicate(Predicate<Exception> terminatePredicate) {
    Objects.requireNonNull(terminatePredicate, "'terminatePredicate' should not be 'null'.");
    if (this.terminatePredicate != null)
    {
      this.terminatePredicate = this.terminatePredicate.or(terminatePredicate);
    } else {
      this.terminatePredicate = terminatePredicate;
    }
    return _this();
  }

  public final B terminateOn(Class<? extends Exception> exceptionClass) {
    Objects.requireNonNull(exceptionClass, "'exceptionClass' should not be 'null'.");
    return terminatePredicate(exceptionClass::isInstance);
  }

  @SuppressWarnings("unchecked")
  public final <E extends Exception> B terminateOn(Class<E> exceptionClass, Predicate<E> predicate) {
    Objects.requireNonNull(exceptionClass, "'exceptionClass' should not be 'null'.");
    Objects.requireNonNull(predicate, "'predicate' should not be 'null'.");
    return terminatePredicate(exception -> exceptionClass.isInstance(exception) && predicate.test((E) exception));
  }

  public final Backoff backOff() {
    return backOff;
  }

  public final B backOff(Backoff backOff) {
    this.backOff = backOff;
    return _this();
  }

  public final B withoutDelay() {
    return backOff(Backoffs.noBackoff())
        .noFirstDelay();
  }

  @SuppressWarnings("unchecked")
  private B _this() {
    return (B) this;
  }

}
