package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.SyncRetryExecutor;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A builder for the {@link SyncRetryExecutor}.
 * <p>
 * Has almost the same properties as {@link AsyncRetryExecutorBuilder},
 * except the ones, which are related to {@link ScheduledExecutorService}.
 *
 * @author Igor Sorokin
 * @see AsyncRetryExecutorBuilder
 */
public final class SyncRetryExecutorBuilder extends AbstractRetryBuilder<SyncRetryExecutorBuilder>
    implements SyncRetryExecutor {

  public SyncRetryExecutor build() {
    Policy policy = buildPolicy(true);
    return new SameThreadRetryExecutor(policy, timeoutNanos());
  }

  @Override
  public <T> T execute(Callable<? extends T> task) {
    return build().execute(task);
  }

  @Override
  public <T> Optional<T> tryExecute(Callable<? extends T> task) {
    return build().tryExecute(task);
  }

}
