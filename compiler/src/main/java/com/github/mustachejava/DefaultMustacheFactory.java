package com.github.mustachejava;

import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static com.github.mustachejava.util.HtmlEscaper.escape;

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
          is = new FileInputStream(file);
        } catch (FileNotFoundException e) {
          throw new MustacheException("Found file, could not open: " + file, e);
        }
      }
    }
    if (is == null) {
      throw new MustacheException("Template '" + resourceName + "' not found");
    } else {
      return new BufferedReader(new InputStreamReader(is, "UTF-8"));
    }
  }

  // Override this in a super class if you don't want encoding or would like
  // to change the way encoding works.
  @Override
  public void encode(String value, Writer writer) {
    escape(value, writer);
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
