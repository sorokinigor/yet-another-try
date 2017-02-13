package com.github.sorokinigor.yat;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * The abstraction for synchronous retries.
 *
 * @author Igor Sorokin
 * @see Retry#sync()
 */
public interface SyncRetryExecutor extends Executor {

  /**
   * @throws CompletionException if the task is failed with the underlying
   * exception as its cause.
   */
  <T> T execute(Callable<? extends T> task) throws CompletionException;

  /**
   * @see SyncRetryExecutor#execute(Callable)
   */
  @Override
  default void execute(Runnable command) throws CompletionException {
    Objects.requireNonNull(command, "'command' should not be 'null'.");
    execute(() -> { command.run(); return null; });
  }

  /**
   * @return {@link Optional#empty()} if the task has failed or
   * the {@link Optional}, which contains the result of task execution, if it has been successful.
   * @see SyncRetryExecutor#execute(Callable)
   */
  <T> Optional<T> tryExecute(Callable<? extends T> task);

}
