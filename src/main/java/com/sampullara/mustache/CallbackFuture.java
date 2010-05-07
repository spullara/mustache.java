package com.sampullara.mustache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A callback future lets you set the value on the future rather than waiting in a thread for it to return.
 * Useful for making synchronous processes wait for truly asynchronous processes to complete.
 * <p/>
 * User: sam
 * Date: May 7, 2010
 * Time: 12:33:29 PM
 */
public class CallbackFuture<T> implements Future<T> {
  private T o;
  private boolean cancelled;
  private boolean isDone;
  private Throwable error;

  /**
   * Set the value to return to the waiter. Ignore if done.
   * @param o
   */
  public synchronized void set(T o) {
    if (!isDone) {
      isDone = true;
      this.o = o;
      notify();
    }
  }

  /**
   * Set an error to be thrown back to the waiter. Ignore if done.
   * @param error
   */
  public synchronized void error(Throwable error) {
    if (!isDone) {
      isDone = true;
      this.error = error;
      notify();
    }
  }

  @Override
  public boolean cancel(boolean interrupt) {
    synchronized (this) {
      if (cancelled) return true;
      if (!isDone) {
        cancelled = true;
        isDone = true;
        notify();
        return true;
      }
      return false;
    }
  }

  @Override
  public synchronized boolean isCancelled() {
    return cancelled;
  }

  @Override
  public synchronized boolean isDone() {
    return isDone;
  }

  @Override
  public synchronized T get() throws InterruptedException, ExecutionException {
    while (!isDone) {
      wait();
    }
    if (cancelled) {
      throw new InterruptedException("Cancelled");
    }
    if (error != null) {
      throw new ExecutionException(error);
    }
    return o;
  }

  @Override
  public T get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
    long total = timeUnit.convert(l, TimeUnit.MILLISECONDS);
    long start = System.currentTimeMillis();
    synchronized (this) {
      while (!isDone && total > 0) {
        wait(total);
        total -= (System.currentTimeMillis() - start);
      }
      if (cancelled) {
        throw new InterruptedException("Cancelled");
      }
      if (error != null) {
        throw new ExecutionException(error);
      }
      if (total <= 0) {
        throw new TimeoutException("Timed out after waiting: " + (System.currentTimeMillis() - start) + "ms");
      }
      return o;
    }
  }
}
