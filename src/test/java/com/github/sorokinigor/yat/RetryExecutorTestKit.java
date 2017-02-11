package com.github.sorokinigor.yat;

import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Igor Sorokin
 */
abstract class RetryExecutorTestKit {

  protected static final long TEST_TIMEOUT_MILLIS = 10_000L;
  protected static final int MAX_ATTEMPTS = 2;

  protected abstract RetryExecutor create();

  @Test
  public void when_callable_is_completed_it_should_return_result() {
    try (RetryExecutor executor = create()) {
      String expected = "expected";
      CompletableFuture<String> task = executor.submit(successfulCallable(expected));
      assertThat(task.join())
          .isEqualTo(expected);
    }
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void when_callable_is_failed_it_should_fail() throws Throwable {
    try (RetryExecutor executor = create()) {
      CompletableFuture<Integer> task = executor.submit(failedCallable());
      task.join();
      fail("The task is expected to be failed.");
    } catch (CompletionException e) {
      throw e.getCause();
    }
  }

  @Test
  public void when_runnable_is_completed_it_should_return_successfully() {
    try (RetryExecutor executor = create()) {
      CompletableFuture<Void> task = executor.submit(successfulRunnable());
      assertThat(task.join())
          .isEqualTo(null);
    }
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void when_runnable_is_failed_it_should_fail() throws Throwable {
    try (RetryExecutor executor = create()) {
      CompletableFuture<Void> task = executor.submit(failedRunnable());
      task.join();
      fail("The task is expected to be failed.");
    } catch (CompletionException e) {
      throw e.getCause();
    }
  }

  @Test
  public void it_should_execute_the_task() {
    try (RetryExecutor executor = create()) {
      String expected = "expected";
      CompletableFuture<String> future = new CompletableFuture<>();
      executor.execute(() -> future.complete(expected));
      assertThat(future.join())
          .isEqualTo(expected);
    }
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void when_tasks_for_invoke_all_is_null_it_should_fail() throws InterruptedException {
    try (RetryExecutor executor = create()) {
      executor.invokeAll(null);
    }
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void when_any_of_tasks_for_invoke_all_is_null_it_should_fail()
      throws InterruptedException {
    try (RetryExecutor executor = create()) {
      executor.invokeAll(Collections.singletonList(null));
    }
  }

  @Test
  public void when_invoke_all_is_completed_all_of_the_futures_should_be_done() throws Exception {
    String expectedValue = "value";
    Collection<Callable<String>> tasks = Arrays.asList(
        failedCallable(),
        successfulCallable(expectedValue)
    );
    try (RetryExecutor executor = create()) {
      List<Future<String>> futures = executor.invokeAll(tasks);
      assertThat(futures)
          .allMatch(Future::isDone);
      assertFailedWith(futures.get(0), RuntimeException.class);
      assertCompletedWith(futures.get(1), expectedValue);
    }
  }

  @Test(timeOut = TEST_TIMEOUT_MILLIS)
  public void when_invoke_all_is_completed_all_of_the_futures_should_be_successful() throws Exception {
    String firstValue = "first";
    String secondValue = "second";
    Collection<Callable<String>> tasks = Arrays.asList(
        successfulCallable(firstValue),
        successfulCallable(secondValue)
    );
    try (RetryExecutor executor = create()) {
      List<Future<String>> futures = executor.invokeAll(tasks, 3L, TimeUnit.SECONDS);
      assertThat(futures)
          .allMatch(Future::isDone);
      assertCompletedWith(futures.get(0), firstValue);
      assertCompletedWith(futures.get(1), secondValue);
    }
  }

  @Test(timeOut = TEST_TIMEOUT_MILLIS)
  public void when_invoke_all_with_timeout_is_completed_all_of_the_futures_should_be_done() throws Exception {
    String expectedValue = "value";
    Collection<Callable<String>> tasks = Arrays.asList(
        infiniteLoopCallable(),
        successfulCallable(expectedValue)
    );

    try (RetryExecutor executor = create()) {
      List<Future<String>> futures = executor.invokeAll(tasks, 3L, TimeUnit.SECONDS);
      assertThat(futures)
          .allMatch(Future::isDone);
      assertFailedWith(futures.get(0), TimeoutException.class);
      assertCompletedWith(futures.get(1), expectedValue);
    }
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void when_tasks_for_invoke_any_is_null_it_should_fail() throws Exception {
    try (RetryExecutor executor = create()) {
      executor.invokeAny(null);
    }
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void when_any_of_tasks_for_invoke_any_is_null_it_should_fail()
      throws Exception {
    try (RetryExecutor executor = create()) {
      executor.invokeAny(Collections.singletonList(null));
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void when_tasks_is_empty_it_should_fail() throws Exception {
    try (RetryExecutor executor = create()) {
      executor.invokeAny(Collections.emptyList());
    }
  }

  @Test(expectedExceptions = ExecutionException.class)
  public void when_none_of_tasks_are_successfully_completed_it_should_fail() throws Exception {
    try (RetryExecutor executor = create()) {
      executor.invokeAny(Arrays.asList(failedCallable(), failedCallable()));
    }
  }

  @Test
  public void it_should_return_the_result_of_the_first_successful_task() throws Exception {
    try (RetryExecutor executor = create()) {
      String expected = "value";

      String actual = executor.invokeAny(Arrays.asList(
          successfulCallable(expected),
          failedCallable())
      );
      assertThat(actual)
          .isEqualTo(expected);
    }
  }

  @Test(timeOut = TEST_TIMEOUT_MILLIS)
  public void when_all_of_the_task_are_completed_successfully_it_should_return_the_result_of_the_first_successful_one()
      throws Exception {
    try (RetryExecutor executor = create()) {
      String expected = "value";

      String actual = executor.invokeAny(
          Arrays.asList(
              successfulCallable(expected),
              successfulCallable("notExpected")
          ),
          3L,
          TimeUnit.SECONDS
      );
      assertThat(actual)
          .isEqualTo(expected);
    }
  }

  @Test(expectedExceptions = TimeoutException.class, timeOut = TEST_TIMEOUT_MILLIS)
  public void when_none_of_tasks_are_completed_within_timeout_it_should_fail() throws Exception {
    try (RetryExecutor executor = create()) {
      executor.invokeAny(Collections.singletonList(infiniteLoopCallable()), 1, TimeUnit.MILLISECONDS);
    }
  }

  protected final <T, E extends Exception> E assertFailedWith(Future<T> future, Class<E> exceptionClazz)
      throws InterruptedException {
    try {
      T result = future.get();
      fail("Failed future has been completed without exception. The result is '" + result + "'.");
      throw new Error("Not expected to be thrown.");
    } catch (ExecutionException e) {
      assertThat(e)
          .hasCauseExactlyInstanceOf(exceptionClazz);
      return (E) e.getCause();
    }
  }

  protected final <T> void assertCompletedWith(Future<T> future, T expected)
      throws ExecutionException, InterruptedException {
    T actual = future.get();
    assertThat(actual)
        .isEqualTo(expected);
  }

  protected final <T> void assertCompletedNotWith(Future<T> future, T notExpected)
      throws ExecutionException, InterruptedException {
    T actual = future.get();
    assertThat(actual)
        .isNotEqualTo(notExpected);
  }

  @SuppressWarnings("StatementWithEmptyBody")
  protected final <T> Callable<T> infiniteLoopCallable() {
    return () -> {
      while (!Thread.currentThread().isInterrupted()) {}
      return null;
    };
  }

  protected final <T> Callable<T> failedCallable() {
    return () -> { throw new RuntimeException("expectedException"); };
  }

  protected final <T> Callable<T> successfulCallable(T value) {
    return () -> value;
  }

  protected final Runnable successfulRunnable() {
    return () -> {};
  }

  protected final Runnable failedRunnable() {
    return () -> { throw new RuntimeException("expectedException"); };
  }

  protected final void assertTerminated(ExecutorService...executorServices) {
    assertThat(executorServices)
        .allMatch(ExecutorService::isTerminated);
  }

  protected final void assertShutdown(ExecutorService...executorServices) {
    assertThat(executorServices)
        .allMatch(ExecutorService::isShutdown);
  }

  protected final ScheduledExecutorService createExecutorService() {
    return Executors.newScheduledThreadPool(
        Runtime.getRuntime().availableProcessors(),
        new NamingThreadFactory(Executors.defaultThreadFactory(), getClass().getSimpleName().toLowerCase() + "-")
    );
  }

  private static final class NamingThreadFactory implements ThreadFactory {

    private final ThreadFactory delegate;
    private final AtomicLong count = new AtomicLong();
    private final String prefix;

    private NamingThreadFactory(ThreadFactory delegate, String prefix) {
      this.delegate = Objects.requireNonNull(delegate, "'delegate' should not be 'null'.");
      this.prefix = Objects.requireNonNull(prefix, "'prefix' should not be 'null'.");
    }

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = delegate.newThread(runnable);
      thread.setName(prefix + count.getAndIncrement());
      return thread;
    }

  }
}
