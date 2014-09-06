package com.github.mustachejava;

import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.resolver.DefaultResolver;
import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.Files;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
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

  protected int recursionLimit = 100;

  private final MustacheResolver mustacheResolver;

  protected ListeningExecutorService les;

  public DefaultMustacheFactory() {
    this.mustacheResolver = new DefaultResolver();
  }

  public DefaultMustacheFactory(MustacheResolver mustacheResolver) {
    this.mustacheResolver = mustacheResolver;
  }

  /**
   * Use the classpath to resolve mustache templates.
   *
   * @param resourceRoot
   */
  public DefaultMustacheFactory(String resourceRoot) {
    this.mustacheResolver = new DefaultResolver(resourceRoot);
  }

  /**
   * Use the file system to resolve mustache templates.
   *
   * @param fileRoot
   */
  public DefaultMustacheFactory(File fileRoot) {
    this.mustacheResolver = new DefaultResolver(fileRoot);
  }

  public String resolvePartialPath(String dir, String name, String extension) {
    String filePath = name;

    // Do not prepend directory if it is already defined
    if (!name.startsWith("/")) {
      filePath = dir + filePath;
    }

    // Do not append extension if it is already defined
    if (!name.endsWith(extension)) {
      filePath = filePath + extension;
    }

    String path = Files.simplifyPath(new File(filePath).getPath());
    return ensureForwardSlash(path);
  }

  private static String ensureForwardSlash(String path) {
    return path.replace('\\', '/');
  }

  @Override
  public MustacheVisitor createMustacheVisitor() {
    return new DefaultMustacheVisitor(this);
  }

  @Override
  public Reader getReader(String resourceName) {
    Reader reader = mustacheResolver.getReader(resourceName);
    if(reader == null) {
      throw new MustacheNotFoundException(resourceName);
    }
    return reader;
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

  private MustacheException handle(Exception e) {
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
    } catch (UncheckedExecutionException e) {
      throw handle(e);
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
   *
   *
   * @param appended
   * @param b
   * @return
   */
  public String filterText(String appended, boolean b) {
    return appended;
  }

  /**
   * Maximum recursion limit for partials.
   */
  public void setRecursionLimit(int recursionLimit) {
    this.recursionLimit = recursionLimit;
  }

  public int getRecursionLimit() {
    return recursionLimit;
  }

  private final ThreadLocal<Map<String, Mustache>> partialCache = new ThreadLocal<Map<String, Mustache>>() {
    @Override
    protected Map<String, Mustache> initialValue() {
      return new HashMap<String, Mustache>();
    }
  };

  /**
   * In order to handle recursion, we need a temporary thread local cache during compilation
   * that is ultimately thrown away after the top level partial is complete.
   *
   * @param s
   * @return
   */
  public Mustache compilePartial(String s) {
    Map<String, Mustache> cache = partialCache.get();
    try {
      Mustache mustache = cache.get(s);
      if (mustache == null) {
        mustache = mc.compile(s);
        cache.put(s, mustache);
        mustache.init();
      }
      return mustache;
    } finally {
      cache.remove(s);
    }
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
      return mc.compile(reader, tc.file(), tc.startChars(), tc.endChars(), tc.startOfLine());
    }
  }

  protected LoadingCache<String, Mustache> createMustacheCache() {
    return CacheBuilder.newBuilder().build(new MustacheCacheLoader());
  }

  protected LoadingCache<FragmentKey, Mustache> createLambdaCache() {
    return CacheBuilder.newBuilder().build(new FragmentCacheLoader());
  }
}
