package com.github.sorokinigor.yat.executor;

/**
 * INTERNAL API
 *
 * @author Igor Sorokin
 */
final class ExceptionUtils {

  private ExceptionUtils() { throw new IllegalStateException("Not expected to be instantiated"); }

  static Exception addSuppressed(Exception exception, Exception suppressed) {
    if (suppressed != null) {
      exception.addSuppressed(suppressed);
    }
    return exception;
  }

}
