package com.github.mustachejava.codes;

import com.github.mustachejava.Code;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.ObjectHandler;

/**
 * Name a section: {{$name}}...{{/name}}
 */
public class ExtendNameCode extends DefaultCode {
  public String getName() {
    return name;
  }

  public ExtendNameCode(MustacheFactory mf, Mustache mustache, String name, String sm, String em) {
    super(mf.getObjectHandler(), mustache, name, "$", sm, em);
  }
}
