package com.sampullara.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
  private static ExecutorService es = new ThreadPoolExecutor(10, 100, 60, TimeUnit.SECONDS,
          new ArrayBlockingQueue<Runnable>(10), new ThreadPoolExecutor.CallerRunsPolicy());
  private Writer writer;
  private boolean closed = false;

  public static void setExecutorService(ExecutorService es) {
    ExecutorService old = FutureWriter.es;
    // Switch to the new one
    FutureWriter.es = es;
    // Gracefully shutdown the old one
    old.shutdown();
  }

  public FutureWriter() {    
  }

  public FutureWriter(Writer writer) {
    this.writer = writer;
  }

  public Writer getWriter() {
    return writer;
  }

  public void setWriter(Writer writer) {
    this.writer = writer;
  }

  int total = 0;

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
    if (last != null) {
      last.append(cs);
    } else {
      AppendableCallable call = new AppendableCallable(cs);
      enqueue(es.submit(call));
      last = call;
    }
  }

  public void enqueue(Callable<Object> callable) throws IOException {
    Future<Object> future = es.submit(callable);
    enqueue(future);
  }

  public void enqueue(Future<Object> future) throws IOException {
    if (closed) {
      throw new IOException("closed");
    }
    last = null;
    total++;
    ordered.add(future);
  }

  private static class AppendableCallable implements Appendable, Callable<Object> {
    StringBuffer sb;

    AppendableCallable(CharSequence cs) {
      sb = new StringBuffer(cs);
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
  }


  @Override
  public void flush() throws IOException {
    flush(true);
  }

  public void flush(boolean top) throws IOException {
    try {
      for (Future<Object> work : ordered) {
        Object o;
        try {
          o = work.get(50, TimeUnit.MILLISECONDS);
        } catch(TimeoutException te) {
          System.out.println("1 Flushed: " + System.currentTimeMillis());
          writer.flush();
          o = work.get();
        }
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
        total--;
      }
      if (top) {
        if (!(writer instanceof StringWriter)) {
          System.out.println("2 Flushed: " + System.currentTimeMillis());
        }
        writer.flush();
      }
      if (total != 0) {
        throw new AssertionError("Enqueued work != executed work: " + total);
      }
    } catch (Exception e) {
      throw new IOException("Failed to flush", e);
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
    enqueue(new String(chars, i, i1));
  }

  @Override
  public void write(final String s, final int i, final int i1) throws IOException {
    enqueue(s.substring(i, i1));
  }

  @Override
  public void write(final char[] chars) throws IOException {
    enqueue(new String(chars));
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
