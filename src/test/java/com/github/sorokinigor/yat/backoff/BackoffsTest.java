package com.github.sorokinigor.yat.backoff;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Igor Sorokin
 */
public class BackoffsTest {

  @Test
  public void no_backoff_should_always_return_zero() {
    Backoff backoff = Backoffs.noBackoff();
    long actual = backoff.calculateDelayNanos(1, 10_000L);
    assertThat(actual)
        .isEqualTo(0L);
  }

}
