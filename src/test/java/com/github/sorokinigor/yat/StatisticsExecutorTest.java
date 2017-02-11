package com.github.sorokinigor.yat;

import com.github.sorokinigor.yat.backoff.Backoffs;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Igor Sorokin
 */
public class StatisticsExecutorTest extends RetryExecutorTestKit {

  @Test(dataProvider = "tasksData")
  public void tasksDataDrivenTest(List<Callable<Object>> tasks, StatisticsExecutor.Stats expected)
      throws InterruptedException {
    StatisticsExecutor executor = create();
    try (StatisticsExecutor ignored = executor) {
      executor.invokeAll(tasks);
    }
    StatisticsExecutor.Stats actual = executor.stats();
    assertThat(actual)
        .isEqualTo(expected);
  }

  @DataProvider(name = "tasksData")
  public Iterator<Object[]> tasksData() {
    Collection<Object[]> data = new ArrayList<>();

    ThreadLocalRandom random = ThreadLocalRandom.current();
    IntStream.range(1, 10)
        .mapToObj(bound -> createData(random.nextInt(bound), random.nextInt(bound)))
        .forEach(data::add);
    data.add(createData(0, 0));

    return data.iterator();
  }

  private Object[] createData(int successfulCount, int failedCount) {
    List<Callable<?>> successful = Stream.generate(() -> successfulCallable("value"))
        .limit(successfulCount)
        .collect(Collectors.toList());
    List<Callable<?>> failed = Stream.generate(this::failedCallable)
        .limit(failedCount)
        .collect(Collectors.toList());
    List<Callable<?>> tasks = new ArrayList<>(successful.size() + failed.size());
    tasks.addAll(successful);
    tasks.addAll(failed);

    StatisticsExecutor.Stats stats = new StatisticsExecutor.Stats(
        successful.size(),
        failed.size(),
        failed.size() * MAX_ATTEMPTS
    );
    return new Object[]{ tasks, stats };
  }

  @Test
  public void when_terminated_it_should_terminate_internal_executor()
      throws InterruptedException {
    ScheduledExecutorService executorService = createExecutorService();
    RetryExecutor executor = create(executorService);
    executor.submit(successfulRunnable());

    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.SECONDS);
    assertShutdown(executor, executorService);
    assertTerminated(executor, executorService);
  }

  @Test
  public void when_shutdown_immediately_it_should_shutdown_internal_executor()
      throws InterruptedException {
    ScheduledExecutorService executorService = createExecutorService();
    RetryExecutor executor = create(executorService);
    executor.submit(infiniteLoopCallable());

    List<Runnable> neverExecutedTasks = executor.shutdownNow();
    assertThat(neverExecutedTasks)
        .size()
        .isBetween(0, 1);
    assertShutdown(executor, executorService);
  }

  @Test
  public void when_shutdown__it_should_shutdown_internal_executor()
      throws InterruptedException {
    ScheduledExecutorService executorService = createExecutorService();
    RetryExecutor executor = create(executorService);
    executor.submit(infiniteLoopCallable());

    executor.shutdown();
    assertShutdown(executor, executorService);
  }

  @Override
  protected StatisticsExecutor create() {
    return create(createExecutorService());
  }

  private StatisticsExecutor create(ScheduledExecutorService executorService) {
    RetryExecutor retryExecutor = Retry
        .async(executorService)
        .maxAttempts(MAX_ATTEMPTS)
        .backOff(Backoffs.fixedDelay(10, TimeUnit.MILLISECONDS))
        .build();
    return Retry.gatherStatisticFor(retryExecutor);
  }

}
