package com.github.mustachejava;

import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static com.github.mustachejava.util.HtmlEscaper.escape;

/**
 * Simplest possible code factory
 */
public class DefaultMustacheFactory implements MustacheFactory {

  /**
   * Create the default cache for mustache compilations. This is basically
   * required by the specification to handle recursive templates.
   */
  protected final LoadingCache<String, Mustache> mustacheCache = createMustacheCache();

  /**
   * This is the default object handler.
   */
  protected ObjectHandler oh = new ReflectionObjectHandler();

  /**
   * This parser should work with any MustacheFactory
   */
  protected final MustacheParser mc = new MustacheParser(this);

  /**
   * New templates that are generated at runtime are cached here. The template key
   * includes the text of the template and the context so we get proper error
   * messages and debugging information.
   */
  protected final LoadingCache<FragmentKey, Mustache> templateCache = createLambdaCache();

  private final String resourceRoot;
  private final File fileRoot;

  protected ListeningExecutorService les;

  public DefaultMustacheFactory() {
    this.resourceRoot = null;
    this.fileRoot = null;
  }

  /**
   * Use the classpath to resolve mustache templates.
   *
   * @param resourceRoot
   */
  public DefaultMustacheFactory(String resourceRoot) {
    if (!resourceRoot.endsWith("/")) resourceRoot += "/";
    this.resourceRoot = resourceRoot;
    this.fileRoot = null;
  }

  /**
   * Use the file system to resolve mustache templates.
   *
   * @param fileRoot
   */
  public DefaultMustacheFactory(File fileRoot) {
    if (!fileRoot.exists()) {
      throw new MustacheException(fileRoot + " does not exist");
    }
    if (!fileRoot.isDirectory()) {
      throw new MustacheException(fileRoot + " is not a directory");
    }
    this.fileRoot = fileRoot;
    this.resourceRoot = null;
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
      return new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
    }
  }

  @Override
  public void encode(String value, Writer writer) {
    escape(value, writer, true);
  }

  @Override
  public ObjectHandler getObjectHandler() {
    return oh;
  }

  /**
   * You can override the default object handler post construction.
   *
   * @param oh
   */
  public void setObjectHandler(ObjectHandler oh) {
    this.oh = oh;
  }

  /**
   * There is an ExecutorService that is used when executing parallel
   * operations when a Callable is returned from a mustache value or iterable.
   *
   * @return
   */
  public ExecutorService getExecutorService() {
    return les;
  }

  /**
   * If you need to specify your own executor service you can.
   *
   * @param es
   */
  public void setExecutorService(ExecutorService es) {
    if (es instanceof ListeningExecutorService) {
      les = (ListeningExecutorService) es;
    } else {
      les = MoreExecutors.listeningDecorator(es);
    }
  }

  public Mustache getFragment(FragmentKey templateKey) {
    try {
      Mustache mustache = templateCache.get(templateKey);
      mustache.init();
      return mustache;
    } catch (ExecutionException e) {
      throw handle(e);
    }
  }

  private MustacheException handle(ExecutionException e) {
    Throwable cause = e.getCause();
    if (cause instanceof MustacheException) {
      return (MustacheException) cause;
    }
    return new MustacheException(cause);
  }

  @Override
  public Mustache compile(String name) {
    try {
      Mustache mustache = mustacheCache.get(name);
      mustache.init();
      return mustache;
    } catch (ExecutionException e) {
      throw handle(e);
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

  /**
   * Override this method to apply any filtering to text that will appear
   * verbatim in the output template.
   * @param appended
   * @return
   */
  public String filterText(String appended) {
    return appended;
  }

  protected class MustacheCacheLoader extends CacheLoader<String, Mustache> {
    @Override
    public Mustache load(String key) throws Exception {
      return mc.compile(key);
    }
  }

  protected class FragmentCacheLoader extends CacheLoader<FragmentKey, Mustache> {
    @Override
    public Mustache load(FragmentKey fragmentKey) throws Exception {
      StringReader reader = new StringReader(fragmentKey.templateText);
      TemplateContext tc = fragmentKey.tc;
      return mc.compile(reader, tc.file(), tc.startChars(), tc.endChars());
    }
  }

  protected LoadingCache<String, Mustache> createMustacheCache() {
    return CacheBuilder.newBuilder().build(new MustacheCacheLoader());
  }

  protected LoadingCache<FragmentKey, Mustache> createLambdaCache() {
    return CacheBuilder.newBuilder().build(new FragmentCacheLoader());
  }
}
