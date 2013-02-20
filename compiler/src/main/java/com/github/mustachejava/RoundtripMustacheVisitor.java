package com.github.mustachejava;

import com.github.mustachejava.codes.ExtendCode;
import com.github.mustachejava.codes.ExtendNameCode;
import com.github.mustachejava.codes.IterableCode;
import com.github.mustachejava.codes.NotIterableCode;
import com.github.mustachejava.codes.PartialCode;
import com.github.mustachejava.codes.ValueCode;

import java.io.IOException;
import java.io.Writer;

/**
 * Wrap resolved partials with markup but don't resolve other variables.
 * <p/>
 * User: sam
 * Date: 2/19/13
 * Time: 6:30 PM
 */
public class RoundtripMustacheVisitor extends DefaultMustacheVisitor {
  public RoundtripMustacheVisitor(DefaultMustacheFactory df) {
    super(df);
  }

  @Override
  public void extend(TemplateContext templateContext, final String variable, Mustache mustache) {
    list.add(new ExtendCode(templateContext, df, mustache, variable) {
      @Override
      public Writer execute(Writer writer, Object[] scopes) {
        try {
          writer.write("<mustache extends='" + variable + "'>");
          writer = super.execute(writer, scopes);
          writer.write("</mustache>");
          return writer;    //To change body of overridden methods use File | Settings | File Templates.
        } catch (IOException e) {
          throw new MustacheException(e);
        }
      }
    });
  }

  @Override
  public void iterable(TemplateContext templateContext, String variable, Mustache mustache) {
    list.add(new IterableCode(templateContext, df, mustache, variable) {
      @Override
      public Writer execute(Writer writer, Object[] scopes) {
        identity(writer);
        return writer;
      }
    });
  }

  @Override
  public void name(TemplateContext templateContext, String variable, Mustache mustache) {
    list.add(new ExtendNameCode(templateContext, df, mustache, variable));
  }

  @Override
  public void notIterable(TemplateContext templateContext, String variable, Mustache mustache) {
    list.add(new NotIterableCode(templateContext, df, mustache, variable) {
      @Override
      public Writer execute(Writer writer, Object[] scopes) {
        identity(writer);
        return writer;
      }
    });
  }

  @Override
  public void partial(TemplateContext templateContext, final String variable) {
    TemplateContext partialTC = new TemplateContext("{{", "}}", templateContext.file(), templateContext.line());
    list.add(new PartialCode(partialTC, df, variable) {
      @Override
      public Writer execute(Writer writer, Object[] scopes) {
        try {
          writer.write("<mustache partial='" + variable + "'>");
          writer = super.execute(writer, scopes);
          writer.write("</mustache>");
          return writer;    //To change body of overridden methods use File | Settings | File Templates.
        } catch (IOException e) {
          throw new MustacheException(e);
        }
      }
    });
  }

  @Override
  public void value(TemplateContext templateContext, String variable, boolean encoded) {
    list.add(new ValueCode(templateContext, df, variable, encoded) {
      @Override
      public Writer execute(Writer writer, Object[] scopes) {
        identity(writer);
        return writer;
      }
    });
  }
}
