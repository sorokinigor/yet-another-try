package com.github.sorokinigor.yat.backoff;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static com.github.sorokinigor.yat.backoff.Backoffs.fixedDelay;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Igor Sorokin
 */
public class FixedDelayBackoffTest {

  @Test(dataProvider = "data")
  public void it_should_always_return_the_same_value(long amount, TimeUnit unit, int numberOfAttempts) {
    Backoff backoff = fixedDelay(amount, unit);

    long expected = unit.toNanos(amount);
    long executionNanos = 10_000L;
    for (int attempt = 0; attempt < numberOfAttempts; attempt++) {
      long actual = backoff.calculateDelayNanos(numberOfAttempts, executionNanos);
      assertThat(actual)
          .isEqualTo(expected);
    }
  }

  @DataProvider(name = "data")
  public Iterator<Object[]> data() {
    long delay = 100L;
    int numberOfAttempts = 10;
    return Arrays.stream(TimeUnit.values())
        .map(delayUnit -> new Object[]{ delay, delayUnit, numberOfAttempts })
        .iterator();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void delay_should_not_be_less_than_zero() {
    fixedDelay(-1L, TimeUnit.SECONDS);
  }

}
