package com.github.mustachejava;

import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * Simplest possible code factory
 */
public class DefaultMustacheFactory implements MustacheFactory {

  private final MustacheParser mc = new MustacheParser(this);
  private final Map<String, Mustache> templateCache = new ConcurrentHashMap<String, Mustache>();
  private final LoadingCache<String, Mustache> mustacheCache = CacheBuilder.newBuilder().build(
          new CacheLoader<String, Mustache>() {
            @Override
            public Mustache load(String key) throws Exception {
              return mc.compile(key);
            }
          });
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

  // Override this in a super class if you don't want encoding or would like
  // to change the way encoding works.
  @Override
  public void encode(String value, Writer writer) {
    try {
      int position = 0;
      int length = value.length();
      for (int i = 0; i < length; i++) {
        char c = value.charAt(i);
        switch (c) {
          case '&':
            // If we match an entity or char ref then keep it
            // as is in the text. Otherwise, replace it.
            if (matchesEntityRef(i + 1, length, value)) {
              // If we are at the beginning we can just keep going
              if (position != 0) {
                position = append(value, writer, position, i, "&");
              }
            } else {
              position = append(value, writer, position, i, "&amp;");
            }
            break;
          case '<':
            position = append(value, writer, position, i, "&lt;");
            break;
          case '>':
            position = append(value, writer, position, i, "&gt;");
            break;
          case '"':
            position = append(value, writer, position, i, "&quot;");
            break;
          case '\'':
            position = append(value, writer, position, i, "&#39;");
            break;
          case '/':
            position = append(value, writer, position, i, "&#x2F;");
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
    // Append the clean text
    writer.append(value, position, i);
    // Append the encoded value
    writer.append(replace);
    // and advance the position past it
    return i + 1;
  }

  // Matches all HTML named and character entity references
  private boolean matchesEntityRef(int position, int length, String value) {
    for (int i = position; i < length; i++) {
      char c = value.charAt(i);
      switch (c) {
        case ';':
          // End of the entity
          return i == position;
        case ':':
        case '_':
          // Can appear at the start
          continue;
        case '-':
        case '.':
          // Can only appear in the middle
          if (i == position) {
            return false;
          }
          continue;
        case '#':
          // Switch to char reference
          return i == position && matchesCharRef(i + 1, length, value);
        default:
          // Letters can be at the start
          if (c >= 'a' && c <= 'z') continue;
          if (c >= 'A' && c <= 'Z') continue;
          if (i != position) {
            // Can only appear in the middle
            if (c >= '0' && c <= '9') continue;
          }
          return false;
      }
    }
    // Didn't find ending ;
    return false;
  }

  private boolean matchesCharRef(int position, int length, String value) {
    for (int i = position; i < length; i++) {
      char c = value.charAt(i);
      if (c == ';') {
        return i == position;
      } else if (c < '0' || c > '9') {
        return false;
      }
    }
    return false;
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
  public Mustache compile(String name) {
    try {
      Mustache mustache = mustacheCache.get(name);
      mustache.init();
      return mustache;
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof MustacheException) {
        throw (MustacheException) cause;
      }
      throw new MustacheException(cause);
    }
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
