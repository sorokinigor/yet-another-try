package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.RetryExecutor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Igor Sorokin
 */
final class TimeoutExecutor extends AbstractRetryExecutor {

  private final RetryExecutor delegate;
  private final ScheduledExecutorService timeoutScheduler;
  private final long timeoutNanos;

  TimeoutExecutor(RetryExecutor delegate, ScheduledExecutorService timeoutScheduler, long timeoutNanos) {
    this.delegate = Objects.requireNonNull(delegate, "'delegate' should not be 'null'.");
    this.timeoutScheduler = Objects.requireNonNull(timeoutScheduler, "'timeoutScheduler' should not be 'null'.");
    this.timeoutNanos = timeoutNanos;
  }

  @Override
  public <T> CompletableFuture<T> submit(Callable<T> task) {
    CompletableFuture<T> future = delegate.submit(task);
    timeoutScheduler.schedule(
        () -> future.completeExceptionally(new TimeoutException("Got timeout after '" + timeoutNanos + "' nanos.")),
        timeoutNanos,
        TimeUnit.NANOSECONDS
    );
    return future;
  }

  @Override
  public void shutdown() {
    timeoutScheduler.shutdown();
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return Stream.concat(delegate.shutdownNow().stream(), timeoutScheduler.shutdownNow().stream())
        .distinct()
        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown() && timeoutScheduler.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated() && timeoutScheduler.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit) && timeoutScheduler.awaitTermination(timeout, unit);
  }

}
