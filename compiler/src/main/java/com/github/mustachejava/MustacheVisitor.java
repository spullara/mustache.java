package com.github.mustachejava;

/**
 * Callbacks from the parser as a mustache template is parsed.
 */
public interface MustacheVisitor {
  // Mustache
  Mustache mustache(TemplateContext templateContext);

  // Specified
  void iterable(TemplateContext templateContext, String variable, Mustache mustache);

  void notIterable(TemplateContext templateContext, String variable, Mustache mustache);

  void partial(TemplateContext templateContext, String variable);

  void value(TemplateContext templateContext, String variable, boolean encoded);

  void write(TemplateContext templateContext, String text);

  void pragma(TemplateContext templateContext, String pragma, String args);

  // Internal
  void eof(TemplateContext templateContext);

  // Extension
  void extend(TemplateContext templateContext, String variable, Mustache mustache);

  void name(TemplateContext templateContext, String variable, Mustache mustache);

  void comment(TemplateContext templateContext, String comment);
}
