package com.github.sorokinigor.yat;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The main abstraction for asynchronous retries.
 * <p>
 * It is fully compatible with the code, which uses plain {@link ExecutorService},
 * as it implements it.
 * <p>
 * In addition, the {@link Future} is replaced with the {@link CompletableFuture}
 * in the return types of the methods whenever it is possible.
 *
 * @author Igor Sorokin
 * @see Retry#async(ScheduledExecutorService)
 * @see Retry#async()
 */
public interface AsyncRetryExecutor extends ExecutorService, AutoCloseable {

  /**
   *
   * @return the {@link CompletableFuture} which is completed with the result
   * of task execution upon successful completion.
   * Upon the task failure it is completed with the last exception,
   * which contains the previous exception in {@link Exception#getSuppressed()}[0].
   * Each of the encountered exceptions has it's previous exception in `suppressed` array.
   *
   */
  @Override
  <T> CompletableFuture<T> submit(Callable<T> task);

  /**
   * @see AsyncRetryExecutor#submit(Callable)
   */
  @Override
  default <T> CompletableFuture<T> submit(Runnable task, T result) {
    return submit(Executors.callable(task, result));
  }

  /**
   * @see AsyncRetryExecutor#submit(Callable)
   */
  @Override
  default CompletableFuture<Void> submit(Runnable task) {
    return submit(task, null);
  }

  /**
   * @see AsyncRetryExecutor#submit(Callable)
   */
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

  /**
   * @see ExecutorService#shutdown()
   */
  @Override
  default void close() {
    shutdown();
  }

}
