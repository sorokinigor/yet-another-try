package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.SyncRetryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Igor Sorokin
 */
final class SameThreadRetryExecutor implements SyncRetryExecutor {

  private static final Logger logger = LoggerFactory.getLogger(SameThreadRetryExecutor.class);

  private final Policy policy;
  private final long timeoutNanos;

  SameThreadRetryExecutor(Policy policy, long timeoutNanos) {
    this.policy = Objects.requireNonNull(policy, "'policy' should not be 'null'.");
    if (!policy.firstAttemptInInvocationThread) {
      throw new IllegalArgumentException("'firstAttemptInInvocationThread' should be 'true'.");
    }
    this.timeoutNanos = timeoutNanos;
  }

  @Override
  public <T> T execute(Callable<? extends T> supplier) {
    Objects.requireNonNull(supplier, "'supplier' should not be 'null'.");
    Exception lastException = null;
    int attempt = 0;
    Thread currentThread = Thread.currentThread();
    long delayNanos = policy.firstDelayNanos;
    long deadline = System.nanoTime() + timeoutNanos;
    long timeoutDelta = timeoutNanos > 0L ? timeoutNanos :
        1L;
    while (!currentThread.isInterrupted() &&
        attempt < policy.maxAttempts &&
        (lastException == null || policy.shouldRetry(lastException)) &&
        timeoutDelta > 0L)
    {
      if (delayNanos > 0L) {
        try {
          Thread.sleep(TimeUnit.NANOSECONDS.toMillis(delayNanos));
        } catch (InterruptedException e) {
          logger.warn("Execution has been interrupted.");
          currentThread.interrupt();
          break;
        }
      }

      long start = System.nanoTime();
      try {
        return supplier.call();
      } catch (Exception e) {
        long finish = System.nanoTime();
        if (lastException != null) {
          e.addSuppressed(lastException);
        }
        lastException = e;

        if (timeoutNanos > 0L) {
          timeoutDelta = deadline - finish;
          if (timeoutDelta <= 0L) {
            TimeoutException timeoutException = new TimeoutException("Got timeout after '" + timeoutNanos + "' nanos.");
            timeoutException.addSuppressed(lastException);
            lastException = timeoutException;
          }
        }

        attempt++;
        long executionDurationNanos = finish - start;
        delayNanos = policy.backOff.calculateDelayNanos(attempt, executionDurationNanos);
        logger.debug("Attempt '{}/{}' is failed. Next attempt will be in '{}' nanos.",
            attempt, policy.maxAttempts, delayNanos);
      }
    }
    logger.debug("'{}/{}' attempts have been failed.", attempt, policy.maxAttempts);
    throw new CompletionException(lastException);
  }

  @Override
  public <T> Optional<T> tryExecute(Callable<? extends T> supplier) {
    try {
      return Optional.ofNullable(execute(supplier));
    } catch (Exception e) {
      logger.error("Unable to execute task '{}'.", supplier, e);
      return Optional.empty();
    }
  }

}
