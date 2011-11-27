package com.sampullara.mustache.code;

import com.sampullara.mustache.Code;
import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
* Writes a raw value with or without mustache encoding.
* <p/>
* User: sam
* Date: 11/27/11
* Time: 10:40 AM
*/
public class WriteValueCode implements Code {
  private final Mustache m;
  private final String name;
  private final boolean encoded;
  private final int line;

  public WriteValueCode(Mustache m, String name, boolean encoded, int line) {
    this.m = m;
    this.name = name;
    this.encoded = encoded;
    this.line = line;
  }

  @Override
  public void execute(FutureWriter fw, Scope scope) throws MustacheException {
    m.write(fw, scope, name, encoded);
  }

  @Override
  public int getLine() {
    return line;
  }

  @Override
  public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
    String value = BuilderCodeFactory.unexecuteValueCode(m, current, text, position, next, encoded);
    if (value != null) {
      BuilderCodeFactory.put(current, name, value);
      return current;
    }
    return null;
  }

  @Override
  public void identity(FutureWriter fw) throws MustacheException {
    try {
      if (!encoded) fw.append("{");
      fw.append("{{").append(name).append("}}");
      if (!encoded) fw.append("}");
    } catch (IOException e) {
      throw new MustacheException(e);
    }
  }

}
