package com.github.mustachejava.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.github.mustachejava.Code;
import com.github.mustachejava.CodeFactory;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.ObjectHandler;

/**
 * Simplest possible code factory
 */
public class DefaultCodeFactory implements CodeFactory {

  public static final Code EOF = new DefaultCode();
  private ObjectHandler oh = new DefaultObjectHandler();
  private String root;

  public DefaultCodeFactory() {
    root = "/";
  }
  
  public DefaultCodeFactory(String root) {
    if (!root.endsWith("/")) root += "/";
    this.root = root;
  }

  private Object resolve(List<Object> scopes, String name) {
    Object value = null;
    int size = scopes.size();
    for (int i = size - 1; i >= 0; i--) {
      Object scope = scopes.get(i);
      Object o = oh.handleObject(scope, name);
      if (o != null) {
        if (o == ObjectHandler.NULL) {
          value = null;
        } else {
          value = o;
        }
        break;
      }
    }
    return value;
  }

  @Override
  public Code iterable(final String variable, List<Code> codes, String file, int start) {
    return new DefaultCode(codes.toArray(new Code[0])) {
      @Override
      public void execute(Writer writer, List<Object> scopes) {
        Object resolve = resolve(scopes, variable);
        if (resolve != null) {
          for (Iterator i = oh.iterate(resolve); i.hasNext(); ) {
            Object next = i.next();
            List<Object> iteratorScopes = scopes;
            if (next != null) {
              iteratorScopes = new ArrayList<Object>(scopes);
              iteratorScopes.add(next);
            }
            if (codes != null) {
              int length = codes.length;
              for (int i1 = 0; i1 < length; i1++) {
                codes[i1].execute(writer, iteratorScopes);
              }
            }
          }
        }
        if (appended != null) {
          try {
            writer.write(appended);
          } catch (IOException e) {
            throw new MustacheException(e);
          }
        }
      }
    };
  }

  @Override
  public Code notIterable(String variable, List<Code> codes, String file, int start) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Code name(String variable, List<Code> codes, String file, int start) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Code partial(String variable, String file, int line) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Code value(final String variable, final boolean encoded, final int line) {
    return new DefaultCode() {
      @Override
      public void execute(Writer writer, List<Object> scopes) {
        Object object = resolve(scopes, variable);
        if (object != null) {
          try {
            String value = object.toString();
            if (encoded) {
              writer.write(encode(value));
            } else {
              writer.write(value);
            }
          } catch (Exception e) {
            throw new MustacheException("Failed to get value for " + variable + " at line " + line);
          }
        }
        super.execute(writer, scopes);
      }
    };
  }

  @Override
  public Code write(final String text, int line) {
    return new DefaultCode() {
      @Override
      public void execute(Writer writer, List<Object> scopes) {
        try {
          writer.write(text);
        } catch (IOException e) {
          throw new MustacheException();
        }
      }
    };
  }

  @Override
  public Code eof(int line) {
    return EOF;
  }

  @Override
  public Code extend(String variable, List<Code> codes, String file, int start) {
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
