package com.github.mustachejava.impl;

import com.github.mustachejava.Code;
import com.github.mustachejava.CodeFactory;
import com.github.mustachejava.Mustache;

/**
* Default Mustache
*/
public class DefaultMustache extends DefaultCode implements Mustache {
  public DefaultMustache(CodeFactory cf, Code[] codes, String name, String sm, String em) {
    super(cf.getObjectHandler(), codes, name, ">", sm, em);
  }
}
