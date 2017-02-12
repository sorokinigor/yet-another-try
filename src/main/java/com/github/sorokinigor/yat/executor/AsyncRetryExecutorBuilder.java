package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.RetryExecutor;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Igor Sorokin
 */
public final class AsyncRetryExecutorBuilder extends AbstractRetryBuilder<AsyncRetryExecutorBuilder> {

  private ScheduledExecutorService executorService;
  private boolean firstAttemptInInvocationThread;
  private ScheduledExecutorService timeoutExecutorService;

  public RetryExecutor build() {
    Policy policy = buildPolicy(firstAttemptInInvocationThread);
    RetryExecutor executor = new AsyncRetryExecutor(executorService, policy);
    if (timeoutNanos > 0L) {
      timeoutExecutorService = timeoutExecutorService == null ? executorService
          : timeoutExecutorService;
      return new TimeoutExecutor(executor, timeoutExecutorService, timeoutNanos);
    } else {
      return executor;
    }
  }

  public ScheduledExecutorService executorService() {
    return executorService;
  }

  public AsyncRetryExecutorBuilder executorService(ScheduledExecutorService executorService) {
    this.executorService = executorService;
    return this;
  }

  public boolean firstAttemptInInvocationThread() {
    return firstAttemptInInvocationThread;
  }

  public AsyncRetryExecutorBuilder runFirstAttemptInInvocationThread() {
    return firstAttemptInInvocationThread(true);
  }

  public AsyncRetryExecutorBuilder firstAttemptInInvocationThread(boolean firstAttemptInInvocationThread) {
    this.firstAttemptInInvocationThread = firstAttemptInInvocationThread;
    return this;
  }

  public ScheduledExecutorService timeoutExecutorService() {
    return timeoutExecutorService;
  }

  public AsyncRetryExecutorBuilder timeoutExecutorService(ScheduledExecutorService timeoutExecutorService) {
    this.timeoutExecutorService = timeoutExecutorService;
    return this;
  }

}
