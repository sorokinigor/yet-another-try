package com.github.sorokinigor.yat;

import com.github.sorokinigor.yat.backoff.Backoff;
import com.github.sorokinigor.yat.backoff.Backoffs;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.exception.RuntimeIOException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.sorokinigor.yat.RetryExecutorBuilderTest.BuilderAssertion.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Igor Sorokin
 */
public class RetryExecutorBuilderTest {

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
    assertThat(new RetryExecutorBuilder())
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
            RetryExecutorBuilder::maxAttempts
        )
        .assertIsSet(
            builder -> { builder.retryOnce(); return 2; },
            RetryExecutorBuilder::maxAttempts
        )
        .assertIsSet(
            builder -> { builder.doNotRetry(); return 1; },
            RetryExecutorBuilder::maxAttempts
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
            RetryExecutorBuilder::firstDelayNanos
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
            RetryExecutorBuilder::firstDelayNanos
        )
        .assertIsSet(
            builder -> { builder.noFirstDelay(); return 0L; },
            RetryExecutorBuilder::firstDelayNanos
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
            RetryExecutorBuilder::firstAttemptInInvocationThread
        )
        .assertIsSet(
            builder -> { builder.runFirstAttemptInInvocationThread(); return true; },
            RetryExecutorBuilder::firstAttemptInInvocationThread
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
            RetryExecutorBuilder::timeoutNanos
        )
        .verify(builder -> {
          long actual = builder.timeout(timeoutTimeUnit);
          assertThat(actual)
              .isEqualTo(timeoutSeconds);
        })
        .assertBuilt(TimeoutExecutor.class)
        .assertIsSet(
            builder -> {
              builder.timeout(timeoutSeconds, timeoutTimeUnit);
              return timeoutTimeUnit.toNanos(timeoutSeconds);
            },
            RetryExecutorBuilder::timeoutNanos
        )
        .assertBuilt(TimeoutExecutor.class)
        .assertIsSet(
            builder -> { builder.noTimeout(); return RetryExecutorBuilder.NO_TIMEOUT; },
            RetryExecutorBuilder::timeoutNanos
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
            RetryExecutorBuilder::timeoutExecutorService
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
            RetryExecutorBuilder::backOff
        )
        .assertIsSet(
            builder -> { builder.withoutDelay(); return Backoffs.noBackoff(); },
            RetryExecutorBuilder::backOff
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
        .assertBuilt();
  }

  private RetryExecutorBuilder createBuilder() {
    return Retry.async(executorService);
  }

  @AfterClass
  public void afterAll() {
    executorService.shutdown();
  }

  static final class BuilderAssertion {

    private final RetryExecutorBuilder builder;

    private BuilderAssertion(RetryExecutorBuilder builder) {
      this.builder = builder;
    }

    static BuilderAssertion assertThat(RetryExecutorBuilder builder) {
      return new BuilderAssertion(builder);
    }

    private <T> BuilderAssertion assertIsSet(
        Function<RetryExecutorBuilder, T> setterToExpectedF,
        Function<RetryExecutorBuilder, T> getter
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
        Function<RetryExecutorBuilder, T> setterToExpectedF,
        BiConsumer<RetryExecutorBuilder, T> verifier
    ) {
      T expected = setterToExpectedF.apply(builder);
      verifier.accept(builder, expected);
      return this;
    }

    private BuilderAssertion assertBuilt() {
      return assertBuilt(AsyncRetryExecutor.class);
    }

    private BuilderAssertion assertBuilt(Class<? extends RetryExecutor> expectedImplementation) {
      RetryExecutor executor = builder.build();
      Assertions.assertThat(executor)
          .isExactlyInstanceOf(expectedImplementation);
      return this;
    }

    private BuilderAssertion verify(Consumer<RetryExecutorBuilder> verificationClosure) {
      verificationClosure.accept(builder);
      return this;
    }

  }


}
