package com.sampullara.mustache.code;

import java.util.List;

import com.sampullara.mustache.Code;
import com.sampullara.mustache.Mustache;

/**
 * Implementation strategy:
 * - Load extension template codes
 * - Load local template codes
 * - Execute extension template codes, replacing named sections with local replacements
 */

public abstract class ExtendBaseCode extends SubCode {

  public ExtendBaseCode(Mustache m, String variable, List<Code> codes, String file, int line) {
    super("<", m, variable, codes, file, line);
  }

  public String getName() {
    return variable;
  }
}
