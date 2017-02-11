package com.github.sorokinigor.yat.backoff;

import org.assertj.core.data.Percentage;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

/**
 * @author Igor Sorokin
 */
public class ExponentialBackoffTest {

  private final long min = 1L;
  private long max = 5L;
  private final TimeUnit unit = TimeUnit.SECONDS;
  private final double randomFactor = 0.2D;
  private final Backoff backoff = Backoffs.exponential(min, max, unit, randomFactor);
  private final Percentage defaultPercentage = withPercentage(randomFactor * 100.0D);

  @Test(dataProvider = "data")
  public void it_should_subtract_execution_duration_from_delay(int attempt, long expectedNanos, Percentage percentage) {
    long actual = backoff.calculateDelayNanos(attempt, 10_000L);
    assertThat(actual)
        .isCloseTo(expectedNanos, percentage);
  }

  @DataProvider(name = "data")
  public Iterator<Object[]> data() {
    Collection<Object[]> data = new ArrayList<>();

    {
      int attempt = ExponentialBackoff.MAX_ATTEMPT + 1;
      long expectedNanos = unit.toNanos(max);
      Percentage percentage = withPercentage(0.0D);
      data.add(new Object[]{ attempt, expectedNanos, percentage });
    }

    {
      int attempt = 2;
      long expectedNanos = (long) (unit.toNanos(min) * Math.pow(2, attempt));
      data.add(new Object[]{ attempt, expectedNanos, defaultPercentage });
    }

    {
      int attempt = 10;
      long expectedNanos = unit.toNanos(max);
      data.add(new Object[]{ attempt, expectedNanos, defaultPercentage });
    }

    return data.iterator();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void min_nanos_should_not_be_less_than_zero() {
    Backoffs.exponential(-1L, max, unit, randomFactor);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void min_nanos_should_not_be_greater_than_max_nanos() {
    Backoffs.exponential(max + 1L, max, unit, randomFactor);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void random_factor_should_not_be_greater_than_one() {
    Backoffs.exponential(min, max, unit, 1.1D);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void random_factor_should_not_be_less_than_zero() {
    Backoffs.exponential(min, max, unit, -0.1D);
  }

}
