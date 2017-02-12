package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.Retry;
import com.github.sorokinigor.yat.RetryExecutor;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.github.sorokinigor.yat.backoff.Backoffs.fixedDelay;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Igor Sorokin
 */
public class AsyncRetryExecutorTest extends RetryExecutorTestKit {

  @Test
  public void it_should_invoke_first_attempt_in_invocation_thread() throws Exception {
    try (RetryExecutor executor = createBuilder(createExecutorService())
        .runFirstAttemptInInvocationThread()
        .build()) {
      Thread currentThread = Thread.currentThread();
      CompletableFuture<Thread> future = executor.submit(Thread::currentThread);
      assertCompletedWith(future, currentThread);
    }
  }

  @Test
  public void it_should_invoke_only_first_attempt_in_invocation_thread() throws Exception {
    try (RetryExecutor executor = createBuilder(createExecutorService()).build()) {
      Thread currentThread = Thread.currentThread();
      AtomicBoolean shouldThrowException = new AtomicBoolean(true);
      CompletableFuture<Thread> future = executor.submit(() -> {
        if (shouldThrowException.compareAndSet(true, false)) {
          throw new Exception();
        }
        return Thread.currentThread();
      });
      assertCompletedNotWith(future, currentThread);
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void when_task_is_completed_it_should_not_perform_another_attempt() throws Exception {
    long timeoutMillis = 3_000L;
    try (RetryExecutor executor = createBuilder(createExecutorService())
        .runFirstAttemptInInvocationThread()
        .backOff(fixedDelay(timeoutMillis / 3L, TimeUnit.MILLISECONDS))
        .build()) {
      String expected = "expected";
      Callable<String> task = Mockito.mock(Callable.class);
      when(task.call())
          .thenThrow(RuntimeException.class)
          .thenReturn("unexpected.");
      CompletableFuture<String> future = executor.submit(task);
      future.complete(expected);

      assertCompletedWith(future, expected);
      verify(task, after(timeoutMillis).times(1))
          .call();
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void when_task_is_successful_eventually_it_should_perform_the_requested_number_of_attempts_and_return_result()
      throws Exception {
    int maxAttempts = 5;
    try (RetryExecutor executor = createBuilder(createExecutorService())
        .maxAttempts(maxAttempts)
        .build()) {
      String expected = "expected";
      Callable<String> task = Mockito.mock(Callable.class);
      when(task.call())
          .thenThrow(Stream.generate(RuntimeException::new)
              .limit(maxAttempts - 1)
              .toArray(Throwable[]::new))
          .thenReturn(expected);

      CompletableFuture<String> future = executor.submit(task);
      assertCompletedWith(future, expected);
      verify(task, times(maxAttempts))
          .call();
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void when_task_is_failed_it_should_return_all_of_exception_which_were_thrown_during_invocations()
      throws Exception {
    int maxAttempts = 5;
    try (RetryExecutor executor = createBuilder(createExecutorService())
        .maxAttempts(maxAttempts)
        .build()) {
      Callable<String> task = Mockito.mock(Callable.class);
      when(task.call())
          .thenThrow(Stream.generate(RuntimeException::new)
              .limit(maxAttempts)
              .toArray(Throwable[]::new));
      Class<RuntimeException> exceptionClazz = RuntimeException.class;

      CompletableFuture<String> future = executor.submit(task);
      RuntimeException actualException = assertFailedWith(future, exceptionClazz);
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
  }

  @SuppressWarnings("unchecked")
  @Test
  public void it_should_stop_after_termination_exception()
      throws Exception {
    Class<? extends Exception> terminateExceptionClass = IllegalStateException.class;
    int expectedAttempts = 2;
    try (RetryExecutor executor = createBuilder(createExecutorService())
        .terminateOn(terminateExceptionClass)
        .maxAttempts(expectedAttempts + 1)
        .build()) {
      Callable<String> task = Mockito.mock(Callable.class);
      when(task.call())
          .thenThrow(RuntimeException.class, terminateExceptionClass);

      CompletableFuture<String> future = executor.submit(task);
      assertFailedWith(future, terminateExceptionClass);
      verify(task, times(expectedAttempts))
          .call();
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void it_should_retry_only_retryable_exceptions()
      throws Exception {
    Class<? extends Exception> retryableExceptionClass = IllegalStateException.class;
    int expectedAttempts = 2;
    try (RetryExecutor executor = createBuilder(createExecutorService())
        .retryOn(retryableExceptionClass)
        .maxAttempts(expectedAttempts + 1)
        .build()) {
      Callable<String> task = Mockito.mock(Callable.class);
      Class<? extends Exception> nonRetryableException = RuntimeException.class;
      when(task.call())
          .thenThrow(retryableExceptionClass, nonRetryableException);

      CompletableFuture<String> future = executor.submit(task);
      assertFailedWith(future, nonRetryableException);
      verify(task, times(expectedAttempts))
          .call();
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void it_should_schedule_next_attempt_if_the_thread_was_interrupted() throws Exception {
    try (RetryExecutor executor = create()) {
      Callable<String> task = Mockito.mock(Callable.class);
      when(task.call())
          .thenAnswer(answer -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException();
          });

      CompletableFuture<String> future = executor.submit(task);
      assertFailedWith(future, RuntimeException.class);
      verify(task, after(1_000).times(1))
          .call();
    }
  }

  @Override
  protected RetryExecutor create() {
    return createBuilder(createExecutorService())
        .build();
  }

  private AsyncRetryExecutorBuilder createBuilder(ScheduledExecutorService executorService) {
    return Retry
        .async(executorService)
        .maxAttempts(MAX_ATTEMPTS)
        .withoutDelay();
  }

}
