package com.github.mustachejava;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import com.github.mustachejava.codes.DefaultCode;
import com.github.mustachejava.codes.ExtendCode;
import com.github.mustachejava.codes.ExtendNameCode;
import com.github.mustachejava.codes.IterableCode;
import com.github.mustachejava.codes.NotIterableCode;
import com.github.mustachejava.codes.PartialCode;
import com.github.mustachejava.codes.ValueCode;
import com.github.mustachejava.codes.WriteCode;
import com.github.mustachejava.reflect.ReflectionObjectHandler;

/**
 * Simplest possible code factory
 */
public class DefaultMustacheFactory implements MustacheFactory {

  private final MustacheParser mc = new MustacheParser(this);
  private final Map<String, Mustache> templateCache = new ConcurrentHashMap<String, Mustache>();
  private final Map<String, Mustache> mustacheCache = new ConcurrentHashMap<String, Mustache>();
  private ObjectHandler oh = new ReflectionObjectHandler();

  private String resourceRoot;
  private File fileRoot;

  private ListeningExecutorService les;

  public DefaultMustacheFactory() {
  }

  public DefaultMustacheFactory(String resourceRoot) {
    if (!resourceRoot.endsWith("/")) resourceRoot += "/";
    this.resourceRoot = resourceRoot;
  }

  public DefaultMustacheFactory(File fileRoot) {
    if (!fileRoot.exists()) {
      throw new MustacheException(fileRoot + " does not exist");
    }
    if (!fileRoot.isDirectory()) {
      throw new MustacheException(fileRoot + " is not a directory");
    }
    this.fileRoot = fileRoot;
  }

  @Override
  public MustacheVisitor createMustacheVisitor() {
    return new DefaultMustacheVisitor(this);
  }

  @Override
  public Reader getReader(String resourceName) {
    ClassLoader ccl = Thread.currentThread().getContextClassLoader();
    InputStream is = ccl.getResourceAsStream(
            (resourceRoot == null ? "" : resourceRoot) + resourceName);
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
  public void encode(String value, Writer writer) {
    try {
      int position = 0;
      int length = value.length();
      for (int i = 0; i < length; i++) {
        char c = value.charAt(i);
        switch (c) {
          case '&':
            if (!escapedPattern.matcher(value.substring(i, length)).find()) {
              position = append(value, writer, position, i, "&amp;");
            } else {
              if (position != 0) {
                position = append(value, writer, position, i, "&");
              }
            }
            break;
          case '\\':
            position = append(value, writer, position, i, "\\\\");
            break;
          case '"':
            position = append(value, writer, position, i, "&quot;");
            break;
          case '<':
            position = append(value, writer, position, i, "&lt;");
            break;
          case '>':
            position = append(value, writer, position, i, "&gt;");
            break;
          case '\n':
            position = append(value, writer, position, i, "&#10;");
            break;
        }
      }
      writer.append(value, position, length);
    } catch (IOException e) {
      throw new MustacheException("Failed to encode value: " + value);
    }
  }

  private int append(String value, Writer writer, int position, int i, String replace) throws IOException {
    writer.append(value, position, i);
    writer.append(replace);
    return i + 1;
  }

  @Override
  public ObjectHandler getObjectHandler() {
    return oh;
  }

  public void setObjectHandler(ObjectHandler oh) {
    this.oh = oh;
  }

  public ExecutorService getExecutorService() {
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

  @Override
  public synchronized Mustache compile(String name) {
    Mustache mustache = mustacheCache.get(name);
    if (mustache == null) {
      mustache = mc.compile(name);
      mustacheCache.put(name, mustache);
      mustache.init();
    }
    return mustache;
  }

  @Override
  public Mustache compile(Reader reader, String name) {
    return compile(reader, name, "{{", "}}");
  }

  // Template functions need this to comply with the specification
  public Mustache compile(Reader reader, String file, String sm, String em) {
    Mustache compile = mc.compile(reader, file, sm, em);
    compile.init();
    return compile;
  }

  @Override
  public String translate(String from) {
    return from;
  }
}
