package com.github.sorokinigor.yat;

import com.github.sorokinigor.yat.executor.AsyncRetryExecutorTestKit;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Igor Sorokin
 */
public class RetryTest extends AsyncRetryExecutorTestKit {

  @Override
  protected AsyncRetryExecutor create() {
    return Retry.async();
  }

  @Test
  public void it_should_not_shutdown_default_executor() {
    AsyncRetryExecutor executor = create();
    executor.shutdown();

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

    assertThat(executor.isShutdown())
        .isFalse();
    assertThat(executor.isTerminated())
        .isFalse();
  }

  @Test
  public void it_should_always_return_the_same_instance_of_the_default_async_executor() {
    assertThat(Retry.async())
        .isSameAs(Retry.async());
  }

}
