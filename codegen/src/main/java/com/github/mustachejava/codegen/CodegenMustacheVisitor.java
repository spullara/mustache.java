package com.github.mustachejava.codegen;

import com.github.mustachejava.*;
import com.github.mustachejava.codes.DefaultMustache;

import java.io.Writer;
import java.util.List;

public class CodegenMustacheVisitor extends DefaultMustacheVisitor {
  public CodegenMustacheVisitor(DefaultMustacheFactory mf) {
    super(mf);
  }

  @Override
  public Mustache mustache(TemplateContext templateContext) {
    return new DefaultMustache(templateContext, df, list.toArray(new Code[list.size()]), templateContext.file()) {

      private CompiledCodes compiledCodes;

      @Override
      public Writer run(Writer writer, List<Object> scopes) {
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
}
