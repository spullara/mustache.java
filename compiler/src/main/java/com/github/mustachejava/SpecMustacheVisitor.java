package com.github.mustachejava;

import com.github.mustachejava.codes.PartialCode;
import com.github.mustachejava.codes.ValueCode;
import com.github.mustachejava.util.IndentWriter;

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

  @Override
  public void value(TemplateContext tc, final String variable, boolean encoded) {
    list.add(new SpecValueCode(tc, df, variable, encoded));
  }

  static class SpecPartialCode extends PartialCode {
    private final String indent;

    public SpecPartialCode(TemplateContext tc, DefaultMustacheFactory cf, String variable, String indent) {
      super(tc, cf, variable);
      this.indent = indent;
    }

    @Override
    protected Writer executePartial(Writer writer, final List<Object> scopes) {
      partial.execute(new IndentWriter(writer, indent), scopes);
      return writer;
    }
  }

  static class SpecValueCode extends ValueCode {

    public SpecValueCode(TemplateContext tc, DefaultMustacheFactory df, String variable, boolean encoded) {
      super(tc, df, variable, encoded);
    }

    @Override
    protected void execute(Writer writer, final String value) throws IOException {
      if (writer instanceof IndentWriter) {
        IndentWriter iw = (IndentWriter) writer;
        iw.flushIndent();
        writer = iw.inner;
        while (writer instanceof IndentWriter) {
          writer = ((IndentWriter) writer).inner;
        }
      }

      super.execute(writer, value);
    }
  }
}
