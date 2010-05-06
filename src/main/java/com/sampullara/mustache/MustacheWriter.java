package com.sampullara.mustache;

import java.io.IOException;
import java.io.Writer;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: May 6, 2010
 * Time: 2:44:42 PM
 */
public class MustacheWriter extends Writer {

  private Queue<Future<Object>> ordered = new ConcurrentLinkedQueue<Future<Object>>();
  private static ExecutorService es = Executors.newCachedThreadPool();
  private Writer writer;

  public MustacheWriter(Writer writer) {
    this.writer = writer;
  }

  public void enqueue(final Mustache m, final Scope s) {
    ordered.add(es.submit(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        MustacheWriter w = new MustacheWriter(writer);
        m.execute(w, s);
        return w;
      }
    }));
  }

  public void enqueue(Callable<Object> callable) {
    ordered.add(es.submit(callable));
  }

  public void done() throws ExecutionException, InterruptedException, IOException {
    for (Future<Object> work : ordered) {
      Object o = work.get();
      if (o instanceof MustacheWriter) {
        MustacheWriter mw = (MustacheWriter) o;
        mw.done();
      } else {
        writer.write(o.toString());
      }
    }
  }

  @Override
  public void write(final int i) throws IOException {
    enqueue(new Callable<Object>() {
      public String call() throws Exception {
        return String.valueOf((char) i);
      }
    });
  }

  @Override
  public void write(final char[] chars, final int i, final int i1) throws IOException {
    enqueue(new Callable<Object>() {
      public String call() throws Exception {
        return new String(chars, i, i1);
      }
    });
  }

  @Override
  public void write(final String s, final int i, final int i1) throws IOException {
    enqueue(new Callable<Object>() {
      public String call() throws Exception {
        return s.substring(i, i1);
      }
    });
  }

  @Override
  public void flush() throws IOException {
    try {
      done();
    } catch (Exception e) {
      throw new IOException("Failed to flush", e);
    }
    writer.flush();
  }

  @Override
  public void close() throws IOException {
    flush();
    writer.close();
  }

  @Override
  public void write(final char[] chars) throws IOException {
    enqueue(new Callable<Object>() {
      public String call() throws Exception {
        return new String(chars);
      }
    });
  }

  @Override
  public void write(final String s) throws IOException {
    enqueue(new Callable<Object>() {
      public String call() throws Exception {
        return s;
      }
    });
  }

  @Override
  public Writer append(final CharSequence charSequence) throws IOException {
    enqueue(new Callable<Object>() {
      public String call() throws Exception {
        return charSequence.toString();
      }
    });
    return this;
  }

  @Override
  public Writer append(final CharSequence charSequence, final int i, final int i1) throws IOException {
    enqueue(new Callable<Object>() {
      public String call() throws Exception {
        return charSequence.subSequence(i, i1).toString();
      }
    });
    return this;
  }

  @Override
  public Writer append(final char c) throws IOException {
    enqueue(new Callable<Object>() {
      public String call() throws Exception {
        return String.valueOf(c);
      }
    });
    return this;
  }
}
