package com.github.mustachejava.util;

import com.github.mustachejava.MustacheException;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

/**
 * Buffer content while a future is being evaluated in another thread.
 */
public class LatchedWriter extends AbstractIndentWriter {

  // Write to the buffer while latched, when unlatched, write the
  // buffer to the underlying writer and any future writes
  private final CountDownLatch latch = new CountDownLatch(1);

  // A buffer that holds writes until the latch is unlatched
  private final ArrayList<char[]> buffereredLines = new ArrayList<>();

  // This is set when the latch holder fails
  private volatile Throwable e;

  public LatchedWriter(IndentWriter writer) {
    super(writer);
  }

  // Call this when your processing is complete
  public synchronized void done() throws IOException {
    if (!buffereredLines.isEmpty()) {
      inner.writeLines(buffereredLines.toArray(new char[buffereredLines.size()][]));
    }
    latch.countDown();
  }

  // If you fail to complete, put an exception here
  public void failed(Throwable e) {
    this.e = e;
    latch.countDown();
  }

  @Override
  public synchronized void write(char[] cbuf, int off, int len) throws IOException {
    checkException();
    if (latch.getCount() == 0) {
      inner.write(cbuf, off, len);
    } else {
      this.appendToLastBuffer(cbuf, off, len);
    }
  }

  @Override
  public synchronized void writeLines(char[][] lines) throws IOException {
    checkException();
    if (latch.getCount() == 0) {
      inner.writeLines(lines);
    } else if (lines.length > 0) {
      this.appendToLastBuffer(lines[0], 0, lines[0].length);

      for (int i = 1; i < lines.length; ++i) {
        this.buffereredLines.add(lines[i]);
      }
    }
  }

  private void appendToLastBuffer(char[] cbuf, int off, int len) {
    int destOff;
    char[] dest;

    if (buffereredLines.isEmpty()) {
      dest = new char[len];
      destOff = 0;
      buffereredLines.add(dest);
    } else {
      char[] lastBuffer = buffereredLines.get(buffereredLines.size() - 1);

      destOff = lastBuffer.length;
      dest = new char[destOff + len];

      System.arraycopy(lastBuffer, 0, dest, 0, destOff);
      buffereredLines.set(buffereredLines.size() - 1, dest);
    }

    System.arraycopy(cbuf, off, dest, destOff, len);
  }

  private void checkException() throws IOException {
    if (e != null) {
      if (e instanceof IOException) {
        throw ((IOException) e);
      }
      throw new IOException(e);
    }
  }

  @Override
  public void flush() throws IOException {
    checkException();
    if (latch.getCount() == 0) {
      synchronized (this) {
        inner.flush();
      }
    }
  }

  @Override
  public void close() throws IOException {
    checkException();
    await();
    flush();
    inner.close();
  }

  public void await() {
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new MustacheException("Interrupted while waiting for completion", e);
    }
  }
}
