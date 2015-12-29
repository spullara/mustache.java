package com.github.mustachejava;

import com.github.mustachejava.codes.DefaultCode;
import com.github.mustachejava.codes.DefaultMustache;
import com.github.mustachejava.codes.ExtendCode;
import com.github.mustachejava.codes.ExtendNameCode;
import com.github.mustachejava.codes.IterableCode;
import com.github.mustachejava.codes.NotIterableCode;
import com.github.mustachejava.codes.PartialCode;
import com.github.mustachejava.codes.DynamicPartialCode;
import com.github.mustachejava.codes.ValueCode;
import com.github.mustachejava.codes.WriteCode;
import com.github.mustachejava.util.Node;

import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * The default implementation that builds up Code lists
 */
public class DefaultMustacheVisitor implements MustacheVisitor {
  protected static Logger logger = Logger.getLogger(DefaultMustacheVisitor.class.getSimpleName());

  private static final Code EOF = new DefaultCode() {
    @Override
    public Node invert(Node node, String text, AtomicInteger position) {
      return text.length() == position.get() ? node : null;
    }
  };

  protected final List<Code> list = new LinkedList<Code>();
  private final Map<String, PragmaHandler> handlers = new HashMap<String, PragmaHandler>() {{
    put("implicit-iterator", new PragmaHandler() {
      @Override
      public Code handle(TemplateContext tc, String pragma, String args) {
        return new DefaultCode() {
          @Override
          public Writer execute(Writer writer, Object[] scopes) {
            return super.execute(writer, scopes);
          }
        };
      }
    });
  }};

  protected DefaultMustacheFactory df;

  public DefaultMustacheVisitor(DefaultMustacheFactory df) {
    this.df = df;
  }

  public void addPragmaHandler(String pragma, PragmaHandler handler) {
    handlers.put(pragma.toLowerCase(), handler);
  }

  @Override
  public Mustache mustache(TemplateContext templateContext) {
    return new DefaultMustache(templateContext, df, list.toArray(new Code[list.size()]), templateContext.file());
  }

  @Override
  public void iterable(TemplateContext templateContext, String variable, Mustache mustache) {
    list.add(new IterableCode(templateContext, df, mustache, variable));
  }

  @Override
  public void notIterable(TemplateContext templateContext, String variable, Mustache mustache) {
    list.add(new NotIterableCode(templateContext, df, mustache, variable));
  }

  @Override
  public void name(TemplateContext templateContext, String variable, Mustache mustache) {
    list.add(new ExtendNameCode(templateContext, df, mustache, variable));
  }

  @Override
  public void partial(TemplateContext tc, final String variable) {
    TemplateContext partialTC = new TemplateContext("{{", "}}", tc.file(), tc.line(), tc.startOfLine());
    list.add(new PartialCode(partialTC, df, variable));
  }
  
  @Override
  public void dynamicPartial(TemplateContext tc, final String variable) {
    TemplateContext partialTC = new TemplateContext("{{", "}}", tc.file(), tc.line(), tc.startOfLine());
    list.add(new DynamicPartialCode(partialTC, df, variable));
  }

  @Override
  public void value(TemplateContext tc, final String variable, boolean encoded) {
    list.add(new ValueCode(tc, df, variable, encoded));
  }

  @Override
  public void write(TemplateContext tc, final String text) {
    if (text.length() > 0) {
      int size = list.size();
      if (size > 0) {
        Code code = list.get(size - 1);
        code.append(text);
      } else {
        list.add(new WriteCode(tc, df, text));
      }
    }
  }

  @Override
  public void pragma(TemplateContext tc, String pragma, String args) {
    PragmaHandler pragmaHandler = handlers.get(pragma.toLowerCase());
    if (pragmaHandler == null) {
      // By default, warn that no pragmas are understood
      logger.warning("Unimplemented pragma: " + pragma);
    } else {
      Code code = pragmaHandler.handle(tc, pragma, args);
      if (code != null) {
        list.add(code);
      }
    }
  }

  @Override
  public void eof(TemplateContext templateContext) {
    list.add(EOF);
  }

  @Override
  public void extend(TemplateContext templateContext, String variable, Mustache mustache) {
    list.add(new ExtendCode(templateContext, df, mustache, variable));
  }

}
