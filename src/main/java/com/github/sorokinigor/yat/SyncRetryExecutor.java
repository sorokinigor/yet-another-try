package com.github.sorokinigor.yat;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * @author Igor Sorokin
 */
public interface SyncRetryExecutor extends Executor {

  <T> T execute(Callable<? extends T> supplier);

  @Override
  default void execute(Runnable command) {
    Objects.requireNonNull(command, "'command' should not be 'null'.");
    execute(() -> { command.run(); return null; });
  }

  default <T> Optional<T> tryExecute(Callable<? extends T> supplier) {
    try {
      return Optional.ofNullable(execute(supplier));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

}
