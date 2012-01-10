package com.github.mustachejava.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.CountDownLatch;

/**
 * Buffer content while a future is being evaluated in another thread.
 */
public class LatchedWriter extends Writer {

  // Write to the buffer while latched, when unlatched, write the
  // buffer to the underlying writer and any future writes
  private final CountDownLatch latch = new CountDownLatch(1);

  // A buffer that holds writes until the latch is unlatched
  private final StringWriter buffer = new StringWriter();

  // The underlying writer
  private final Writer writer;

  // true when we have written the buffer to the underlying writer
  private boolean isWritten;

  // true when we have flushed and closed the underlying writer
  private boolean isClosed;

  // This is set when the latch holder fails
  private Throwable e;

  public LatchedWriter(Writer writer) {
    this.writer = writer;
  }

  // Call this when your processing is complete
  public synchronized void done() {
    latch.countDown();
  }

  @Override
  public synchronized void write(char[] cbuf, int off, int len) throws IOException {
    checkException();
    if (latch.getCount() == 0) {
      if (!isWritten) {
        isWritten = true;
        writer.append(buffer.getBuffer());
      }
      writer.write(cbuf, off, len);
    } else {
      buffer.write(cbuf, off, len);
    }
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
    try {
      latch.await();
      synchronized (this) {
        if (!isWritten) {
          isWritten = true;
          writer.append(buffer.getBuffer());
        }
        writer.flush();
      }
    } catch (InterruptedException e) {
      throw new IOException("Interrupted while waiting to complete", e);
    }
  }

  @Override
  public synchronized void close() throws IOException {
    checkException();
    if (isClosed) throw new IOException("Alread closed");
    isClosed = true;
    flush();
    writer.close();
  }


  public synchronized void failed(Throwable e) {
    this.e = e;
  }
}
