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
  public Mustache mustache(String file, String sm, String em) {
    return new DefaultMustache(cf, list.toArray(new Code[list.size()]), file, sm, em);
  }

  @Override
  public void iterable(final String variable, Mustache mustache, final String file, final int start, String sm, String em) {
    list.add(new IterableCode(cf, mustache, variable, sm, em, file));
  }

  @Override
  public void notIterable(final String variable, Mustache mustache, String file, int start, String sm, String em) {
    list.add(new NotIterableCode(cf, mustache, variable, sm, em));
  }

  @Override
  public void name(String variable, Mustache mustache, String file, int start, String sm, String em) {
    list.add(new ExtendNameCode(cf, mustache, variable, sm, em));
  }

  @Override
  public void partial(final String variable, String file, int line, String sm, String em) {
    list.add(new PartialCode(cf, variable, file, "{", "}"));
  }

  @Override
  public void value(final String variable, final boolean encoded, final int line, String sm, String em) {
    list.add(new ValueCode(cf, variable, sm, em, encoded, line));
  }

  @Override
  public void write(final String text, int line, String sm, String em) {
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
  public void eof(int line) {
    list.add(EOF);
  }

  @Override
  public void extend(String variable, Mustache mustache, String file, int start, String sm, String em) {
    list.add(new ExtendCode(cf, mustache, variable, file, sm, em));
  }

}
