package com.sampullara.mustache.code;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import com.sampullara.mustache.Code;
import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;

import static com.sampullara.mustache.Mustache.truncate;

/**
* Partial handling.
* <p/>
* User: sam
* Date: 11/27/11
* Time: 10:38 AM
*/
public class PartialCode implements Code {
  private final String variable;
  private Mustache m;
  private final String file;
  private final int line;
  private volatile Mustache partial;

  public PartialCode(Mustache m, String variable, String file, int line) throws MustacheException {
    this.variable = variable;
    this.m = m;
    this.file = file;
    this.line = line;
    partial = m.partial(variable);
  }

  @Override
  public void execute(FutureWriter fw, final Scope scope) throws MustacheException {
    try {
      if (fw.isParallel()) {
        fw.enqueue(new Callable<Object>() {
          @Override
          public Object call() throws Exception {
            FutureWriter fw = new FutureWriter();
            partial.partial(fw, scope, variable, partial);
            return fw;
          }
        });
      } else {
        partial.partial(fw, scope, variable, partial);
      }
    } catch (IOException e) {
      throw new MustacheException("Execution failed: " + file + ":" + line, e);
    }
  }

  @Override
  public int getLine() {
    return line;
  }

  @Override
  public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
    Mustache partial = m.partial(variable);
    Code[] compiled = partial.getCompiled();
    Scope unexecuted = new Scope();
    for (int i = 0; i < compiled.length && unexecuted != null; i++) {
      if (compiled[i] instanceof EOFCode) break;
      Code[] truncate = truncate(compiled, i + 1, new Code[0]);
      unexecuted = compiled[i].unexecute(unexecuted, text, position, truncate);
    }
    if (unexecuted != null) {
      BuilderCodeFactory.put(current, variable, unexecuted);
    }
    return current;
  }

  @Override
  public void identity(FutureWriter fw) throws MustacheException {
    try {
      fw.append("{{>").append(variable).append("}}");
    } catch (IOException e) {
      throw new MustacheException("Failed", e);
    }
  }
}
