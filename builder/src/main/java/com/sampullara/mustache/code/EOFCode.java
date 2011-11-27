package com.sampullara.mustache.code;

import com.sampullara.mustache.Code;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;

import java.util.concurrent.atomic.AtomicInteger;

/**
* Represents the end of file.
* <p/>
* User: sam
* Date: 11/27/11
* Time: 10:40 AM
*/
public class EOFCode implements Code {

  private final int line;

  public EOFCode(int line) {
    this.line = line;
  }

  @Override
  public void execute(FutureWriter fw, Scope scope) throws MustacheException {
    // NOP
  }

  @Override
  public int getLine() {
    return line;
  }

  @Override
  public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
    // End of text
    position.set(text.length());
    return current;
  }

  @Override
  public void identity(FutureWriter fw) throws MustacheException {
    // NOP
  }
}
