package com.github.sorokinigor.yat.backoff;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Igor Sorokin
 */
public class FixedRateBackoffTest {

  private final TimeUnit unit = TimeUnit.SECONDS;

  @Test(dataProvider = "data")
  public void it_should_subtract_execution_duration_from_delay(long delay, long executionNanos, long expectedDelay) {
    Backoff backoff = Backoffs.fixedRate(delay, unit);

    long actual = backoff.calculateDelayNanos(1, executionNanos);
    assertThat(actual)
        .isEqualTo(expectedDelay);
  }

  @DataProvider(name = "data")
  public Iterator<Object[]> data() {
    Collection<Object[]> data = new ArrayList<>();

    {
      long delay = 10L;
      long executionNanos = unit.toNanos(5L);
      long expectedDelay = unit.toNanos(delay) - executionNanos;
      data.add(new Object[]{ delay, executionNanos, expectedDelay });
    }

    {
      long delay = 10L;
      long executionNanos = unit.toNanos(delay + 1L);
      long expectedDelay = 0L;
      data.add(new Object[]{ delay, executionNanos, expectedDelay });
    }

    return data.iterator();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void rate_should_not_be_less_than_zero() {
    Backoffs.fixedRate(-1L, TimeUnit.SECONDS);
  }

}
