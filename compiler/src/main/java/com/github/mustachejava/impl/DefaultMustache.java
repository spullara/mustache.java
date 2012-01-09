package com.github.mustachejava.impl;

import java.io.Writer;
import java.util.List;

import com.github.mustachejava.Code;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.impl.DefaultCode;

/**
* Default Mustache
*/
public class DefaultMustache extends DefaultCode implements Mustache {
  public DefaultMustache(Code[] codes, String name, String sm, String em) {
    super(codes, name, ">", sm, em);
  }
}
