package com.sampullara.mustache;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Compile a class at runtime.
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

  public static class JavaClassFromFile extends SimpleJavaFileObject {
    private File file;

    protected JavaClassFromFile(File file) {
      super(URI.create("file://" + file.toString()), Kind.CLASS);
      this.file = file;
    }

    @Override
    public InputStream openInputStream() throws IOException {
      return new FileInputStream(file);
    }
  }

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

    byte[] getBytes() {
      return bytes.toByteArray();
    }

  }

  public static class CompilerClassLoader extends ClassLoader {
    private Map<String, JavaClassOutput> classes = new HashMap<String, JavaClassOutput>();

    public Collection<JavaClassOutput> getJavaClasses() {
      return classes.values();
    }

    public CompilerClassLoader(ClassLoader loader) {
      super(loader);
    }

    public void add(String name, JavaClassOutput clazz) {
      classes.put(name, clazz);
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

    @Override
    public Iterable<JavaFileObject> list(Location location, String s, Set<JavaFileObject.Kind> kinds, boolean b) throws IOException {
      List<JavaFileObject> jfos = new ArrayList<JavaFileObject>();
      if (location.getName().equals("CLASS_PATH")) {
        final String name = s.replace('.', '/');
        Set<URL> urls = new HashSet<URL>();
        if (loader instanceof URLClassLoader) {
          for (Enumeration<URL> loaderurls = ((URLClassLoader)loader).findResources(name); loaderurls.hasMoreElements();) {
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
        if (loader instanceof CompilerClassLoader) {
          jfos.addAll(((CompilerClassLoader)loader).getJavaClasses());
        }
        for (URL url : urls) {
          String filename = url.getFile();
          String protocol = url.getProtocol();
          if ("jar".equals(protocol)) {
            String jarFilename = filename.substring(5);
            jarFilename = jarFilename.substring(0, jarFilename.indexOf("!"));
            if (new File(jarFilename).exists()) {
              JarFile jar = new JarFile(jarFilename);
              for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.startsWith(name) && !entry.isDirectory()) {
                  jfos.add(new JavaClassFromEntry(jar, entry));
                }
              }
            }
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
      for (JavaFileObject jfo : super.list(location, s, kinds, b)) {
        jfos.add(jfo);
      }
      return jfos;
    }
  }

  /**
   * A file object used to represent source coming from a string.
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

  public static class JavaClassFromEntry extends SimpleJavaFileObject {
    private JarFile jarfile;
    private ZipEntry entry;

    protected JavaClassFromEntry(JarFile jarfile, ZipEntry entry) {
      super(URI.create("jar://" + jarfile.getName() + "!" + entry.getName()), Kind.CLASS);
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
}
