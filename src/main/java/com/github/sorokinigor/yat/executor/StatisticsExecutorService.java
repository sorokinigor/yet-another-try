package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.AsyncRetryExecutor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Igor Sorokin
 */
public final class StatisticsExecutorService extends AbstractRetryExecutorService {

  private final AsyncRetryExecutor delegate;
  private final LongAdder successful;
  private final LongAdder failed;
  private final LongAdder failedAttempts;

  public StatisticsExecutorService(AsyncRetryExecutor delegate) {
    this.delegate = Objects.requireNonNull(delegate, "'delegate' should not be 'null'.");
    this.successful = new LongAdder();
    this.failed = new LongAdder();
    this.failedAttempts = new LongAdder();
  }

  @Override
  public <T> CompletableFuture<T> submit(Callable<T> task) {
    Objects.requireNonNull(task, "'task' should not be 'null'.");
    return delegate
        .submit(() -> {
          try {
            return task.call();
          } catch (Exception e) {
            failedAttempts.increment();
            throw e;
          }
        })
        .whenComplete((ignored, exception) -> {
          if (exception == null) {
            successful.increment();
          } else {
            failed.increment();
          }
        });
  }

  public Stats stats() {
    return new Stats(successful.longValue(), failed.longValue(), failedAttempts.longValue());
  }

  public static final class Stats {
    public final long successful;
    public final long failed;
    public final long failedAttempts;

    public Stats(long successful, long failed, long failedAttempts) {
      this.successful = successful;
      this.failed = failed;
      this.failedAttempts = failedAttempts;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Stats stats = (Stats) o;
      return successful == stats.successful && failed == stats.failed
          && failedAttempts == stats.failedAttempts;
    }

    @Override
    public int hashCode() {
      int result = (int) (successful ^ (successful >>> 32));
      result = 31 * result + (int) (failed ^ (failed >>> 32));
      result = 31 * result + (int) (failedAttempts ^ (failedAttempts >>> 32));
      return result;
    }

    @Override
    public String toString() {
      return "Stats{" +
          "successful=" + successful +
          ", failed=" + failed +
          ", failedAttempts=" + failedAttempts +
          '}';
    }
  }

  @Override
  public void shutdown() {
    logger.info("Stats: '{}'.", stats());
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }

}
