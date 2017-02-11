package com.github.sorokinigor.yat;

import com.github.sorokinigor.yat.backoff.Backoffs;
import org.testng.annotations.Test;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Igor Sorokin
 */
public class PolicyTest {

  @Test(expectedExceptions = NullPointerException.class)
  public void when_exception_filter_is_null_it_should_fail() {
    new Policy(null, Backoffs.noBackoff(), 1L, 1, true);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void when_backoff_is_null_it_should_fail() {
    new Policy(e -> true, null, 1L, 1, true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void when_max_attempts_less_than_one_it_should_fail() {
    new Policy(e -> true, Backoffs.noBackoff(), 0L, 0, true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void when_first_delay_nanos_less_than_zero_it_should_fail() {
    new Policy(e -> true, Backoffs.noBackoff(), -1L, 1, true);
  }

  @Test
  public void when_exception_is_not_filtered_it_should_request_one_more_attempt() {
    Exception retryableException = new RuntimeException();
    Exception nonRetryableException = new IllegalArgumentException();
    Predicate<Exception> exceptionFilter = exception -> retryableException.getClass() == exception.getClass();
    Policy policy = new Policy(exceptionFilter, Backoffs.noBackoff(), 0L, 1, true);

    assertThat(policy.shouldRetry(retryableException))
        .isTrue();
    assertThat(policy.shouldRetry(nonRetryableException))
        .isFalse();
  }

}
