package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.AsyncRetryExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * A trait, which implements most of the methods of {@link AsyncRetryExecutor}.
 *
 * @author Igor Sorokin
 */
abstract class AbstractRetryExecutorService implements AsyncRetryExecutor {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public final <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return invokeAll(tasks, OptionalLong.empty());
  }

  @Override
  public final <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    return invokeAll(tasks, OptionalLong.of(unit.toNanos(timeout)));
  }

  @SuppressWarnings({"unchecked", "OptionalUsedAsFieldOrParameterType"})
  private <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, OptionalLong timeoutNanos)
      throws InterruptedException {
    Objects.requireNonNull(tasks, "'tasks' should not be 'null'.");
    CompletableFuture[] futures = new CompletableFuture[tasks.size()];
    int index = 0;
    for (Callable<T> task : tasks) {
      futures[index++] = submit(task);
    }
    boolean completeWithTimeout = timeoutNanos.isPresent();
    try {
      if (timeoutNanos.isPresent()) {
        CompletableFuture.allOf(futures)
            .get(timeoutNanos.getAsLong(), TimeUnit.NANOSECONDS);
        completeWithTimeout = false;
      } else {
        CompletableFuture.allOf(futures)
            .get();
      }
    } catch (ExecutionException | TimeoutException ignored) {
      logger.error("Got exception while waiting for completion of '{}' tasks.", tasks.size(), ignored);
    }
    if (completeWithTimeout) {
      TimeoutException timeoutException = new TimeoutException(
          "Got timeout after '" + timeoutNanos.getAsLong() + "' nanos."
      );
      for (CompletableFuture future : futures) {
        future.completeExceptionally(timeoutException);
      }
    }
    return Arrays.stream(futures)
        .map(future -> (Future<T>) future)
        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
  }

  @Override
  public final <T> T invokeAny(Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    return doInvokeAny(tasks)
        .get();
  }

  @Override
  public final <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return doInvokeAny(tasks)
        .get(timeout, unit);
  }

  @SuppressWarnings("unchecked")
  private <T> CompletableFuture<T> doInvokeAny(Collection<? extends Callable<T>> tasks) {
    Objects.requireNonNull(tasks, "'tasks' should not be 'null'.");
    if (tasks.isEmpty()) {
      throw new IllegalArgumentException("'tasks' should not be empty.");
    }
    CompletableFuture[] futures = new CompletableFuture[tasks.size()];
    int index = 0;
    for (Callable<T> task : tasks) {
      futures[index++] = submit(task);
    }
    return (CompletableFuture<T>) CompletableFuture.anyOf(futures);
  }

  @Override
  public final <T> CompletableFuture<T> submit(Runnable task, T result) {
    return AsyncRetryExecutor.super.submit(task, result);
  }

  @Override
  public final CompletableFuture<Void> submit(Runnable task) {
    return AsyncRetryExecutor.super.submit(task);
  }

  @Override
  public final void execute(Runnable command) {
    AsyncRetryExecutor.super.execute(command);
  }

  @Override
  public final void close() {
    AsyncRetryExecutor.super.close();
  }

}
