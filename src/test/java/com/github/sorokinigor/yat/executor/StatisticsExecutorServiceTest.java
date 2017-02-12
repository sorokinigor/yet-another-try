package com.github.sorokinigor.yat.executor;

import com.github.sorokinigor.yat.AsyncRetryExecutor;
import com.github.sorokinigor.yat.Retry;
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

import static com.github.sorokinigor.yat.backoff.Backoffs.fixedDelay;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Igor Sorokin
 */
public class StatisticsExecutorServiceTest extends AsyncRetryExecutorTestKit {

  @Test(dataProvider = "tasksData")
  public void tasksDataDrivenTest(List<Callable<Object>> tasks, StatisticsExecutorService.Stats expected)
      throws InterruptedException {
    StatisticsExecutorService executor = create();
    try (StatisticsExecutorService ignored = executor) {
      executor.invokeAll(tasks);
    }
    StatisticsExecutorService.Stats actual = executor.stats();
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

    StatisticsExecutorService.Stats stats = new StatisticsExecutorService.Stats(
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
    AsyncRetryExecutor executor = create(executorService);
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
    AsyncRetryExecutor executor = create(executorService);
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
    AsyncRetryExecutor executor = create(executorService);
    executor.submit(infiniteLoopCallable());

    executor.shutdown();
    assertShutdown(executor, executorService);
  }

  @Override
  protected StatisticsExecutorService create() {
    return create(createExecutorService());
  }

  private StatisticsExecutorService create(ScheduledExecutorService executorService) {
    AsyncRetryExecutor retryExecutor = Retry
        .async(executorService)
        .maxAttempts(MAX_ATTEMPTS)
        .backOff(fixedDelay(10, TimeUnit.MILLISECONDS))
        .build();
    return Retry.gatherStatisticFor(retryExecutor);
  }

}
