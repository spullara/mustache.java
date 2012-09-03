package com.github.mustachejava.codegen;

import com.github.mustachejava.*;
import com.github.mustachejava.codes.DefaultMustache;

import java.io.File;
import java.io.Writer;

/**
 * Codegen mustache code execution.
 */
public class CodegenMustacheFactory extends DefaultMustacheFactory {
  @Override
  public MustacheVisitor createMustacheVisitor() {
    return new DefaultMustacheVisitor(this) {
      @Override
      public Mustache mustache(TemplateContext templateContext) {
        return new DefaultMustache(templateContext, cf, list.toArray(new Code[list.size()]), templateContext.file()) {

          private CompiledCodes compiledCodes;

          @Override
          public Writer run(Writer writer, Object[] scopes) {
            if (compiledCodes != null) {
              return compiledCodes.runCodes(writer, scopes);
            }
            return super.run(writer, scopes);
          }

          @Override
          public void setCodes(Code[] newcodes) {
            super.setCodes(newcodes);
            if (newcodes != null) {
              compiledCodes = CodeCompiler.compile(newcodes);
            }
          }
        };
      }
    };
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
