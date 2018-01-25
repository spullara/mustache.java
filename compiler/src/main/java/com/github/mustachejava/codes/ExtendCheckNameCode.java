package com.github.mustachejava.codes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.TemplateContext;

import java.io.Writer;
import java.util.List;

/**
 * Name a section: {{$name}}...{{/name}}
 */
public class ExtendCheckNameCode extends DefaultCode {
  private boolean suppressed;

  public ExtendCheckNameCode(TemplateContext templateContext, DefaultMustacheFactory df, Mustache mustache, String variable) {
    super(templateContext, df, mustache, variable, "$");
  }

  public String getName() {
    return name;
  }
}
