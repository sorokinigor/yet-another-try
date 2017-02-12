package com.github.sorokinigor.yat;

import com.github.sorokinigor.yat.executor.AsyncRetryExecutorBuilder;
import com.github.sorokinigor.yat.executor.StatisticsExecutor;
import com.github.sorokinigor.yat.executor.SyncRetryExecutorBuilder;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Igor Sorokin
 */
public final class Retry {

  private Retry() { throw new IllegalStateException("Not expected to be instantiated"); }

  public static AsyncRetryExecutorBuilder async(ScheduledExecutorService executorService) {
    return new AsyncRetryExecutorBuilder()
        .executorService(executorService);
  }

  public static StatisticsExecutor gatherStatisticFor(RetryExecutor executor) {
    return new StatisticsExecutor(executor);
  }

  public static SyncRetryExecutorBuilder sync() {
    return new SyncRetryExecutorBuilder();
  }

}
