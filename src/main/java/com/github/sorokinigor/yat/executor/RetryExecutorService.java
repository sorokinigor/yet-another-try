package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.AsyncRetryExecutor;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * INTERNAL API
 * <p>
 * The core implementation of {@link AsyncRetryExecutor}.
 * Does all the work regarding re-scheduling task execution,
 * maintaining the current attempt number and encountered exceptions.
 * <p>
 * Manages the lifecycle of the underlying {@link ScheduledExecutorService}.
 *
 * @author Igor Sorokin
 * @see Policy
 */
final class RetryExecutorService extends AbstractRetryExecutorService {

  private final ScheduledExecutorService executor;
  private final Policy policy;

  RetryExecutorService(ScheduledExecutorService executor, Policy policy) {
    this.executor = Objects.requireNonNull(executor, "'executor' should not be 'null'.");
    this.policy = Objects.requireNonNull(policy, "'policy' should not be 'null'.");
  }

  @Override
  public <T> CompletableFuture<T> submit(Callable<T> task) {
    Objects.requireNonNull(task, "'task' should not be 'null'.");
    CompletableFuture<T> resultFuture = new CompletableFuture<>();
    RetryTask<T> retryTask = new RetryTask<>(0, null, new Context<>(this, resultFuture, task));
    if (policy.firstAttemptInInvocationThread) {
      retryTask.run();
    } else {
      executor.schedule(retryTask, policy.firstDelayNanos, TimeUnit.NANOSECONDS);
    }
    return resultFuture;
  }

  private static final class RetryTask <T> implements Runnable {
    private final int attempt;
    private final Exception lastException;
    private final Context<T> context;

    private RetryTask(int attempt, Exception lastException, Context<T> context) {
      this.attempt = attempt;
      this.lastException = lastException;
      this.context = context;
    }

    @SuppressWarnings("ThrowableNotThrown")
    @Override
    public void run() {
      if (context.future.isDone()) {
        context.logger().debug("The task '{}' was completed externally before '{}/{}' attempt.", attempt + 1,
            context.task, context.delayPolicy().maxAttempts);
      } else {
        long start = System.nanoTime();
        try {
          context.future.complete(context.task.call());
        } catch (Exception e) {
          long finish = System.nanoTime();
          ExceptionUtils.addSuppressed(e, lastException);
          if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }

          Policy policy = context.delayPolicy();
          int maxAttempts = policy.maxAttempts;
          int nextAttempt = attempt + 1;
          if(nextAttempt < maxAttempts && policy.shouldRetry(e) && !Thread.currentThread().isInterrupted()) {
            RetryTask<T> task = new RetryTask<>(nextAttempt, e, context);
            long executionDurationNanos = finish - start;
            long delayNanos = policy.backOff.calculateDelayNanos(nextAttempt, executionDurationNanos);
            context.logger().debug("Attempt '{}/{}' of task '{}' is failed. Next attempt will be in '{}' nanos.",
                attempt + 1, maxAttempts, context.task, delayNanos);
            context.parentExecutor.executor.schedule(task, delayNanos, TimeUnit.NANOSECONDS);
          } else {
            context.logger().debug("'{}/{}' attempts of task '{}' have been failed.", attempt + 1, maxAttempts,
                context.task);
            context.future.completeExceptionally(e);
          }
        }
      }
    }

  }

  private static final class Context<T> {
    private final RetryExecutorService parentExecutor;
    private final CompletableFuture<T> future;
    private final Callable<T> task;

    private Context(RetryExecutorService parentExecutor, CompletableFuture<T> future, Callable<T> task) {
      this.parentExecutor = parentExecutor;
      this.future = future;
      this.task = task;
    }

    private Policy delayPolicy() {
      return parentExecutor.policy;
    }

    private Logger logger() {
      return parentExecutor.logger;
    }

  }

  @Override
  public void shutdown() {
    executor.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return executor.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return executor.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return executor.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return executor.awaitTermination(timeout, unit);
  }

}
