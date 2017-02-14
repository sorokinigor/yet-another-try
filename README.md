# Yet Another Try
[![Build Status](https://travis-ci.org/sorokinigor/yet-another-try.svg?branch=master)](https://travis-ci.org/sorokinigor/yet-another-try)
[![codecov](https://codecov.io/gh/sorokinigor/yet-another-try/branch/master/graph/badge.svg)](https://codecov.io/gh/sorokinigor/yet-another-try)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.sorokinigor/yet-another-try.svg?label=Maven%20Central)](http://search.maven.org/#artifactdetails%7Ccom.github.sorokinigor%7Cyet-another-try%7C1.0.0%7Cjar)

* [Features](#features)
* [Dependencies and prerequisites](#dependencies-and-prerequisites)
* [Usage](#usage)
   * [Asynchronous](#asynchronous)
     * [Timeout](#timeout)
     * [Delay](#delay)
        * [Exponential](#exponential)
        * [Fixed delay](#fixed-delay)
        * [Fixed rate](#fixed-rate)
     * [Exceptions](#exceptions)
     * [Default executor](#default-executor)
     * [Statistics](#statistics)
   * [Synchronous](#synchronous)

# Features
* Configure: 
  * max number of attempts
  * timeout
  * delay between attempts
  * which exceptions should be retried and which should not.
  * whenever or not use the invocation thread for the first attempt
* Implements plain java
[ExecutorService](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html), 
therefore it is fully compatible with the code, which uses 
[ExecutorService](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html) directly.
* Uses [CompletableFuture](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)
as a return type.
* Has both [asynchronous](#asynchronous) and [synchronous](#synchronous) versions.
* Collects statistics about successful and failed attempts if 
[requested](#statistics). 

#Dependencies and prerequisites
The library requires **Java 8+**. Use the following code snippets to add the library to your project:
* Gradle
```Groovy
dependencies {
    compile "com.github.sorokinigor:yet-another-try:1.0.0"
}

repositories {
    mavenCentral()
}
```
* Maven
```xml
<dependency>
  <groupId>com.github.sorokinigor</groupId>
  <artifactId>yet-another-try</artifactId>
  <version>1.0.0</version>
</dependency>
```

# Usage
The main entry point is 
[Retry](https://static.javadoc.io/com.github.sorokinigor/yet-another-try/1.0.0/com/github/sorokinigor/yat/Retry.html) utility class.
```java
/*
  Uses the current thread for the first attempt
  and passed ScheduledExecutorService for the subsequent attempts,
  does not retry on malformed request.
*/
AsyncRetryExecutor executor = Retry.async(Executors.newSingleThreadScheduledExecutor())
    .retryOnce()
    .runFirstAttemptInInvocationThread()
    .terminateOn(IllegalArgumentException.class)
    .terminateOn(HttpGenericException.class, e -> e.statusCode() == 400)
    .build();
CompletableFuture<String> future = executor.submit(() -> faultyResource("malformedRequest"));
future.whenComplete((response, exception) -> System.out.println(
    "Response '" + response + "', exception '" + exception + "'."
));

//Uses default lazy singleton instance of AsyncRetryExecutor
Retry.async()
    .submit(() -> faultyResource("request"))
    .thenAccept(response -> System.out.println("Response is '" + response + "'."));

/*
  Uses the current thread for task invocation.
  Tries 2 times with fixed rate between attempts.
*/
SyncRetryExecutor syncExecutor = Retry.sync()
    .maxAttempts(2)
    .backOff(Backoffs.fixedRate(1L, TimeUnit.SECONDS))
    .build();
String response = syncExecutor.execute(() -> faultyResource("syncRequest"));
    
/*
 Shortcut for ad hoc synchronous execution.
 Completes with exception on timeout.
 */
String result = Retry.sync()
    .timeout(5L, TimeUnit.SECONDS)
    .execute(() -> faultyResource("adhoc request"));
```

## Asynchronous
Any arbitrary [ScheduledExecutorService](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ScheduledExecutorService.html)
should be passed in order to use asynchronous executor. Example:
```java
AsyncRetryExecutor executor = Retry.async(Executors.newSingleThreadScheduledExecutor())
    .maxAttempts(3)
    .timeout(10, TimeUnit.SECONDS)
    .backOff(Backoffs.fixedDelay(1L, TimeUnit.SECONDS))
    .retryOn(NotYetConnectedException.class)
    .terminateOn(NullPointerException.class)
    .build();

CompletableFuture<Integer> result = executor.submit(() -> {
      try (SocketChannel socket = SocketChannel.open(new InetSocketAddress("music.yandex.ru", 80))) {
        socket.configureBlocking(false);
        ByteBuffer buffer = ByteBuffer.allocate(10);
        return socket.read(buffer);
      }
    })
    .thenApply(numberOfBytesRead -> numberOfBytesRead / 2);
```
Please note that by default 
[AsyncRetryExecutor](https://static.javadoc.io/com.github.sorokinigor/yet-another-try/1.0.0/com/github/sorokinigor/yat/AsyncRetryExecutor.html)
manages the lifecycle of the passed 
[ScheduledExecutorService](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ScheduledExecutorService.html).
Consequently, the 
[AsyncRetryExecutor](https://static.javadoc.io/com.github.sorokinigor/yet-another-try/1.0.0/com/github/sorokinigor/yat/AsyncRetryExecutor.html) will shutdown underlying 
[ScheduledExecutorService](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ScheduledExecutorService.html).
If you want to prevent it, use:
```java
ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
AsyncRetryExecutor executor = Retry.async(executorService)
    .doNotShutdownExecutors()
    .build();
```  

### Timeout
This code snippet shows how you can specify a timeout for a task:
```java
AsyncRetryExecutor executor = Retry.async(Executors.newSingleThreadScheduledExecutor())
    .timeout(10L, TimeUnit.SECONDS)
    .build();
``` 
After the timeout is expired, the result
[CompletableFuture](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) is completed 
with 
[TimeoutException](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeoutException.html).
Since then, there would be no retries of the task.

By default, the same executor is used for both task execution and timeout handling, but it is configurable:
```java
ScheduledExecutorService taskExecutor = Executors.newSingleThreadScheduledExecutor();
ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
AsyncRetryExecutor executor = Retry.async(taskExecutor)
    .timeout(5L, TimeUnit.SECONDS)
    .timeoutExecutorService(timeoutExecutor)
    .build();
```   

### Delay
The library itself contains exponential **(default and preferable)**, fixed delay and fixed rate backoffs 
for delay calculation. But, feel free to implement your own backoff strategy, as the 
[Backoff](https://static.javadoc.io/com.github.sorokinigor/yet-another-try/1.0.0/com/github/sorokinigor/yat/backoff/Backoff.html) 
interface is a part of the public API.

In order to instantiate the built-in backoff strategies use 
[Backoffs](https://static.javadoc.io/com.github.sorokinigor/yet-another-try/1.0.0/com/github/sorokinigor/yat/backoff/Backoffs.html)
utility class. 
#### Exponential
The [delay](https://static.javadoc.io/com.github.sorokinigor/yet-another-try/1.0.0/com/github/sorokinigor/yat/backoff/Backoffs.html#exponential-long-long-java.util.concurrent.TimeUnit-double-) 
is exponentially increases until it reaches the upper bound for the delay or the number of attempts.
After the calculation of the exponential backoff, it also adds an additional random delay based on the passed random 
factor. For instance, `0.2` adds up to `20%` delay. Example:
```java
AsyncRetryExecutor executor = Retry.async(Executors.newSingleThreadScheduledExecutor())
    .backOff(Backoffs.exponential(3L, 30L, TimeUnit.SECONDS, 0.2D))
    .build();
```

#### Fixed delay
[It](https://static.javadoc.io/com.github.sorokinigor/yet-another-try/1.0.0/com/github/sorokinigor/yat/backoff/Backoffs.html#fixedDelay-long-java.util.concurrent.TimeUnit-)
always uses the same delay for each attempt. Example:
```java
AsyncRetryExecutor executor = Retry.async(Executors.newSingleThreadScheduledExecutor())
    .backOff(Backoffs.fixedDelay(1L, TimeUnit.SECONDS))
    .build();
```
#### Fixed rate
[It](https://static.javadoc.io/com.github.sorokinigor/yet-another-try/1.0.0/com/github/sorokinigor/yat/backoff/Backoffs.html#fixedRate-long-java.util.concurrent.TimeUnit-)
subtracts the task execution time from the delay. If the execution time is greater than or equal the delay, the delay is 0. Example:
```java
AsyncRetryExecutor executor = Retry.async(Executors.newSingleThreadScheduledExecutor())
    .backOff(Backoffs.fixedRate(1L, TimeUnit.SECONDS))
    .build();
```

### Exceptions
The library provides the ability to retry only specific type exception and the exception matching the predicate.
Also, it is possible to configure to stop retrying after a specific exception (by type or predicate too).
Example:
```java
AsyncRetryExecutor executor = Retry.async(Executors.newSingleThreadScheduledExecutor())
     .retryOn(SocketException.class)
     .retryOn(HttpGenericException.class, e -> e.statusCode() == 500)
     .terminateOn(IllegalStateException.class)
     .terminateOn(HttpGenericException.class, e -> e.statusCode() == 400)
     .terminatePredicate(e -> e instanceof BindException && e.getMessage().contains("in use"))
     .build();
```
Notice that the task is retried only if: 
* all of the retry predicates returns `true` or you didn't specify any (in that case there is a default predicate,
which always returns `true`).
* all of the terminate predicates returns `false` or you didn't specify any (in that case there is a default predicate,
which always returns `false`).

### Default executor
A default lazy singleton instance of [asynchronous](#asynchronous) executor is available via
[Retry.async()](https://static.javadoc.io/com.github.sorokinigor/yet-another-try/1.0.0/com/github/sorokinigor/yat/Retry.html#async--) 
method. Example:
```java
CompletableFuture<String> future = Retry.async()
    .submit(() -> faultyResource("request"));
```
It is lazily instantiated on first usage and creates a shutdown hook for the internal 
[ScheduledExecutorService](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ScheduledExecutorService.html)
shutting down.

### Statistics 
There is a simple [wrapper](https://static.javadoc.io/com.github.sorokinigor/yet-another-try/1.0.0/com/github/sorokinigor/yat/executor/StatisticsExecutorService.html)
for the [asynchronous](#asynchronous) executor, which collects the number of 
failed attempts and the number of successful and failed tasks. Example:
```java
AsyncRetryExecutor executor = Retry.async(Executors.newSingleThreadScheduledExecutor())
    .maxAttempts(2)
    .build();
StatisticsExecutorService statsExecutor = Retry.gatherStatisticFor(executor);
CompletableFuture<String> successful = statsExecutor.submit(() -> "successful");
CompletableFuture<String> failed = statsExecutor.submit(() -> { throw new Exception(); });
successful.thenAcceptBoth(failed, (ignored1, ignored2) -> {})
    .whenComplete((ignored, ignoredException) -> {
      System.out.println(statsExecutor.stats());
      //Stats{successful=1, failed=1, failedAttempts=2}
    });
```

## Synchronous  
The [synchronous executor](https://static.javadoc.io/com.github.sorokinigor/yet-another-try/1.0.0/com/github/sorokinigor/yat/SyncRetryExecutor.html)
does not use any thread pool, instead, it uses the current thread for task execution.
It has approximately the same configuration as [asynchronous](#asynchronous) one, except
the settings related to 
[ScheduledExecutorService](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ScheduledExecutorService.html).
Example:
```java
SyncRetryExecutor executor = Retry.sync()
    .maxAttempts(3)
    .timeout(10, TimeUnit.SECONDS)
    .backOff(Backoffs.fixedDelay(1L, TimeUnit.SECONDS))
    .retryOn(NotYetConnectedException.class)
    .terminateOn(NullPointerException.class)
    .build();

int numberOfBytesRead = executor.execute(() -> {
    try (SocketChannel socket = SocketChannel.open(new InetSocketAddress("music.yandex.ru", 80))) {
      socket.configureBlocking(false);
      ByteBuffer buffer = ByteBuffer.allocate(10);
      return socket.read(buffer);
    }
});
```
If you do not want to store a reference to the executor, you can use a shortcut:
```java
String response = Retry.sync()
    .withoutDelay()
    .terminateOn(IllegalArgumentException.class)
    .terminateOn(UnsupportedOperationException.class)
    .execute(() -> faultyResource("request"));
```


