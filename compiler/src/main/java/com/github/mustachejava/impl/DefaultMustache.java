package com.github.mustachejava.impl;

import java.io.Writer;

import com.github.mustachejava.Code;
import com.github.mustachejava.CodeFactory;
import com.github.mustachejava.Mustache;

/**
* Default Mustache
*/
public class DefaultMustache extends DefaultCode implements Mustache {
  public DefaultMustache(CodeFactory cf, Code[] codes, String name, String sm, String em) {
    super(cf.getObjectHandler(), codes, name, null, sm, em);
  }

  @Override
  public void identity(Writer writer) {
    // No self output at the top level
    runIdentity(writer);
  }
}
