package com.github.sorokinigor.yat;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Igor Sorokin
 */
public interface RetryExecutor extends ExecutorService, AutoCloseable {

  @Override
  <T> CompletableFuture<T> submit(Callable<T> task);

  @Override
  default <T> CompletableFuture<T> submit(Runnable task, T result) {
    return submit(() -> { task.run(); return result; });
  }

  @Override
  default CompletableFuture<Void> submit(Runnable task) {
    return submit(task, null);
  }

  @Override
  default void execute(Runnable command) {
    submit(command);
  }

  @Override
  <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException;

  @Override
  <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException;

  @Override
  <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException;

  <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException;

  @Override
  default void close() {
    shutdown();
  }

}
