package com.sampullara.util;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Compile a class at runtime.  Various fallbacks and assumptions are baked into this class such that if you
 * use a non-standard classloader it may not be able to see the bytes of your classes.
 * User: sam
 * Date: May 3, 2010
 * Time: 9:32:47 AM
 */
public class RuntimeJavaCompiler {

  public static ClassLoader compile(PrintWriter printWriter, String className, String code) throws IOException {
    return compile(printWriter, className, code, null);
  }

  public static ClassLoader compile(PrintWriter printWriter, String className, String code, ClassLoader loader) throws IOException {
    if (loader == null) {
      loader = Thread.currentThread().getContextClassLoader();
    }
    final CompilerClassLoader ccl = new CompilerClassLoader(loader);
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager jfm = compiler.getStandardFileManager(null, null, null);
    ForwardingJavaFileManager<StandardJavaFileManager> fjfm = new ClassLoaderFileManager(jfm, ccl, loader);
    List<JavaFileObject> sources = new ArrayList<JavaFileObject>();
    sources.add(new JavaSourceFromString(className, code));
    JavaCompiler.CompilationTask task = compiler.getTask(printWriter, fjfm, null, null, null, sources);
    task.call();
    return ccl;
  }

  /**
   * Given a file, create an input stream from that file.
   */
  public static class JavaClassFromFile extends SimpleJavaFileObject {
    private File file;

    protected JavaClassFromFile(File file) {
      super(file.toURI(), Kind.CLASS);
      this.file = file;
    }

    @Override
    public InputStream openInputStream() throws IOException {
      return new FileInputStream(file);
    }
  }

  /**
   * Given a Jar Entry, create an input stream from that entry.
   */
  public static class JavaClassFromEntry extends SimpleJavaFileObject {
    private JarFile jarfile;
    private ZipEntry entry;

    protected JavaClassFromEntry(JarFile jarfile, ZipEntry entry) {
      super(URI.create("jar://" + jarfile.getName().toString().replace('\\', '/') + "!" + entry.getName()), Kind.CLASS);
      this.jarfile = jarfile;
      this.entry = entry;
    }

    @Override
    public String getName() {
      return entry.getName();
    }

    @Override
    public InputStream openInputStream() throws IOException {
      return jarfile.getInputStream(entry);
    }
  }

  /**
   * Given a URI and a Kind collect the output of the compiler.
   */
  public static class JavaClassOutput extends SimpleJavaFileObject {
    private ByteArrayOutputStream bytes;

    protected JavaClassOutput(URI uri, Kind kind) {
      super(uri, kind);
    }

    @Override
    public OutputStream openOutputStream() {
      bytes = new ByteArrayOutputStream();
      return bytes;
    }

    public InputStream openInputStream() {
      return new ByteArrayInputStream(bytes.toByteArray());
    }

    public byte[] getBytes() {
      return bytes.toByteArray();
    }

  }

  /**
   * Our class loader can store a set of classes but is otherwise unremarkable.
   */
  public static class CompilerClassLoader extends ClassLoader {
    private Map<String, JavaClassOutput> classes = new HashMap<String, JavaClassOutput>();
    private final ClassLoader parent;

    public Map<String, JavaClassOutput> getJavaClassMap() {
      return classes;
    }

    public Collection<JavaClassOutput> getJavaClasses() {
      return classes.values();
    }

    public CompilerClassLoader(ClassLoader loader) {
      super(loader);
      parent = loader;
    }

    public void merge(CompilerClassLoader merger) {
      classes.putAll(merger.classes);
    }

    public String toString() {
      return parent.toString() + " ->\n  " + classes.toString();
    }

    public void add(String name, JavaClassOutput clazz) {
      classes.put(name, clazz);
    }

    @Override
    protected URL findResource(String s) {
      if (s.startsWith("/")) s = s.substring(1);
      if (s.endsWith(".class")) s = s.substring(0, s.length() - 6);
      s = s.replace('/', '.');
      JavaClassOutput jco = classes.get(s);
      try {
        File tempFile = File.createTempFile("class", "bytes");
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(jco.getBytes());
        fos.close();
        return tempFile.toURI().toURL();
      } catch (IOException e) {
        return null;
      }
    }

    @Override
    protected Class<?> findClass(final String classname)
        throws ClassNotFoundException {
      JavaClassOutput file = classes.get(classname);
      if (file != null) {
        byte[] bytes = file.getBytes();
        return defineClass(classname, bytes, 0, bytes.length);
      }
      try {
        return Class.forName(classname);
      } catch (ClassNotFoundException nf) {
        return super.findClass(classname);
      }
    }
  }

  /**
   * Use the standard file manager along with a little bit of code that is specific to our in-memory
   * classes that we are creating.
   */
  public static class ClassLoaderFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
    private final CompilerClassLoader ccl;
    private final ClassLoader loader;

    public ClassLoaderFileManager(StandardJavaFileManager jfm, CompilerClassLoader ccl, ClassLoader loader) {
      super(jfm);
      this.ccl = ccl;
      this.loader = loader;
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
      String result;
      if (file instanceof JavaClassFromFile || file instanceof JavaClassFromEntry || file instanceof JavaClassOutput) {
        String name = file.getName();
        name = name.substring(0, name.length() - 6).replace('/', '.');
        result = name;
      } else {
        result = super.inferBinaryName(location, file);
      }
      return result;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String classname, JavaFileObject.Kind kind, FileObject fileObject) throws IOException {
      if (kind == JavaFileObject.Kind.CLASS) {
        JavaClassOutput jco =
            new JavaClassOutput(URI.create("memory://" + classname.replace('.', '/') + ".class"), JavaFileObject.Kind.CLASS);
        ccl.add(classname, jco);
        return jco;
      }
      return super.getJavaFileForOutput(location, classname, kind, fileObject);
    }

    // Individual class loaders can change at runtime, mostly due to this class
    private Map<String, List<JavaFileObject>> jfosCache = new ConcurrentHashMap<String, List<JavaFileObject>>();

    // We only need to do this for every package since we don't allow the jars to vary at runtime
    private static Map<String, List<JavaFileObject>> jfosGlobalCache = new ConcurrentHashMap<String, List<JavaFileObject>>();

    /**
     * This is the core of the problems with the compiler API.  Here we have to track down classes from whereever they
     * might be in the classloader hierarchy so the compiler can see them.
     *
     * @param location
     * @param s
     * @param kinds
     * @param b
     * @return
     * @throws IOException
     */
    @Override
    public synchronized Iterable<JavaFileObject> list(Location location, String s, Set<JavaFileObject.Kind> kinds, boolean b) throws IOException {
      String key = location + " " + s + " " + kinds + " " + b;
      List<JavaFileObject> jfos = jfosCache.get(key);
      if (jfos != null) return jfos;
      jfos = new ArrayList<JavaFileObject>();
      if (location.getName().equals("CLASS_PATH")) {
        final String name = s.replace('.', '/');
        Set<URL> urls = new HashSet<URL>();
        // Special handling for our own class loaders
        if (loader instanceof CompilerClassLoader) {
          jfos.addAll(((CompilerClassLoader) loader).getJavaClasses());
          ClassLoader parent = loader;
          while ((parent = parent.getParent()) instanceof CompilerClassLoader) {
            jfos.addAll(((CompilerClassLoader) parent).getJavaClasses());
          }
          if (parent instanceof URLClassLoader) {
            for (Enumeration<URL> loaderurls = ((URLClassLoader) parent).findResources(name); loaderurls.hasMoreElements();) {
              urls.add(loaderurls.nextElement());
            }
          }
        }
        // Add everything from the current loader
        if (loader instanceof URLClassLoader) {
          for (Enumeration<URL> loaderurls = ((URLClassLoader) loader).findResources(name); loaderurls.hasMoreElements();) {
            urls.add(loaderurls.nextElement());
          }
        }
        // Tomcat will sometimes obfuscate the parent classloader entries
        ClassLoader parent = loader.getParent();
        if (parent instanceof URLClassLoader) {
          for (Enumeration<URL> loaderurls = ((URLClassLoader) parent).findResources(name); loaderurls.hasMoreElements();) {
            urls.add(loaderurls.nextElement());
          }
        }
        // This takes all the discovered URLs in the class loaders and compares them with the request
        for (URL url : urls) {
          String filename = url.getFile();
          String protocol = url.getProtocol();
          if ("jar".equals(protocol)) {
            String jarFilename = filename.substring(5);
            jarFilename = jarFilename.substring(0, jarFilename.indexOf("!"));
            List<JavaFileObject> list = jfosGlobalCache.get(jarFilename);
            if (list == null) {
              list = new ArrayList<JavaFileObject>();
              if (new File(jarFilename).exists()) {
                JarFile jar = new JarFile(jarFilename);
                for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
                  JarEntry entry = entries.nextElement();
                  String entryName = entry.getName();
                  if (entryName.startsWith(name) && !entry.isDirectory()) {
                    list.add(new JavaClassFromEntry(jar, entry));
                  }
                }
                // jfosGlobalCache.put(jarFilename, list);
              }
            }
            jfos.addAll(list);
          } else if ("file".equals(protocol)) {
            File file = new File(filename);
            if (file.exists()) {
              if (file.isDirectory()) {
                for (File entry : file.listFiles()) {
                  if (entry.isFile()) jfos.add(new JavaClassFromFile(entry));
                }
              } else if (file.isFile()) {
                jfos.add(new JavaClassFromFile(file));
              }
            }
          }
        }
      }
      // Finally add everything from the super class
      for (JavaFileObject jfo : super.list(location, s, kinds, b)) {
        jfos.add(jfo);
      }
      jfosCache.put(key, jfos);
      return jfos;
    }
  }

  /**
   * This takes the passed in source and provides it to the compiler.
   */
  public static class JavaSourceFromString extends SimpleJavaFileObject {
    private String code;

    public JavaSourceFromString(String name, String code) {
      super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension),
          Kind.SOURCE);
      this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return code;
    }
  }

}
