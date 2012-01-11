package com.github.mustachejava.codes;

import com.github.mustachejava.Code;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.ObjectHandler;

/**
 * Name a section: {{$name}}...{{/name}}
 */
public class ExtendNameCode extends DefaultCode {
  public String getName() {
    return name;
  }

  public ExtendNameCode(MustacheFactory mf, Code[] codes, String name, String sm, String em) {
    super(mf.getObjectHandler(), codes, name, "$", sm, em);
  }
}
