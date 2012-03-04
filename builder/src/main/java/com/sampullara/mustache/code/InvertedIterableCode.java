package com.sampullara.mustache.code;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sampullara.mustache.Code;
import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;

import static com.sampullara.mustache.Mustache.truncate;

/**
* This handles the falseyness check.
* <p/>
* User: sam
* Date: 11/27/11
* Time: 10:37 AM
*/
public class InvertedIterableCode extends SubCode {
  public InvertedIterableCode(Mustache m, String variable, List<Code> codes, String file, int line) {
    super("^", m, variable, codes, file, line);
  }

  @Override
  public void execute(FutureWriter fw, Scope scope) throws MustacheException {
    fw = m.pushWriter(fw);
    execute(fw, m.inverted(scope, variable));
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
    if (result != null) {
      current.putAll(result);
      BuilderCodeFactory.put(current, variable, false);
    }
    return current;
  }
}
