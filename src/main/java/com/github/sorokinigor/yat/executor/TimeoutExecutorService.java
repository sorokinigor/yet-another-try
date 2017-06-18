package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.AsyncRetryExecutor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * INTERNAL API
 *
 * @author Igor Sorokin
 * @see AsyncRetryExecutorBuilder
 * @see AsyncRetryExecutor
 */
final class TimeoutExecutorService extends AbstractRetryExecutorService {

  private final AsyncRetryExecutor delegate;
  private final ScheduledExecutorService timeoutScheduler;
  private final long timeoutNanos;

  TimeoutExecutorService(AsyncRetryExecutor delegate, ScheduledExecutorService timeoutScheduler, long timeoutNanos) {
    this.delegate = Objects.requireNonNull(delegate, "'delegate' should not be 'null'.");
    this.timeoutScheduler = Objects.requireNonNull(timeoutScheduler, "'timeoutScheduler' should not be 'null'.");
    if (timeoutNanos <= 0L) {
      throw new IllegalArgumentException("'timeoutNanos' must be > 0.");
    }
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
