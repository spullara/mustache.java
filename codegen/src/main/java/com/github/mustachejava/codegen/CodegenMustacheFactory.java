package com.github.mustachejava.codegen;

import com.github.mustachejava.*;

import java.io.File;

/**
 * Codegen mustache code execution.
 */
public class CodegenMustacheFactory extends DefaultMustacheFactory {
  @Override
  public MustacheVisitor createMustacheVisitor() {
    return new CodegenMustacheVisitor(this);
  }

  public CodegenMustacheFactory() {
    super();
    setObjectHandler(new CodegenObjectHandler());
  }

  public CodegenMustacheFactory(File fileRoot) {
    super(fileRoot);
    setObjectHandler(new CodegenObjectHandler());
  }

  public CodegenMustacheFactory(String resourceRoot) {
    super(resourceRoot);
    setObjectHandler(new CodegenObjectHandler());
  }

}
