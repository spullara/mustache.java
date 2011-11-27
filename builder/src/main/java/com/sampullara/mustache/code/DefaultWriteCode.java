package com.sampullara.mustache.code;

import com.sampullara.mustache.Code;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
* Writes a string.
* <p/>
* User: sam
* Date: 11/27/11
* Time: 10:46 AM
*/
public class DefaultWriteCode implements WriteCode {
  private final StringBuffer rest;
  private final int line;

  public DefaultWriteCode(String rest, int line) {
    this.rest = new StringBuffer(rest);
    this.line = line;
  }

  public void execute(FutureWriter fw, Scope scope) throws MustacheException {
    try {
      fw.write(rest.toString());
    } catch (IOException e) {
      throw new MustacheException("Failed to write", e);
    }
  }

  @Override
  public int getLine() {
    return line;
  }

  @Override
  public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
    if (position.get() + rest.length() <= text.length()) {
      String substring = text.substring(position.get(), position.get() + rest.length());
      if (rest.toString().equals(substring)) {
        position.addAndGet(rest.length());
        return current;
      }
    }
    return null;
  }

  @Override
  public void identity(FutureWriter fw) throws MustacheException {
    execute(fw, null);
  }

  public void append(String append) {
    rest.append(append);
  }

}
