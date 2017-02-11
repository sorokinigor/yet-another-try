package com.github.sorokinigor.yat.backoff;

/**
 * @author Igor Sorokin
 */
public interface Backoff {

  long calculateDelayNanos(int attempt, long executionDurationNanos);

}
