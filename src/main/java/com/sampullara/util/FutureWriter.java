package com.sampullara.util;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class sit in front of a writer and doesn't flush until Done is called on it.  Until then it queues up
 * writes that may not even be completed yet.  They are flushed out in the order they are enqueued.
 * <p/>
 * User: sam
 * Date: May 6, 2010
 * Time: 2:44:42 PM
 */
public class FutureWriter extends Writer implements Iterable<byte[]> {

  private AppendableCallable last;
  private ConcurrentLinkedQueue<Future<Object>> ordered = new ConcurrentLinkedQueue<Future<Object>>();
  private static ExecutorService es = new ThreadPoolExecutor(10, 100, 60, TimeUnit.SECONDS,
          new ArrayBlockingQueue<Runnable>(10), new ThreadPoolExecutor.CallerRunsPolicy());
  private Writer writer;
  private boolean closed = false;
  public static final Charset UTF8 = Charset.forName("UTF-8");
  public static final byte[] EMPTY_BYTEARRAY = new byte[0];

  public FutureWriter(Writer writer) {
    this.writer = writer;
  }


  public Writer getWriter() {
    return writer;
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

  @Override
  public Iterator<byte[]> iterator() {
    return new Iterator<byte[]>() {
      Stack<Iterator> stack = new Stack<Iterator>();
      Iterator i = ordered.iterator();

      @Override
      public boolean hasNext() {
        boolean b = i.hasNext();
        if (!b) {
          if (stack.size() != 0) {
            i = stack.pop();
            return i.hasNext();
          }
        }
        return b;
      }

      @Override
      public byte[] next() {
        try {
          Object value = i.next();
          if (value instanceof Future) {
            Object o = ((Future) value).get();
            if (o instanceof FutureWriter) {
              stack.push(i);
              FutureWriter fw = (FutureWriter) o;
              i = fw.iterator();
              return (byte[]) i.next();
            } else if (o instanceof Future) {
              Object result = ((Future) o).get();
              if (result != null) {
                return result.toString().getBytes(UTF8);
              }
              return EMPTY_BYTEARRAY;
            } else {
              return (o.toString()).getBytes(UTF8);
            }
          }
          return (byte[]) value;
        } catch (Exception e) {
          throw new AssertionError("Failed to iterate over FutureWriter: " + e);
        }
      }

      @Override
      public void remove() {
        i.remove();
      }
    };
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
    try {
      for (Future<Object> work : ordered) {
        Object o = work.get();
        if (o instanceof FutureWriter) {
          FutureWriter fw = (FutureWriter) o;
          fw.flush();
        } else if (o instanceof Future) {
          Object result = ((Future) o).get();
          if (result != null) {
            writer.write(result.toString());
          }
        } else {
          writer.write(o.toString());
        }
        total--;
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
