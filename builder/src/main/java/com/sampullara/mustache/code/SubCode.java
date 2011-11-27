package com.sampullara.mustache.code;

import com.sampullara.mustache.Code;
import com.sampullara.mustache.FunctionIterator;
import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
* The base class for all codes with subcodes.
* <p/>
* User: sam
* Date: 11/27/11
* Time: 10:34 AM
*/
public abstract class SubCode implements Code {
  protected final String marker;
  protected final Mustache m;
  protected final String variable;
  protected final Code[] codes;
  protected final int line;
  protected final String file;

  public SubCode(String marker, Mustache m, String variable, List<Code> codes, String file, int line) {
    this.marker = marker;
    this.m = m;
    this.variable = variable;
    this.codes = new ArrayList<Code>(codes).toArray(new Code[codes.size()]);
    this.line = line;
    this.file = file;
  }

  @Override
  public abstract void execute(FutureWriter fw, Scope scope) throws MustacheException;

  protected void execute(FutureWriter fw, final Iterable<Scope> iterable) throws MustacheException {
    if (iterable != null) {
      for (final Scope subScope : iterable) {
        try {
          fw = m.pushWriter(fw);
          fw.enqueue(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
              FutureWriter writer = new FutureWriter();
              for (Code code : codes) {
                if (Mustache.debug) {
                  Mustache.line.set(code.getLine());
                }
                if (iterable instanceof FunctionIterator && ((FunctionIterator) iterable).isTemplateFunction()) {
                  code.identity(writer);
                } else {
                  code.execute(writer, subScope);
                }
              }
              return writer;
            }
          });
        } catch (IOException e) {
          throw new MustacheException("Execution failed: " + file + ":" + line, e);
        }
      }
    }
  }

  public int getLine() {
    return line;
  }

  public void identity(FutureWriter fw) throws MustacheException {
    try {
      fw.append("{{").append(marker).append(variable).append("}}");
      for (Code code : codes) {
        if (Mustache.debug) {
          Mustache.line.set(code.getLine());
        }
        code.identity(fw);
      }
      fw.append("{{/").append(variable).append("}}");
    } catch (IOException e) {
      throw new MustacheException("Failed to write", e);
    }
  }
}
