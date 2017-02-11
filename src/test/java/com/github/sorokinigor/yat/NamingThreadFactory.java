package com.github.sorokinigor.yat;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Yet another thread factory. Just to avoid external dependencies.
 *
 * @author Igor Sorokin
 */
final class NamingThreadFactory implements ThreadFactory {

  private final ThreadFactory delegate;
  private final AtomicLong count = new AtomicLong();
  private final String prefix;

  NamingThreadFactory(ThreadFactory delegate, String prefix) {
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
