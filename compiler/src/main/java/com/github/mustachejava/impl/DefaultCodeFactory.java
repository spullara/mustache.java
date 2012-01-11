package com.github.mustachejava.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

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

  private static final Code EOF = new DefaultCode();

  private final MustacheCompiler mc = new MustacheCompiler(this);
  private final Map<String, Mustache> templateCache = new MapMaker().weakKeys().makeMap();
  private final ObjectHandler oh = new DefaultObjectHandler();

  private String resourceRoot;
  private File fileRoot;

  private ListeningExecutorService les;
  
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
    return new PartialCode(this, variable, sm, em, extension);
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

  public ListeningExecutorService getListeningExecutorService() {
    return les;
  }

  public void setExecutorService(ExecutorService es) {
    if (es instanceof ListeningExecutorService) {
      les = (ListeningExecutorService) es;
    } else {
      les = MoreExecutors.listeningDecorator(es);
    }
  }
  
  public Mustache getTemplate(String templateText) {
    return templateCache.get(templateText);
  }
  
  public void putTemplate(String templateText, Mustache mustache) {
    templateCache.put(templateText, mustache);
  }
  
  public Mustache compile(String name) {
    return mc.compile(name);
  }
  
  public Mustache compile(Reader reader, String file, String sm, String em) {
    return mc.compile(reader, file, sm, em);
  }
}
