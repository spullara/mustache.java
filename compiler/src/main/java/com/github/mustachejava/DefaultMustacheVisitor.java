package com.github.mustachejava;

import com.github.mustachejava.codes.*;

import java.util.LinkedList;
import java.util.List;

/**
 * The default implementation that builds up Code lists
 */
public class DefaultMustacheVisitor implements MustacheVisitor {
  private static final Code EOF = new DefaultCode();

  protected final List<Code> list = new LinkedList<Code>();
  protected DefaultMustacheFactory cf;

  public DefaultMustacheVisitor(DefaultMustacheFactory cf) {
    this.cf = cf;
  }

  @Override
  public Mustache mustache(TemplateContext templateContext) {
    return new DefaultMustache(templateContext, cf, list.toArray(new Code[list.size()]), templateContext.file());
  }

  @Override
  public void iterable(TemplateContext templateContext, String variable, Mustache mustache) {
    list.add(new IterableCode(templateContext, cf, mustache, variable));
  }

  @Override
  public void notIterable(TemplateContext templateContext, String variable, Mustache mustache) {
    list.add(new NotIterableCode(templateContext, cf, mustache, variable));
  }

  @Override
  public void name(TemplateContext templateContext, String variable, Mustache mustache) {
    list.add(new ExtendNameCode(templateContext, cf, mustache, variable));
  }

  @Override
  public void partial(TemplateContext templateContext, final String variable) {
    TemplateContext partialTC = new TemplateContext("{{", "}}", templateContext.file(), templateContext.line());
    list.add(new PartialCode(partialTC, cf, variable));
  }

  @Override
  public void value(TemplateContext templateContext, final String variable, boolean encoded) {
    list.add(new ValueCode(templateContext, cf, variable, encoded));
  }

  @Override
  public void write(TemplateContext templateContext, final String text) {
    if (text.length() > 0) {
      int size = list.size();
      if (size > 0) {
        Code code = list.get(size - 1);
        code.append(text);
      } else {
        list.add(new WriteCode(text));
      }
    }
  }

  @Override
  public void eof(String file, int line) {
    list.add(EOF);
  }

  @Override
  public void extend(TemplateContext templateContext, String variable, Mustache mustache) {
    list.add(new ExtendCode(templateContext, cf, mustache, variable));
  }

}
