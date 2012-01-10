package com.sampullara.mustache.code;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.sampullara.mustache.Code;
import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;

import static com.sampullara.mustache.Mustache.truncate;

/**
* Extending a template.
* <p/>
* User: sam
* Date: 11/27/11
* Time: 10:39 AM
*/
public class ExtendCode extends ExtendBaseCode {

  private Mustache partial;

  public ExtendCode(Mustache m, String variable, List<Code> codes, String file, int line) throws MustacheException {
    super(m, variable, codes, file, line);
    Map<String, ExtendNameCode> replaceMap = new HashMap<String, ExtendNameCode>();
    for (Code code : codes) {
      if (code instanceof ExtendNameCode) {
        // put name codes in the map
        ExtendNameCode erc = (ExtendNameCode) code;
        replaceMap.put(erc.getName(), erc);
      } else if (code instanceof WriteCode) {
        // ignore text
      } else {
        // fail on everything else
        throw new IllegalArgumentException(
                "Illegal code in extend section: " + code.getClass().getName());
      }
    }
    Map<String, ExtendNameCode> debugMap = null;
    if (Mustache.debug) {
      debugMap = new HashMap<String, ExtendNameCode>(replaceMap);
    }
    partial = m.partial(variable);
    Code[] supercodes = partial.getCompiled();
    // recursively replace named sections with replacements
    replaceCode(supercodes, replaceMap, debugMap);
    if (Mustache.debug) {
      if (debugMap != null && debugMap.size() > 0) {
        throw new MustacheException(
                "Replacement sections failed to match named sections: " + debugMap.keySet());
      }
    }
  }

  private void replaceCode(Code[] supercodes, Map<String, ExtendNameCode> replaceMap, Map<String, ExtendNameCode> debugMap) {
    for (int i = 0; i < supercodes.length; i++) {
      Code code = supercodes[i];
      if (code instanceof ExtendNameCode) {
        ExtendNameCode enc = (ExtendNameCode) code;
        ExtendNameCode extendReplaceCode = replaceMap.get(enc.getName());
        if (extendReplaceCode != null) {
          supercodes[i] = extendReplaceCode;
        } else {
          if (Mustache.debug) {
            debugMap.remove(enc.getName());
          }
          replaceCode(enc.codes, replaceMap, debugMap);
        }
      } else if (code instanceof SubCode) {
        SubCode subcode = (SubCode) code;
        replaceCode(subcode.codes, replaceMap, debugMap);
      }
    }
  }

  @Override
  public void execute(FutureWriter fw, Scope scope) throws MustacheException {
    partial.execute(fw, scope);
  }

  @Override
  public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
    Code[] supercodes = partial.getCompiled();
    for (int i = 0; i < supercodes.length; i++) {
      Code[] truncate = truncate(supercodes, i + 1, next);
      supercodes[i].unexecute(current, text, position, truncate);
    }
    return current;
  }
}
