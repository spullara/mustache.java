package com.sampullara.util;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractFuture;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * No context-switch, lazy evaluating executor service.
 */
public class ImmediateExecutorService implements ExecutorService {
  private volatile boolean shutdown;

  @Override
  public void shutdown() {
    shutdown = true;
  }

  @Override
  public List<Runnable> shutdownNow() {
    return null;
  }

  @Override
  public boolean isShutdown() {
    return shutdown;
  }

  @Override
  public boolean isTerminated() {
    return shutdown;
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return false;
  }

  @Override
  public <T> Future<T> submit(final Callable<T> task) {
    return new ImmediateFuture<T>(task);
  }

  @Override
  public <T> Future<T> submit(final Runnable task, final T result) {
    return new ImmediateFuture<T>(new Callable<T>() {
      @Override
      public T call() throws Exception {
        task.run();
        return result;
      }
    });
  }

  @Override
  public Future<?> submit(final Runnable task) {
    return new ImmediateFuture<Void>(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        task.run();
        return null;
      }
    });
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return Lists.newArrayList(Iterables.transform(tasks, new Function<Callable<T>, Future<T>>() {
      @Override
      public Future<T> apply(Callable<T> input) {
        return new ImmediateFuture<T>(input);
      }
    }));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
    return invokeAll(tasks);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return new ImmediateFuture<T>(tasks.iterator().next()).get();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return invokeAny(tasks);
  }

  @Override
  public void execute(Runnable command) {
    command.run();
  }

  private class ImmediateFuture<T> extends AbstractFuture<T> {
    public ImmediateFuture(Callable<T> task) {
      try {
        set(task.call());
      } catch (Exception e) {
        setException(e);
      }
    }
  }
}
