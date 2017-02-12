package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.Retry;
import com.github.sorokinigor.yat.SyncRetryExecutor;
import com.github.sorokinigor.yat.backoff.Backoffs;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static com.github.sorokinigor.yat.backoff.Backoffs.fixedDelay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

/**
 * @author Igor Sorokin
 */
public class SameThreadRetryExecutorTest {

  @SuppressWarnings("unchecked")
  @Test
  public void when_task_is_successful_eventually_it_should_perform_the_requested_number_of_attempts_and_return_result()
      throws Exception {
    int maxAttempts = 5;
    SyncRetryExecutor executor = Retry.sync()
        .withoutDelay()
        .maxAttempts(maxAttempts);
    String expected = "expected";
    Callable<String> task = Mockito.mock(Callable.class);
    when(task.call())
        .thenThrow(Stream.generate(RuntimeException::new)
            .limit(maxAttempts - 1)
            .toArray(Throwable[]::new))
        .thenReturn(expected);

    String actual = executor.execute(task);
    assertThat(actual)
        .isEqualTo(expected);
    verify(task, times(maxAttempts))
        .call();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void when_task_is_failed_it_should_return_all_of_exception_which_were_thrown_during_invocations()
      throws Exception {
    int maxAttempts = 5;
    SyncRetryExecutor executor = Retry.sync()
        .withoutDelay()
        .maxAttempts(maxAttempts);
    Callable<String> task = Mockito.mock(Callable.class);
    when(task.call())
        .thenThrow(Stream.generate(RuntimeException::new)
            .limit(maxAttempts)
            .toArray(Throwable[]::new));
    Class<RuntimeException> exceptionClazz = RuntimeException.class;

    RuntimeException actualException = assertFailedWith(executor, task, exceptionClazz);
    verify(task, times(maxAttempts))
        .call();
    Throwable current = actualException;
    for (int i = 0; i < maxAttempts - 1; i++) {
      Throwable[] suppressed = current.getSuppressed();
      assertThat(suppressed)
          .hasSize(1)
          .allMatch(exceptionClazz::isInstance);
      current = suppressed[0];
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void it_should_stop_after_termination_exception() throws Exception {
    Class<? extends Exception> terminateExceptionClass = IllegalStateException.class;
    int expectedAttempts = 2;
    SyncRetryExecutor executor = Retry.sync()
        .maxAttempts(expectedAttempts + 1)
        .withoutDelay()
        .terminateOn(terminateExceptionClass);

    Callable<String> task = Mockito.mock(Callable.class);
    when(task.call())
        .thenThrow(RuntimeException.class, terminateExceptionClass);

    assertFailedWith(executor, task, terminateExceptionClass);
    verify(task, times(expectedAttempts))
        .call();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void it_should_retry_only_retryable_exceptions() throws Exception {
    Class<? extends Exception> retryableExceptionClass = IllegalStateException.class;
    int expectedAttempts = 2;
    SyncRetryExecutor executor = Retry.sync()
        .maxAttempts(expectedAttempts + 1)
        .backOff(fixedDelay(1L, TimeUnit.SECONDS))
        .retryOn(retryableExceptionClass);
    Callable<String> task = Mockito.mock(Callable.class);
    Class<? extends Exception> nonRetryableException = RuntimeException.class;
    when(task.call())
        .thenThrow(retryableExceptionClass, nonRetryableException);

    assertFailedWith(executor, task, nonRetryableException);
    verify(task, times(expectedAttempts))
        .call();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void it_should_perform_next_attempt_if_the_thread_was_interrupted() throws Exception {
    SyncRetryExecutor executor = Retry.sync()
        .withoutDelay()
        .build();
    Callable<String> task = Mockito.mock(Callable.class);
    when(task.call())
        .thenAnswer(answer -> {
          Thread.currentThread().interrupt();
          throw new RuntimeException();
        });

    try {
      assertFailedWith(executor, task, RuntimeException.class);
      verify(task, times(1))
          .call();
    } finally {
      Thread.interrupted();
    }
  }

  @SuppressWarnings("unchecked")
  @Test(timeOut = 10_000L)
  public void when_thread_is_while_sleeping_for_a_delay_it_should_not_schedule_perform_nextAttempt() throws Exception {
    long delaySeconds = 2L;
    TimeUnit delayTimeunit = TimeUnit.SECONDS;
    SyncRetryExecutor executor = Retry.sync()
        .firstDelay(delaySeconds, delayTimeunit)
        .backOff(fixedDelay(delaySeconds, delayTimeunit))
        .build();
    Callable<String> task = Mockito.mock(Callable.class);
    when(task.call())
        .thenThrow(RuntimeException.class);
    CompletableFuture<Boolean> interruptedFlag = new CompletableFuture<>();
    Thread executionThread = new Thread(() -> {
      assertFailedWith(executor, task, RuntimeException.class);
      interruptedFlag.complete(Thread.currentThread().isInterrupted());
    });
    executionThread.setName("same-thread-executor-test-thread");
    executionThread.start();

    Thread.sleep(delayTimeunit.toMillis(delaySeconds) / 2L);
    executionThread.interrupt();
    executionThread.join();
    assertThat(interruptedFlag)
        .isCompletedWithValue(true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void first_attempt_in_invocation_thread_should_be_true() {
    new SameThreadRetryExecutor(new Policy(e -> true, Backoffs.noBackoff(), 1L, 1, false), 1);
  }

  @Test
  public void when_timeout_is_reached_it_should_fail() {
    long timeout = 2_000L;
    TimeUnit timeoutTimeunit = TimeUnit.MILLISECONDS;
    int maxAttempts = 10;
    SyncRetryExecutor executor = Retry.sync()
        .timeout(timeout, timeoutTimeunit)
        .maxAttempts(maxAttempts)
        .backOff(fixedDelay(timeout / (maxAttempts / 2), timeoutTimeunit));
    RuntimeException exception = assertFailedWith(executor, () -> { throw new Exception(); }, RuntimeException.class);
    assertThat(exception)
        .hasCauseExactlyInstanceOf(TimeoutException.class);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void when_command_is_null_it_should_fail() {
    Retry.sync()
        .withoutDelay()
        .retryOnce()
        .execute((Runnable) null);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void when_supplier_is_null_it_should_fail() {
    Retry.sync()
        .withoutDelay()
        .retryOnce()
        .execute((Callable<?>) null);
  }

  @Test
  public void it_should_execute_command() {
    Runnable command = Mockito.mock(Runnable.class);
    Retry.sync()
        .execute(command);

    verify(command, times(1))
        .run();
  }

  @Test
  public void when_execution_is_successful_it_should_return_non_empty_result() {
    String expected = "result";
    Optional<String> actual = Retry.sync()
        .tryExecute(() -> expected);
    assertThat(actual)
        .hasValue(expected);
  }

  @Test
  public void when_execution_is_failed_it_should_return_empty_result() {
    Optional<String> actual = Retry.sync()
        .withoutDelay()
        .retryOnce()
        .tryExecute(() -> { throw new Exception(); });
    assertThat(actual)
        .isEmpty();
  }

  private <T, E extends Exception> E assertFailedWith(
      SyncRetryExecutor executor,
      Callable<T> task,
      Class<E> exceptionClazz
  ) {
    try {
      T result = executor.execute(task);
      fail("Failed future has been completed without exception. The result is '" + result + "'.");
      throw new Error("Not expected to be thrown.");
    } catch (Exception e) {
      assertThat(e)
          .isExactlyInstanceOf(exceptionClazz);
      return (E) e;
    }
  }

}
