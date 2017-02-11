package com.github.sorokinigor.yat;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Igor Sorokin
 */
public final class Retry {

  private Retry() { throw new IllegalStateException("Not expected to be instantiated"); }

  public static RetryExecutorBuilder async(ScheduledExecutorService executorService) {
    return new RetryExecutorBuilder()
        .executorService(executorService);
  }

  public static StatisticsExecutor gatherStatisticFor(RetryExecutor executor) {
    return new StatisticsExecutor(executor);
  }

}
