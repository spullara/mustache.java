package com.github.mustachejava.codes;

import com.github.mustachejava.Code;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.Writer;

/**
* Default Mustache
*/
public class DefaultMustache extends DefaultCode implements Mustache {
  private Code[] codes;

  public DefaultMustache(MustacheFactory cf, Code[] codes, String name, String sm, String em) {
    super(cf.getObjectHandler(), null, name, null, sm, em);
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
}
