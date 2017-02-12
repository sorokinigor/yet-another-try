package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.AsyncRetryExecutor;
import com.github.sorokinigor.yat.Retry;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Igor Sorokin
 */
public class TimeoutExecutorServiceTest extends AsyncRetryExecutorTestKit {

  private static final long TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(TEST_TIMEOUT_MILLIS / 8L);

  @Test(expectedExceptions = TimeoutException.class, timeOut = TEST_TIMEOUT_MILLIS)
  public void when_task_is_not_completed_within_timeout_it_should_complete_future_with_exception() throws Throwable {
    try (AsyncRetryExecutor executor = create()) {
      CompletableFuture<String> task = executor.submit(infiniteLoopCallable());
      task.join();
    } catch (CompletionException e) {
      throw e.getCause();
    }
  }

  @Test
  public void when_task_is_completed_within_timeout_it_should_return_result() {
    try (AsyncRetryExecutor executor = create()) {
      String expected = "expected";
      CompletableFuture<String> task = executor.submit(() -> {
        Thread.sleep(100L);
        return expected;
      });
      assertThat(task.join())
          .isEqualTo(expected);
    }
  }

  @Test
  public void when_terminated_it_should_terminate_both_timeout_executor_and_retry_executor()
      throws InterruptedException {
    ScheduledExecutorService executorService = createExecutorService();
    ScheduledExecutorService timeoutExecutor = createExecutorService();
    AsyncRetryExecutor executor = create(executorService, timeoutExecutor);
    executor.submit(successfulRunnable());

    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.SECONDS);
    assertShutdown(executor, executorService, timeoutExecutor);
    assertTerminated(executor, executorService, timeoutExecutor);
  }

  @Test
  public void when_shutdown_immediately_it_should_shutdown_both_timeout_executor_and_retry_executor()
      throws InterruptedException {
    ScheduledExecutorService executorService = createExecutorService();
    ScheduledExecutorService timeoutExecutor = createExecutorService();
    AsyncRetryExecutor executor = create(executorService, timeoutExecutor);
    executor.submit(infiniteLoopCallable());

    List<Runnable> neverExecutedTasks = executor.shutdownNow();
    assertThat(neverExecutedTasks)
        .size()
        .isBetween(1, 2);
    assertShutdown(executor, executorService, timeoutExecutor);
  }

  @Test
  public void when_shutdown__it_should_shutdown_both_timeout_executor_and_retry_executor()
      throws InterruptedException {
    ScheduledExecutorService executorService = createExecutorService();
    ScheduledExecutorService timeoutExecutor = createExecutorService();
    AsyncRetryExecutor executor = create(executorService, timeoutExecutor);
    executor.submit(infiniteLoopCallable());

    executor.shutdown();
    assertShutdown(executor, executorService, timeoutExecutor);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void when_timeout_nanos_is_less_than_one_it_should_fail() {
    ScheduledExecutorService executorService = createExecutorService();
    try (AsyncRetryExecutor retryExecutor = create(executorService, executorService)) {
      new TimeoutExecutorService(retryExecutor, executorService, 0L);
    }
  }

  private AsyncRetryExecutor create(ScheduledExecutorService executorService, ScheduledExecutorService timeoutExecutor) {
    return Retry
        .async(executorService)
        .timeoutExecutorService(timeoutExecutor)
        .maxAttempts(MAX_ATTEMPTS)
        .withoutDelay()
        .timeoutNanos(TIMEOUT_NANOS)
        .build();
  }

  @Override
  protected AsyncRetryExecutor create() {
    ScheduledExecutorService executorService = createExecutorService();
    return create(executorService, executorService);
  }

}
