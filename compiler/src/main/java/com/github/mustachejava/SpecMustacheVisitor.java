package com.github.mustachejava;

import com.github.mustachejava.codes.PartialCode;
import com.github.mustachejava.codes.ValueCode;
import com.github.mustachejava.util.IndentWriter;
import com.github.mustachejava.util.InnerIndentWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class SpecMustacheVisitor extends DefaultMustacheVisitor {
  public SpecMustacheVisitor(DefaultMustacheFactory df) {
    super(df);
  }

  @Override
  public void partial(TemplateContext tc, final String variable, String indent) {
    TemplateContext partialTC = new TemplateContext("{{", "}}", tc.file(), tc.line(), tc.startOfLine());
    list.add(new SpecPartialCode(partialTC, df, variable, indent));
  }

  static class SpecPartialCode extends PartialCode {
    private final char[] indent;

    public SpecPartialCode(TemplateContext tc, DefaultMustacheFactory cf, String variable, String indent) {
      super(tc, cf, variable);
      this.indent = indent.toCharArray();
    }

    @Override
    protected IndentWriter executePartial(IndentWriter writer, final List<Object> scopes) {
      partial.execute(new InnerIndentWriter(writer, indent), scopes);
      return writer;
    }
  }
}
