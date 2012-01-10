package com.github.mustachejava.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
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

  private String resourceRoot;
  private File fileRoot;

  public DefaultCodeFactory() {
  }

  public DefaultCodeFactory(String resourceRoot) {
    if (!resourceRoot.endsWith("/")) resourceRoot += "/";
    this.resourceRoot = resourceRoot;
  }

  public DefaultCodeFactory(File fileRoot) {
    if (!fileRoot.exists()) {
      throw new MustacheException(fileRoot + " does not exist");
    }
    if (!fileRoot.isDirectory()) {
      throw new MustacheException(fileRoot + " is not a directory");
    }
    this.fileRoot = fileRoot;
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
  public Code partial(final String variable, String file, int line, String sm, String em) {
    // Use the  name of the parent to get the name of the partial
    int index = file.lastIndexOf(".");
    final String extension = index == -1 ? "" : file.substring(index);
    return new DefaultCode(oh, null, variable, ">", sm, em) {
      private Mustache partial;

      @Override
      public void execute(Writer writer, Object... scopes) {
        if (partial == null) {
          partial = mc.compile(variable + extension);
        }
        Object scope = get(variable, scopes);
        Object[] newscopes = new Object[scopes.length + 1];
        System.arraycopy(scopes, 0, newscopes, 0, scopes.length);
        newscopes[scopes.length] = scope;
        partial.execute(writer, newscopes);
        appendText(writer);
      }
    };
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
  public Reader getReader(String resourceName) {
    ClassLoader ccl = Thread.currentThread().getContextClassLoader();
    InputStream is = ccl.getResourceAsStream((resourceRoot == null ? "" : resourceRoot) + resourceName);
    if (is == null) {
      File file = fileRoot == null ? new File(resourceName) : new File(fileRoot, resourceName);
      if (file.exists() && file.isFile()) {
        try {
          return new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
          throw new MustacheException("Found file, could not open: " + file, e);
        }
      }
      throw new MustacheException("Template " + resourceName + " not found");
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

  @Override
  public ObjectHandler getObjectHandler() {
    return oh;
  }

}
