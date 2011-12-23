package com.sampullara.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A reasonably configured Executor for Mustache
 */
public class MustacheExecutor extends ThreadPoolExecutor {
  public MustacheExecutor() {
    super(10, 100, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(10), new ThreadPoolExecutor.CallerRunsPolicy());
    setThreadFactory(new ThreadFactory() {
      int i = 0;

      @Override
      public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName("Mustache-FutureWriter-" + i++);
        return thread;
      }
    });

  }
}
