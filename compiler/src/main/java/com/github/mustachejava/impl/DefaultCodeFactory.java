package com.github.mustachejava.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Function;

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

  private MustacheCompiler mc = new MustacheCompiler(this);
  private Map<String, Mustache> templateCache = new HashMap<String, Mustache>();
  private ObjectHandler oh = new DefaultObjectHandler();
  private String root;

  public DefaultCodeFactory() {
    root = "";
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
  public Code iterable(final String variable, List<Code> codes, final String file, final int start, String sm, String em) {
    return new DefaultCode(codes.toArray(new Code[0]), variable, "#", sm, em) {
      @Override
      public void execute(Writer writer, List<Object> scopes) {
        Object resolve = resolve(scopes, variable);
        if (resolve != null) {
          if (resolve instanceof Function) {
            Function f = (Function) resolve;
            StringWriter sw = new StringWriter();
            runIdentity(sw);
            Object newtemplate = f.apply(sw.toString());
            if (newtemplate != null) {
              String templateText = newtemplate.toString();
              Mustache mustache = templateCache.get(templateText);
              if (mustache == null) {
                mustache = mc.compile(new StringReader(templateText), file, sm, em);
                templateCache.put(templateText, mustache);
              }
              mustache.execute(writer, scopes);
            }
          } else {
            for (Iterator i = oh.iterate(resolve); i.hasNext(); ) {
              Object next = i.next();
              List<Object> iteratorScopes = scopes;
              if (next != null) {
                iteratorScopes = new ArrayList<Object>(scopes);
                iteratorScopes.add(next);
              }
              runCodes(writer, iteratorScopes);
            }
          }
        }
        appendText(writer);
      }
    };
  }

  @Override
  public Code notIterable(final String variable, List<Code> codes, String file, int start, String sm, String em) {
    return new DefaultCode(codes.toArray(new Code[0]), variable, "^", sm, em) {
      @Override
      public void execute(Writer writer, List<Object> scopes) {
        Object resolve = resolve(scopes, variable);
        if (resolve != null) {
          Iterator i = oh.iterate(resolve);
          if (!i.hasNext()) {
            runCodes(writer, scopes);
          }
        } else {
          runCodes(writer, scopes);
        }
        appendText(writer);
      }
    };
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
    return new DefaultCode(null, variable, "", sm, em) {
      @Override
      public void execute(Writer writer, List<Object> scopes) {
        Object object = resolve(scopes, variable);
        if (object != null) {
          try {
            String value;
            if (object instanceof Function) {
              Function f = (Function) object;
              Object newtemplate = f.apply(null);
              if (newtemplate != null) {
                String templateText = newtemplate.toString();
                Mustache mustache = templateCache.get(templateText);
                if (mustache == null) {
                  mustache = mc.compile(new StringReader(templateText), variable,
                          MustacheCompiler.DEFAULT_SM, MustacheCompiler.DEFAULT_EM);
                  templateCache.put(templateText, mustache);
                }
                StringWriter sw = new StringWriter();
                mustache.execute(sw, scopes);
                value = sw.toString();
              } else {
                value = "";
              }              
            } else {
              value = object.toString();
            }
            if (encoded) {
              writer.write(encode(value));
            } else {
              writer.write(value);
            }
          } catch (Exception e) {
            throw new MustacheException("Failed to get value for " + variable + " at line " + line, e);
          }
        }
        super.execute(writer, scopes);
      }
    };
  }

  @Override
  public Code write(final String text, int line, String sm, String em) {
    return new DefaultCode() {
      @Override
      public void identity(Writer writer) {
        execute(writer, null);
      }

      @Override
      public void execute(Writer writer, List<Object> scopes) {
        try {
          writer.write(text);
        } catch (IOException e) {
          throw new MustacheException();
        }
        appendText(writer);
      }
    };
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
