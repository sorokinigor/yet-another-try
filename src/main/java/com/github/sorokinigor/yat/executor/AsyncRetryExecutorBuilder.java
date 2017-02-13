package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.AsyncRetryExecutor;
import com.github.sorokinigor.yat.Retry;
import com.github.sorokinigor.yat.backoff.Backoff;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A builder for the {@link AsyncRetryExecutor}.
 * <p>
 * Configurable parameters:
 * <ul>
 * <li>{@link ScheduledExecutorService} for task execution and timeout handling.
 * <li>max number of attempts {@link AsyncRetryExecutorBuilder#maxAttempts(int)}
 * <li>timeout {@link AsyncRetryExecutorBuilder#timeout(long, TimeUnit)}
 * <li>delay between attempts {@link AsyncRetryExecutorBuilder#backOff(Backoff)}
 * <li>which exceptions should be retried {@link AsyncRetryExecutorBuilder#retryOn(Class)}
 * and which should not {@link AsyncRetryExecutorBuilder#terminateOn(Class)}.
 * <li>whenever or not use the invocation thread for the first attempt
 * {@link AsyncRetryExecutorBuilder#runFirstAttemptInInvocationThread()}
 * <li>whenever or not the shutdown the passed {@link ScheduledExecutorService} or not
 * {@link AsyncRetryExecutorBuilder#doNotShutdownExecutors()}
 * </ul>
 *
 * @author Igor Sorokin
 * @see AbstractRetryBuilder
 * @see Backoff
 * @see Retry
 */
public final class AsyncRetryExecutorBuilder extends AbstractRetryBuilder<AsyncRetryExecutorBuilder> {

  private ScheduledExecutorService executorService;
  private boolean firstAttemptInInvocationThread;
  private ScheduledExecutorService timeoutExecutorService;
  private boolean shouldShutdownExecutors = true;

  public AsyncRetryExecutor build() {
    Policy policy = buildPolicy(firstAttemptInInvocationThread);
    AsyncRetryExecutor executor = new RetryExecutorService(executorService, policy);
    if (!shouldShutdownExecutors) {
      executor = new NoShutdownWrapper(executor);
    }
    if (timeoutNanos > 0L) {
      timeoutExecutorService = timeoutExecutorService == null ? executorService
          : timeoutExecutorService;
      return new TimeoutExecutorService(executor, timeoutExecutorService, timeoutNanos);
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

  static final class NoShutdownWrapper extends AbstractRetryExecutorService {

    private final AsyncRetryExecutor delegate;

    private NoShutdownWrapper(AsyncRetryExecutor delegate) {
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
