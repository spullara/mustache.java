package com.github.mustachejava.codes;

import com.github.mustachejava.Code;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.TemplateContext;

import java.io.Writer;

/**
* Default Mustache
*/
public class DefaultMustache extends DefaultCode implements Mustache {
  private Code[] codes;
  private boolean inited = false;

  public DefaultMustache(TemplateContext tc, MustacheFactory cf, Code[] codes, String name) {
    super(tc, cf.getObjectHandler(), null, name, null);
    this.codes = codes;
  }

  @Override
  public Code[] getCodes() {
    return codes;
  }

  @Override
  public void setCodes(Code[] newcodes) {
    codes = newcodes;
  }

  @Override
  public void identity(Writer writer) {
    // No self output at the top level
    runIdentity(writer);
  }

  @Override
  public synchronized void init() {
    if (!inited) {
      inited = true;
      super.init();
    }
  }
}
