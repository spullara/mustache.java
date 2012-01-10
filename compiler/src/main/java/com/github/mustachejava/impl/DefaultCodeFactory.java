package com.github.mustachejava.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.github.mustachejava.Code;
import com.github.mustachejava.CodeFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheCompiler;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.ObjectHandler;

/**
 * Simplest possible code factory
 */
public class DefaultCodeFactory implements CodeFactory {

  public static final Code EOF = new DefaultCode();

  public final MustacheCompiler mc = new MustacheCompiler(this);
  public final Map<String, Mustache> templateCache = new HashMap<String, Mustache>();
  public final ObjectHandler oh = new DefaultObjectHandler();

  private String root;

  public DefaultCodeFactory() {
    root = "";
  }

  public DefaultCodeFactory(String root) {
    if (!root.endsWith("/")) root += "/";
    this.root = root;
  }

  @Override
  public Object resolve(String name, Object... scopes) {
    if (name.equals(".")) {
      return scopes[scopes.length - 1];
    }
    MethodWrapper methodWrapper = oh.find(name, scopes);
    try {
      return methodWrapper == null ? null : methodWrapper.call(scopes);
    } catch (MethodGuardException e) {
      throw new MustacheException(e);
    }
  }

  @Override
  public Code iterable(final String variable, List<Code> codes, final String file, final int start, String sm, String em) {
    return new IterableCode(this, codes, variable, sm, em, file);
  }

  @Override
  public Code notIterable(final String variable, List<Code> codes, String file, int start, String sm, String em) {
    return new NotIterableCode(this, codes, variable, sm, em);
  }

  @Override
  public Code name(String variable, List<Code> codes, String file, int start, String sm, String em) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Code partial(String variable, String file, int line, String sm, String em) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Code value(final String variable, final boolean encoded, final int line, String sm, String em) {
    return new ValueCode(this, variable, sm, em, encoded, line);
  }

  @Override
  public Code write(final String text, int line, String sm, String em) {
    return new WriteCode(text);
  }

  @Override
  public Code eof(int line) {
    return EOF;
  }

  @Override
  public Code extend(String variable, List<Code> codes, String file, int start, String sm, String em) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Reader getReader(String file) {
    ClassLoader ccl = Thread.currentThread().getContextClassLoader();
    InputStream is = ccl.getResourceAsStream(root + file);
    if (is == null) {
      throw new MustacheException("Resource " + (root + file) + " not found");
    } else {
      return new BufferedReader(new InputStreamReader(is, Charset.forName("utf-8")));
    }
  }

  private static Pattern escapedPattern = Pattern.compile("^&\\w+;");

  // Override this in a super class if you don't want encoding or would like
  // to change the way encoding works. Also, if you use unexecute, make sure
  // also do the inverse in decode.
  @Override
  public String encode(String value) {
    StringBuilder sb = new StringBuilder();
    int position = 0;
    int length = value.length();
    for (int i = 0; i < length; i++) {
      char c = value.charAt(i);
      switch (c) {
        case '&':
          if (!escapedPattern.matcher(value.substring(i, length)).find()) {
            sb.append(value, position, i);
            position = i + 1;
            sb.append("&amp;");
          } else {
            if (position != 0) {
              sb.append(value, position, i);
              position = i + 1;
              sb.append("&");
            }
          }
          break;
        case '\\':
          sb.append(value, position, i);
          position = i + 1;
          sb.append("\\\\");
          break;
        case '"':
          sb.append(value, position, i);
          position = i + 1;
          sb.append("&quot;");
          break;
        case '<':
          sb.append(value, position, i);
          position = i + 1;
          sb.append("&lt;");
          break;
        case '>':
          sb.append(value, position, i);
          position = i + 1;
          sb.append("&gt;");
          break;
        case '\n':
          sb.append(value, position, i);
          position = i + 1;
          sb.append("&#10;");
          break;
      }
    }
    if (position == 0) {
      return value;
    } else {
      sb.append(value, position, value.length());
      return sb.toString();
    }
  }

}
