package com.github.mustachejava.codes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.TemplateContext;

public class CommentCode extends DefaultCode {
  public CommentCode(TemplateContext tc, DefaultMustacheFactory df, String comment) {
    super(tc, df, null, comment, "!");
  }
}
