package com.github.sorokinigor.yat;

import com.github.sorokinigor.yat.executor.AsyncRetryExecutorBuilder;
import com.github.sorokinigor.yat.executor.StatisticsExecutorService;
import com.github.sorokinigor.yat.executor.SyncRetryExecutorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Igor Sorokin
 */
public final class Retry {

  private Retry() { throw new IllegalStateException("Not expected to be instantiated"); }

  public static AsyncRetryExecutorBuilder async(ScheduledExecutorService executorService) {
    return new AsyncRetryExecutorBuilder()
        .executorService(executorService);
  }

  public static AsyncRetryExecutor async() {
    return DefaultAsyncExecutor.INSTANCE;
  }

  public static StatisticsExecutorService gatherStatisticFor(AsyncRetryExecutor executor) {
    return new StatisticsExecutorService(executor);
  }

  public static SyncRetryExecutorBuilder sync() {
    return new SyncRetryExecutorBuilder();
  }

  private static final class DefaultAsyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAsyncExecutor.class);

    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(
        Runtime.getRuntime().availableProcessors(),
        new NamingThreadFactory(Executors.defaultThreadFactory(), "default-async-retry-executor-")
    );
    private static final AsyncRetryExecutor INSTANCE = Retry
        .async(EXECUTOR_SERVICE)
        .doNotShutdownExecutors()
        .build();

    static {
      logger.debug("Registering shutdown hook for 'default-async-retry-executor'.");

      Thread hook = new Thread(() -> {
        List<Runnable> remainingTasks = EXECUTOR_SERVICE.shutdownNow();
        logger.info("'default-async-retry-executor' is shutdown. '{}' tasks have never commenced execution.",
            remainingTasks.size());
      });
      hook.setName("default-async-retry-executor-shutdown-hook");
      Runtime.getRuntime()
          .addShutdownHook(hook);
    }

  }

  private static final class NamingThreadFactory implements ThreadFactory {

    private final ThreadFactory delegate;
    private final AtomicLong count = new AtomicLong();
    private final String prefix;

    private NamingThreadFactory(ThreadFactory delegate, String prefix) {
      this.delegate = Objects.requireNonNull(delegate, "'delegate' should not be 'null'.");
      this.prefix = Objects.requireNonNull(prefix, "'prefix' should not be 'null'.");
    }

    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = delegate.newThread(runnable);
      thread.setName(prefix + count.getAndIncrement());
      return thread;
    }

  }

}
