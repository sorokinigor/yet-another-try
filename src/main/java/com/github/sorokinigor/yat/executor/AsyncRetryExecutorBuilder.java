package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.RetryExecutor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Igor Sorokin
 */
public final class AsyncRetryExecutorBuilder extends AbstractRetryBuilder<AsyncRetryExecutorBuilder> {

  private ScheduledExecutorService executorService;
  private boolean firstAttemptInInvocationThread;
  private ScheduledExecutorService timeoutExecutorService;
  private boolean shouldShutdownExecutors = true;

  public RetryExecutor build() {
    Policy policy = buildPolicy(firstAttemptInInvocationThread);
    RetryExecutor executor = new AsyncRetryExecutor(executorService, policy);
    if (!shouldShutdownExecutors) {
      executor = new NoShutdownWrapper(executor);
    }
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

  public boolean shouldShutdownExecutors() {
    return shouldShutdownExecutors;
  }

  public AsyncRetryExecutorBuilder shouldShutdownExecutors(boolean shouldShutdownExecutors) {
    this.shouldShutdownExecutors = shouldShutdownExecutors;
    return this;
  }

  public AsyncRetryExecutorBuilder shutdownExecutors() {
    return shouldShutdownExecutors(true);
  }

  public AsyncRetryExecutorBuilder doNotShutdownExecutors() {
    return shouldShutdownExecutors(false);
  }

  static final class NoShutdownWrapper extends AbstractRetryExecutor {

    private final RetryExecutor delegate;

    private NoShutdownWrapper(RetryExecutor delegate) {
      this.delegate = delegate;
    }

    @Override
    public <T> CompletableFuture<T> submit(Callable<T> task) {
      return delegate.submit(task);
    }

    @Override
    public void shutdown() {}

    @Override
    public List<Runnable> shutdownNow() { return Collections.emptyList(); }

    @Override
    public boolean isShutdown() { return false; }

    @Override
    public boolean isTerminated() { return false; }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException { return false; }

  }

}
