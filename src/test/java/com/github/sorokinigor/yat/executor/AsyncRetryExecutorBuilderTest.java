package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.AsyncRetryExecutor;
import com.github.sorokinigor.yat.Retry;
import com.github.sorokinigor.yat.backoff.Backoff;
import com.github.sorokinigor.yat.backoff.Backoffs;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.exception.RuntimeIOException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.sorokinigor.yat.executor.AsyncRetryExecutorBuilderTest.BuilderAssertion.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Igor Sorokin
 */
public class AsyncRetryExecutorBuilderTest {

  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  @Test
  public void when_executor_is_set_it_should_be_able_to_create_retry_executor() {
    assertThat(createBuilder())
        .verify(builder ->
            assertThat(builder.executorService())
                .isSameAs(executorService)
        )
        .assertBuilt();
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void when_executor_is_not_set_it_should_fail() {
    assertThat(new AsyncRetryExecutorBuilder())
        .assertBuilt();
  }

  @Test
  public void it_should_set_max_attempts() {
    assertThat(createBuilder())
        .assertIsSet(
            builder -> {
              int maxAttempts = ThreadLocalRandom.current()
                  .nextInt(10);
              builder.maxAttempts(maxAttempts);
              return maxAttempts;
            },
            AsyncRetryExecutorBuilder::maxAttempts
        )
        .assertIsSet(
            builder -> { builder.retryOnce(); return 2; },
            AsyncRetryExecutorBuilder::maxAttempts
        )
        .assertIsSet(
            builder -> { builder.doNotRetry(); return 1; },
            AsyncRetryExecutorBuilder::maxAttempts
        )
        .assertBuilt();
  }

  @Test
  public void it_should_set_first_Delay() {
    long firstDelaySeconds = 1;
    TimeUnit firstDelayTimeUnit = TimeUnit.SECONDS;
    assertThat(createBuilder())
        .assertIsSet(
            builder -> {
              long firstDelayNanos = TimeUnit.SECONDS.toNanos(firstDelaySeconds);
              builder.firstDelayNanos(firstDelayNanos);
              return firstDelayNanos;
            },
            AsyncRetryExecutorBuilder::firstDelayNanos
        )
        .verify(builder -> {
          long actual = builder.firstDelay(TimeUnit.SECONDS);
          assertThat(actual)
              .isEqualTo(firstDelaySeconds);
        })
        .assertIsSet(
            builder -> {
              builder.firstDelay(firstDelaySeconds, firstDelayTimeUnit);
              return firstDelayTimeUnit.toNanos(firstDelaySeconds);
            },
            AsyncRetryExecutorBuilder::firstDelayNanos
        )
        .assertIsSet(
            builder -> { builder.noFirstDelay(); return 0L; },
            AsyncRetryExecutorBuilder::firstDelayNanos
        )
        .assertBuilt();
  }

  @SuppressWarnings("ConstantConditions")
  @Test
  public void it_should_set_first_attempt_in_invocation_thread() {
    assertThat(createBuilder())
        .assertIsSet(
            builder -> {
              boolean firstAttemptInInvocationThread = false;
              builder.firstAttemptInInvocationThread(firstAttemptInInvocationThread);
              return firstAttemptInInvocationThread;
            },
            AsyncRetryExecutorBuilder::firstAttemptInInvocationThread
        )
        .assertIsSet(
            builder -> { builder.runFirstAttemptInInvocationThread(); return true; },
            AsyncRetryExecutorBuilder::firstAttemptInInvocationThread
        )
        .assertBuilt();
  }

  @Test
  public void it_should_set_timeout() {
    long timeoutSeconds = 1;
    TimeUnit timeoutTimeUnit = TimeUnit.SECONDS;
    assertThat(createBuilder())
        .assertIsSet(
            builder -> {
              long timeoutNanos = timeoutTimeUnit.toNanos(timeoutSeconds);
              builder.timeoutNanos(timeoutNanos);
              return timeoutNanos;
            },
            AsyncRetryExecutorBuilder::timeoutNanos
        )
        .verify(builder -> {
          long actual = builder.timeout(timeoutTimeUnit);
          assertThat(actual)
              .isEqualTo(timeoutSeconds);
        })
        .assertBuilt(TimeoutExecutorService.class)
        .assertIsSet(
            builder -> {
              builder.timeout(timeoutSeconds, timeoutTimeUnit);
              return timeoutTimeUnit.toNanos(timeoutSeconds);
            },
            AsyncRetryExecutorBuilder::timeoutNanos
        )
        .assertBuilt(TimeoutExecutorService.class)
        .assertIsSet(
            builder -> { builder.noTimeout(); return AsyncRetryExecutorBuilder.NO_TIMEOUT; },
            AsyncRetryExecutorBuilder::timeoutNanos
        )
        .assertBuilt();
  }

  @Test
  public void it_should_set_timeout_executor_service() {
    assertThat(createBuilder())
        .assertIsSet(
            builder -> {
              builder.timeoutExecutorService(executorService);
              return executorService;
            },
            AsyncRetryExecutorBuilder::timeoutExecutorService
        )
        .assertBuilt();
  }

  @Test
  public void it_should_set_backoff() {
    assertThat(createBuilder())
        .assertIsSet(
            builder -> {
              Backoff backoff = Backoffs.fixedRate(1, TimeUnit.SECONDS);
              builder.backOff(backoff);
              return backoff;
            },
            AsyncRetryExecutorBuilder::backOff
        )
        .assertIsSet(
            builder -> { builder.withoutDelay(); return Backoffs.noBackoff(); },
            AsyncRetryExecutorBuilder::backOff
        )
        .assertBuilt();
  }

  @Test
  public void it_should_append_retry_predicates() {
    Exception firstRetryableException = new RuntimeIOException("oi");
    Exception secondRetryableException = new SocketException();
    assertThat(createBuilder())
        .setAndVerify(
            builder -> {
              Class<? extends Exception> exceptionClazz = firstRetryableException.getClass();
              builder.retryOn(exceptionClazz);
              return exceptionClazz;
            },
            (builder, expected) -> {
              boolean shouldRetry = builder.retryPredicate()
                  .test(firstRetryableException);
              assertThat(shouldRetry)
                  .isTrue();
            }
        )
        .setAndVerify(
            builder -> {
              Class<? extends Exception> exceptionClazz = secondRetryableException.getClass();
              builder.retryOn(exceptionClazz);
              return exceptionClazz;
            },
            (builder, expected) -> {
              Predicate<Exception> retryPredicate = builder.retryPredicate();
              boolean shouldRetryFirst = retryPredicate.test(firstRetryableException);
              assertThat(shouldRetryFirst)
                  .isTrue();

              boolean shouldRetrySecond = retryPredicate.test(secondRetryableException);
              assertThat(shouldRetrySecond)
                  .isTrue();
            }
        )
        .assertBuilt();
  }

  @Test
  public void it_should_append_termination_predicates() {
    Exception firstTerminationException = new IllegalStateException();
    Exception secondTerminationException = new IllegalArgumentException();
    int retryCode = 1;
    int terminateCode = 20;
    assertThat(createBuilder())
        .setAndVerify(
            builder -> {
              Class<? extends Exception> exceptionClazz = firstTerminationException.getClass();
              builder.terminateOn(exceptionClazz);
              return exceptionClazz;
            },
            (builder, expected) -> {
              boolean shouldTerminate = builder.terminatePredicate()
                  .test(firstTerminationException);
              assertThat(shouldTerminate)
                  .isTrue();
            }
        )
        .setAndVerify(
            builder -> {
              Class<? extends Exception> exceptionClazz = secondTerminationException.getClass();
              builder.terminateOn(exceptionClazz);
              return exceptionClazz;
            },
            (builder, expected) -> {
              Predicate<Exception> terminatePredicate = builder.terminatePredicate();
              boolean shouldTerminateOnFirst = terminatePredicate.test(firstTerminationException);
              assertThat(shouldTerminateOnFirst)
                  .isTrue();

              boolean shouldTerminateOnSecond = terminatePredicate.test(secondTerminationException);
              assertThat(shouldTerminateOnSecond)
                  .isTrue();
            }
        )
        .setAndVerify(
            builder -> {
              Class<TestException> exceptionClazz = TestException.class;
              builder.retryOn(exceptionClazz, e -> e.code == retryCode);
              builder.terminateOn(exceptionClazz, e -> e.code == terminateCode);
              return exceptionClazz;
            },
            (builder, expected) -> {
              Predicate<Exception> retryPredicate = builder.retryPredicate();
              boolean shouldRetry = retryPredicate.test(new TestException(retryCode));
              assertThat(shouldRetry)
                  .isTrue();
              boolean shouldNotRetry = retryPredicate.test(new TestException(retryCode + 1));
              assertThat(shouldNotRetry)
                  .isFalse();

              Predicate<Exception> terminatePredicate = builder.terminatePredicate();
              boolean shouldTerminate = terminatePredicate.test(new TestException(terminateCode));
              assertThat(shouldTerminate)
                  .isTrue();
              boolean shouldNotTerminate = terminatePredicate.test(new TestException(terminateCode + 1));
              assertThat(shouldNotTerminate)
                  .isFalse();
              boolean shouldTerminateOnFirst = terminatePredicate.test(firstTerminationException);
              assertThat(shouldTerminateOnFirst)
                  .isTrue();
            }
        )
        .assertBuilt();
  }

  private static final class TestException extends Exception {
    private final int code;

    public TestException(int code) {
      this.code = code;
    }
  }

  @Test
  public void it_should_set_should_shutdown_executors() {
    assertThat(createBuilder())
        .assertIsSet(
            builder -> {
              boolean shouldShutdownExecutors = false;
              builder.shouldShutdownExecutors(shouldShutdownExecutors);
              return shouldShutdownExecutors;
            },
            AsyncRetryExecutorBuilder::shouldShutdownExecutors
        )
        .assertIsSet(
            builder -> { builder.shutdownExecutors(); return true; },
            AsyncRetryExecutorBuilder::shouldShutdownExecutors
        )
        .assertIsSet(
            builder -> { builder.doNotShutdownExecutors(); return false; },
            AsyncRetryExecutorBuilder::shouldShutdownExecutors
        )
        .verify(builder -> {
          AsyncRetryExecutor executor = builder.build();
          executor.shutdown();
          assertThat(executor.shutdownNow())
              .isEmpty();
          try {
            assertThat(executor.awaitTermination(1L, TimeUnit.SECONDS))
                .isFalse();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }

          executor.submit(() -> {});
          assertThat(Arrays.asList(executor, executorService))
              .allMatch(e -> !e.isShutdown())
              .allMatch(e -> !e.isTerminated());
        })
        .assertBuilt(AsyncRetryExecutorBuilder.NoShutdownWrapper.class);
  }

  private AsyncRetryExecutorBuilder createBuilder() {
    return Retry.async(executorService);
  }

  @AfterClass
  public void afterAll() {
    executorService.shutdown();
  }

  static final class BuilderAssertion {

    private final AsyncRetryExecutorBuilder builder;

    private BuilderAssertion(AsyncRetryExecutorBuilder builder) {
      this.builder = builder;
    }

    static BuilderAssertion assertThat(AsyncRetryExecutorBuilder builder) {
      return new BuilderAssertion(builder);
    }

    private <T> BuilderAssertion assertIsSet(
        Function<AsyncRetryExecutorBuilder, T> setterToExpectedF,
        Function<AsyncRetryExecutorBuilder, T> getter
    ) {
      return setAndVerify(
          setterToExpectedF,
          (builder, expected) -> {
            T actual = getter.apply(builder);
            Assertions.assertThat(actual)
                .isEqualTo(expected);
          });
    }

    private <T> BuilderAssertion setAndVerify(
        Function<AsyncRetryExecutorBuilder, T> setterToExpectedF,
        BiConsumer<AsyncRetryExecutorBuilder, T> verifier
    ) {
      T expected = setterToExpectedF.apply(builder);
      verifier.accept(builder, expected);
      return this;
    }

    private BuilderAssertion assertBuilt() {
      return assertBuilt(RetryExecutorService.class);
    }

    private BuilderAssertion assertBuilt(Class<? extends AsyncRetryExecutor> expectedImplementation) {
      AsyncRetryExecutor executor = builder.build();
      Assertions.assertThat(executor)
          .isExactlyInstanceOf(expectedImplementation);
      return this;
    }

    private BuilderAssertion verify(Consumer<AsyncRetryExecutorBuilder> verificationClosure) {
      verificationClosure.accept(builder);
      return this;
    }

  }

}
