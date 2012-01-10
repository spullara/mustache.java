package com.github.mustachejava.impl;

import java.io.IOException;
import java.io.Writer;

import com.github.mustachejava.Code;
import com.github.mustachejava.CodeFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;

/**
* Default Mustache
*/
public class DefaultMustache extends DefaultCode implements Mustache {
  public DefaultMustache(CodeFactory cf, Code[] codes, String name, String sm, String em) {
    super(cf.getObjectHandler(), codes, name, null, sm, em);
  }

  @Override
  public Writer execute(Writer writer, Object... scopes) {
    writer = super.execute(writer, scopes);
    try {
      writer.flush();
    } catch (IOException e) {
      throw new MustacheException(e);
    }
    return writer;
  }

  @Override
  public void identity(Writer writer) {
    // No self output at the top level
    runIdentity(writer);
  }
}
