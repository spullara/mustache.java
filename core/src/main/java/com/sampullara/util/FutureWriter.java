package com.sampullara.util;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheTrace;

/**
 * This class sit in front of a writer and doesn't flush until Done is called on it.  Until then it queues up
 * writes that may not even be completed yet.  They are flushed out in the order they are enqueued.
 * <p/>
 * User: sam
 * Date: May 6, 2010
 * Time: 2:44:42 PM
 */
public class FutureWriter extends Writer {

  private AppendableCallable last;
  private ConcurrentLinkedQueue<Future<Object>> ordered = new ConcurrentLinkedQueue<Future<Object>>();
  private static ExecutorService des;
  private ExecutorService es;

  private Writer writer;
  private boolean closed = false;

  public static void setParallel() {
    setParallel(new MustacheExecutor());
  }

  public static void setParallel(ExecutorService es) {
    ExecutorService old = FutureWriter.des;
    // Switch to the new one
    FutureWriter.des = es;
    // Gracefully shutdown the old one
    if (old != null) old.shutdown();
  }

  public FutureWriter() {
    es = des;
  }

  public FutureWriter(Writer writer) {
    this();
    this.writer = writer;
  }

  public Writer getWriter() {
    return writer;
  }

  public void setWriter(Writer writer) {
    this.writer = writer;
  }

  public static void shutdown() {
    if (des != null) {
      des.shutdownNow();
    }
  }

  /**
   * Optimize for the degenerate case of a set of strings being appended to the writer.
   *
   * @param cs
   * @throws IOException
   */
  public void enqueue(CharSequence cs) throws IOException {
    if (closed) {
      throw new IOException("closed");
    }
    if (isParallel()) {
      if (last != null) {
        last.append(cs);
      } else {
        AppendableCallable call = new AppendableCallable(cs);
        enqueue(call);
        last = call;
      }
    } else {
      if (cs != null) {
        writer.write(cs.toString());
      }
    }
  }

  public void enqueue(Callable<Object> callable) throws IOException {
    if (isParallel()) {
      Future<Object> future;
      if (es == null) {
        future = new ImmediateFuture<Object>(callable);
      } else {
        future = es.submit(callable);
      }
      enqueue(future);
    } else {
      try {
        write(callable.call());
      } catch (Exception e) {
        throw new IOException("Failed to call: " + callable, e);
      }
    }
  }

  public void enqueue(Future<Object> future) throws IOException {    
    if (closed) {
      throw new IOException("closed");
    }
    if (isParallel()) {
      last = null;
      ordered.add(future);
    } else {
      try {
        write(future);
      } catch (Exception e) {
        throw new IOException("Failed to execute: " + future, e);
      }
    }
  }

  public boolean isParallel() {
    return es != null;
  }

  private static class AppendableCallable implements Appendable, Callable<Object> {
    StringBuilder sb;

    AppendableCallable(CharSequence cs) {
      sb = new StringBuilder(cs);
    }

    public AppendableCallable(char[] chars) {
      sb = new StringBuilder().append(chars);
    }

    public AppendableCallable(char[] chars, int offset, int len) {
      sb = new StringBuilder().append(chars, offset, len);
    }

    @Override
    public Appendable append(CharSequence cs) {
      sb.append(cs);
      return this;
    }

    @Override
    public Appendable append(CharSequence charSequence, int i, int i1) throws IOException {
      sb.append(charSequence, i, i1);
      return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
      sb.append(String.valueOf(c));
      return this;
    }

    @Override
    public Object call() throws Exception {
      return sb;
    }

    public Appendable append(char[] chars) {
      sb.append(chars);
      return sb;
    }

    public Appendable append(char[] chars, int offset, int len) {
      sb.append(chars, offset, len);
      return sb;
    }
  }


  @Override
  public void flush() throws IOException {
    flush(true);
  }

  public void flush(boolean top) throws IOException {
    try {
      if (isParallel()) {
        for (Future<Object> work : ordered) {
          Object o;
          try {
            o = work.get(50, TimeUnit.MILLISECONDS);
          } catch (TimeoutException te) {
            MustacheTrace.Event flushEvent = null;
            if (top && Mustache.trace) {
              flushEvent = MustacheTrace.addEvent("flush_wait", "FutureWriter");
            }
            writer.flush();
            o = work.get();
            if (flushEvent != null) {
              flushEvent.start -= 50;
              flushEvent.end();
            }
          }
          write(o);
        }
      }
      if (top) {
        writer.flush();
      }
      // Reset
      ordered.clear();
      last = null;
    } catch (Exception e) {
      throw new IOException("Failed to flush", e);
    }
  }

  private void write(Object o) throws IOException, InterruptedException, ExecutionException {
    if (o instanceof FutureWriter) {
      FutureWriter fw = (FutureWriter) o;
      fw.setWriter(writer);
      fw.flush(false);
    } else if (o instanceof Future) {
      Future future = (Future) o;
      Object result = future.get();
      if (result != null) {
        writer.write(result.toString());
      }
    } else {
      if (o != null) {
        writer.write(o.toString());
      }
    }
  }

  @Override
  public void close() throws IOException {
    flush();
    writer.close();
    closed = true;
  }

  @Override
  public void write(final int i) throws IOException {
    enqueue(String.valueOf((char) i));
  }

  @Override
  public void write(final char[] chars, final int i, final int i1) throws IOException {
    if (closed) {
      throw new IOException("closed");
    }
    if (isParallel()) {
      if (last != null) {
        last.append(chars, i, i1);
      } else {
        AppendableCallable call = new AppendableCallable(chars, i, i1);
        enqueue(call);
        last = call;
      }     
    } else {
      writer.write(chars, i, i1);
    }
  }

  @Override
  public void write(final String s, final int i, final int i1) throws IOException {
    enqueue(s.substring(i, i1));
  }

  @Override
  public void write(final char[] chars) throws IOException {
    if (closed) {
      throw new IOException("closed");
    }
    if (last != null) {
      last.append(chars);
    } else {
      AppendableCallable call = new AppendableCallable(chars);
      enqueue(call);
      last = call;
    }
  }

  @Override
  public void write(final String s) throws IOException {
    enqueue(s);
  }

  @Override
  public Writer append(final CharSequence charSequence) throws IOException {
    enqueue(charSequence);
    return this;
  }

  @Override
  public Writer append(final CharSequence charSequence, final int i, final int i1) throws IOException {
    enqueue(charSequence.subSequence(i, i1));
    return this;
  }

  @Override
  public Writer append(final char c) throws IOException {
    enqueue(String.valueOf(c));
    return this;
  }
}
