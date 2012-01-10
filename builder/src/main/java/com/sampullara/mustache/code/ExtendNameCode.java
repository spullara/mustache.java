package com.sampullara.mustache.code;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.sampullara.mustache.Code;
import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;

import static com.sampullara.mustache.Mustache.truncate;

/**
* The named reference.
* <p/>
* User: sam
* Date: 11/27/11
* Time: 10:39 AM
*/
public class ExtendNameCode extends ExtendBaseCode {

  public ExtendNameCode(Mustache m, String variable, List<Code> codes, String file, int line) {
    super(m, variable, codes, file, line);
  }

  @Override
  public void execute(FutureWriter fw, Scope scope) throws MustacheException {
    execute(fw, Arrays.asList(scope));
  }

  @Override
  public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
    for (int i = 0; i < codes.length; i++) {
      if (Mustache.debug) {
        Mustache.line.set(codes[i].getLine());
      }
      Code[] truncate = truncate(codes, i + 1, next);
      current = codes[i].unexecute(current, text, position, truncate);
    }
    return current;
  }
}
