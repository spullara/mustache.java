package com.sampullara.mustache.code;

import com.sampullara.mustache.Code;
import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sampullara.mustache.Mustache.truncate;

/**
* This handles the if extension.
* <p/>
* User: sam
* Date: 11/27/11
* Time: 10:37 AM
*/
public class IfIterableCode extends SubCode {
  public IfIterableCode(Mustache m, String variable, List<Code> codes, String file, int line) {
    super("?", m, variable, codes, file, line);
  }

  @Override
  public void execute(FutureWriter fw, Scope scope) throws MustacheException {
    fw = m.pushWriter(fw);
    execute(fw, m.ifiterable(scope, variable));
  }

  @Override
  public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
    // Like the iterable version with only one
    Scope result = new Scope();
    for (int i = 0; i < codes.length && result != null; i++) {
      if (Mustache.debug) {
        Mustache.line.set(codes[i].getLine());
      }
      Code[] truncate = truncate(codes, i + 1, next);
      result = codes[i].unexecute(result, text, position, truncate);
    }
    if (result != null && result.size() > 0) {
      BuilderCodeFactory.put(current, variable, result);
    }
    return current;
  }
}
