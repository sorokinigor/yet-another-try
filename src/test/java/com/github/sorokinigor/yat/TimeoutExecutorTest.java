package com.github.sorokinigor.yat;

import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Igor Sorokin
 */
public class TimeoutExecutorTest extends RetryExecutorTestKit {

  private static final long TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(TEST_TIMEOUT_MILLIS / 8L);

  @Test(timeOut = TEST_TIMEOUT_MILLIS, expectedExceptions = TimeoutException.class)
  public void when_task_is_not_completed_within_timeout_it_should_complete_future_with_exception() throws Throwable {
    try (RetryExecutor executor = create()) {
      CompletableFuture<String> task = executor.submit(infiniteLoopCallable());
      task.join();
    } catch (CompletionException e) {
      throw e.getCause();
    }
  }

  @Test
  public void when_task_is_completed_within_timeout_it_should_return_result() {
    try (RetryExecutor executor = create()) {
      Integer expected = 1;
      CompletableFuture<Integer> task = executor.submit(() -> {
        Thread.sleep(100);
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
    RetryExecutor executor = create(executorService, timeoutExecutor);
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
    RetryExecutor executor = create(executorService, timeoutExecutor);
    executor.submit(infiniteLoopCallable());

    List<Runnable> neverExecutedTasks = executor.shutdownNow();
    assertThat(neverExecutedTasks)
        //The scheduled timeout task
        .size()
        .isBetween(1, 2);
    assertShutdown(executor, executorService, timeoutExecutor);
  }

  @Test
  public void when_shutdown__it_should_shutdown_both_timeout_executor_and_retry_executor()
      throws InterruptedException {
    ScheduledExecutorService executorService = createExecutorService();
    ScheduledExecutorService timeoutExecutor = createExecutorService();
    RetryExecutor executor = create(executorService, timeoutExecutor);
    executor.submit(infiniteLoopCallable());

    executor.shutdown();
    assertShutdown(executor, executorService, timeoutExecutor);
  }

  private RetryExecutor create(ScheduledExecutorService executorService, ScheduledExecutorService timeoutExecutor) {
    return Retry
        .async(executorService)
        .timeoutExecutorService(timeoutExecutor)
        .maxAttempts(MAX_ATTEMPTS)
        .withoutDelay()
        .timeoutNanos(TIMEOUT_NANOS)
        .build();
  }

  @Override
  protected RetryExecutor create() {
    ScheduledExecutorService executorService = createExecutorService();
    return create(executorService, executorService);
  }

}
