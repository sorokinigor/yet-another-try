package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.SyncRetryExecutor;

import java.util.concurrent.Callable;

/**
 * @author Igor Sorokin
 */
public final class SyncRetryExecutorBuilder extends AbstractRetryBuilder<SyncRetryExecutorBuilder>
    implements SyncRetryExecutor {

  public SyncRetryExecutor build() {
    Policy policy = buildPolicy(true);
    return new SameThreadRetryExecutor(policy, timeoutNanos);
  }

  @Override
  public <T> T execute(Callable<? extends T> supplier) {
    return build().execute(supplier);
  }

}
