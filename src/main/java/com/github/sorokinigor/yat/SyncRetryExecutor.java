package com.github.sorokinigor.yat;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * @author Igor Sorokin
 */
public interface SyncRetryExecutor extends Executor {

  /**
   *
   * @throws CompletionException if the task is failed with the underlying
   * exception as its cause.
   */
  <T> T execute(Callable<? extends T> supplier) throws CompletionException;

  /**
   * @see SyncRetryExecutor#execute(Callable)
   */
  @Override
  default void execute(Runnable command) throws CompletionException {
    Objects.requireNonNull(command, "'command' should not be 'null'.");
    execute(() -> { command.run(); return null; });
  }

  <T> Optional<T> tryExecute(Callable<? extends T> supplier);

}
