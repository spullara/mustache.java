package com.sampullara.mustache.code;

import com.sampullara.mustache.Code;
import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sampullara.mustache.Mustache.truncate;

/**
* Looping construct.
* <p/>
* User: sam
* Date: 11/27/11
* Time: 10:35 AM
*/
public class IterableCode extends SubCode {
  public IterableCode(Mustache m, String variable, List<Code> codes, String file, int line) {
    super("#", m, variable, codes, file, line);
  }

  @Override
  public void execute(FutureWriter fw, Scope scope) throws MustacheException {
    execute(fw, m.iterable(scope, variable));
  }

  @Override
  public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
    // I think we have to make iteration greedy and match until we can't find a match
    List<Scope> results = new ArrayList<Scope>();
    Scope result;
    do {
      int start = position.get();
      result = new Scope();
      for (int i = 0; i < codes.length && result != null; i++) {
        if (Mustache.debug) {
          Mustache.line.set(codes[i].getLine());
        }
        Code[] truncate = truncate(codes, i + 1, next);
        result = codes[i].unexecute(result, text, position, truncate);
      }
      if (result != null && result.size() > 0) {
        results.add(result);
      } else {
        position.set(start);
        break;
      }
    } while (true);
    if (results.size() != 0) {
      current.put(variable, results);
    }
    return current;
  }
}
