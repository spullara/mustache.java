package com.github.mustachejava;

import com.github.mustachejava.codes.*;

import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The default implementation that builds up Code lists
 */
public class DefaultMustacheVisitor implements MustacheVisitor {
  protected static Logger logger = Logger.getLogger(DefaultMustacheVisitor.class.getSimpleName());

  private static final Code EOF = new DefaultCode();

  protected final List<Code> list = new LinkedList<Code>();
  private final Map<String, PragmaHandler> handlers = new HashMap<String, PragmaHandler>() {{
    put("implicit-iterator", new PragmaHandler() {
      @Override
      public Code handle(String pragma, String args) {
        return new DefaultCode() {
          @Override
          public Writer execute(Writer writer, Object[] scopes) {
            return super.execute(writer, scopes);
          }
        };
      }
    });
  }};

  protected DefaultMustacheFactory cf;

  public DefaultMustacheVisitor(DefaultMustacheFactory cf) {
    this.cf = cf;
  }

  public void addPragmaHandler(String pragma, PragmaHandler handler) {
    handlers.put(pragma.toLowerCase(), handler);
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
  public void pragma(TemplateContext templateContext, String pragma, String args) {
    PragmaHandler pragmaHandler = handlers.get(pragma.toLowerCase());
    if (pragmaHandler == null) {
      // By default, warn that no pragmas are understood
      logger.warning("Unimplemented pragma: " + pragma);
    } else {
      Code code = pragmaHandler.handle(pragma, args);
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
    list.add(new ExtendCode(templateContext, cf, mustache, variable));
  }

}
