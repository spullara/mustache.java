package com.github.mustachejava;

import com.github.mustachejava.codes.DefaultMustache;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.resolver.DefaultResolver;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static com.github.mustachejava.util.HtmlEscaper.escape;

/**
 * Simplest possible code factory
 */
public class DefaultMustacheFactory implements MustacheFactory {

  /**
   * Create the default cache for mustache compilations. This is basically
   * required by the specification to handle recursive templates.
   */
  protected final ConcurrentHashMap<String, Mustache> mustacheCache = createMustacheCache();

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
  protected final ConcurrentHashMap<FragmentKey, Mustache> templateCache = createLambdaCache();

  protected int recursionLimit = 100;

  private final MustacheResolver mustacheResolver;

  protected ExecutorService es;

  public DefaultMustacheFactory() {
    this.mustacheResolver = new DefaultResolver();
  }

  public DefaultMustacheFactory(MustacheResolver mustacheResolver) {
    this.mustacheResolver = mustacheResolver;
  }

  /**
   * Use the classpath to resolve mustache templates.
   *
   * @param classpathResourceRoot the location in the resources where templates are stored
   */
  public DefaultMustacheFactory(String classpathResourceRoot) {
    this.mustacheResolver = new DefaultResolver(classpathResourceRoot);
  }

  /**
   * Use the file system to resolve mustache templates.
   *
   * @param fileRoot the root of the file system where templates are stored
   */
  public DefaultMustacheFactory(File fileRoot) {
    this.mustacheResolver = new DefaultResolver(fileRoot);
  }

  /**
   * Using the directory, namd and extension, resolve a partial to a name.
   *
   * @param dir
   * @param name
   * @param extension
   * @return
   */
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

    String path = new File(filePath).getPath();
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
    if (reader == null) {
      throw new MustacheNotFoundException(resourceName);
    }
    return reader;
  }

  @Override
  public void encode(String value, Writer writer) {
    escape(value, writer);
  }

  @Override
  public ObjectHandler getObjectHandler() {
    return oh;
  }

  /**
   * You can override the default object handler post construction.
   *
   * @param oh The object handler to use.
   */
  public void setObjectHandler(ObjectHandler oh) {
    this.oh = oh;
  }

  /**
   * There is an ExecutorService that is used when executing parallel
   * operations when a Callable is returned from a mustache value or iterable.
   *
   * @return the executor service
   */
  public ExecutorService getExecutorService() {
    return es;
  }

  /**
   * If you need to specify your own executor service you can.
   *
   * @param es The executor service to use for Future evaluation
   */
  public void setExecutorService(ExecutorService es) {
    this.es = es;
  }

  public Mustache getFragment(FragmentKey templateKey) {
    Mustache mustache = templateCache.computeIfAbsent(templateKey, getFragmentCacheFunction());
    mustache.init();
    return mustache;
  }

  protected Function<FragmentKey, Mustache> getFragmentCacheFunction() {
    return (fragmentKey) -> {
      StringReader reader = new StringReader(fragmentKey.templateText);
      TemplateContext tc = fragmentKey.tc;
      return mc.compile(reader, tc.file(), tc.startChars(), tc.endChars(), tc.startOfLine());
    };
  }

  @Override
  public Mustache compile(String name) {
    Mustache mustache = mustacheCache.computeIfAbsent(name, getMustacheCacheFunction());
    mustache.init();
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
    partialCache.remove();
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
   * @param appended The text to be appended to the output
   * @param startOfLine Are we at the start of the line?
   * @return the filtered string
   */
  public String filterText(String appended, boolean startOfLine) {
    return appended;
  }

  /**
   * Maximum recursion limit for partials.
   * 
   * @param recursionLimit the number of recursions we will attempt before failing
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
      return new HashMap<>();
    }
  };

  /**
   * In order to handle recursion, we need a temporary thread local cache during compilation
   * that is ultimately thrown away after the top level partial is complete.
   *
   * @param s the name of the partial to compile
   * @return the compiled partial
   */
  public Mustache compilePartial(String s) {
    final Map<String, Mustache> cache = partialCache.get();
    final Mustache cached = cache.get(s);
    if (cached != null) {
      // Our implementation supports this but I
      // don't think it makes sense in the interface
      if (cached instanceof DefaultMustache) {
        ((DefaultMustache)cached).setRecursive();
      }
      return cached;
    }
    try {
      final Mustache mustache = mc.compile(s);
      cache.put(s, mustache);
      mustache.init();
      return mustache;
    } finally {
      cache.remove(s);
    }
  }

  protected Function<String, Mustache> getMustacheCacheFunction() {
    return mc::compile;
  }

  protected ConcurrentHashMap<String, Mustache> createMustacheCache() {
    return new ConcurrentHashMap<>();
  }

  protected ConcurrentHashMap<FragmentKey, Mustache> createLambdaCache() {
    return new ConcurrentHashMap<>();
  }
}
