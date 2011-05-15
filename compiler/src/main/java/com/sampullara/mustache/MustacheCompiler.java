package com.sampullara.mustache;

import com.google.common.base.Charsets;
import com.sampullara.util.RuntimeJavaCompiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Compile mustache-like templates to java bytecode
 * <p/>
 * User: sam
 * Date: May 4, 2010
 * Time: 9:59:58 AM
 */
public class MustacheCompiler implements MustacheJava {
  private File root;
  private static String header, middle, footer;
  private Logger logger = Logger.getLogger(getClass().getName());
  private boolean debug = false;
  private String superclass;
  private String outputDirectory = System.getProperty("mustcache");
  private static final String ENCODING = "UTF-8";

  public void setDebug() {
    debug = true;
  }

  static {
    header = getText("/header.txt");
    middle = getText("/middle.txt");
    footer = getText("/footer.txt");
  }

  private static String getText(String template) {
    InputStream stream = MustacheCompiler.class.getResourceAsStream(template);
      BufferedReader br = null;
      try {
          br = new BufferedReader(new InputStreamReader(stream,ENCODING));
          return getText(template, br);
      } catch (UnsupportedEncodingException e) {
          return null;
      }      
  }

  private static String getText(String template, BufferedReader br) {
    StringBuilder text = new StringBuilder();
    String line;
    try {
      while ((line = br.readLine()) != null) {
        text.append(line);
      }
      br.close();
    } catch (IOException e) {
      throw new AssertionError("Failed to read template file: " + template);
    }
    return text.toString();
  }

  public MustacheCompiler() {
    this.root = new File(".");
  }

  public MustacheCompiler(File root) {
    this.root = root;
  }

  public MustacheCompiler(File root, String outputDirectory) {
    this(root);
    this.outputDirectory = outputDirectory;
  }


  public void setSuperclass(String superclass) {
    this.superclass = superclass;
  }

  public void setOutputDirectory(String outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  @Override
  public synchronized Mustache parse(String partial) throws MustacheException {
    AtomicInteger currentLine = new AtomicInteger(0);
    BufferedReader br = new BufferedReader(new StringReader(partial));
    return compile(br, new Stack<String>(), currentLine, null);
  }

  @Override
  public synchronized Mustache parseFile(String path) throws MustacheException {
    AtomicInteger currentLine = new AtomicInteger(0);
    File file = new File(root, path);
    BufferedReader br;
    try {
      br = new BufferedReader(new InputStreamReader(new FileInputStream(file),ENCODING));
    } catch (UnsupportedEncodingException e) {
      throw new MustacheException("UnsupportedEncoding: " + ENCODING);  
    } catch (FileNotFoundException e) {
      throw new MustacheException("Mustache file not found: " + file);
    }
    Mustache result = compile(br, new Stack<String>(), currentLine, null);
    result.setPath(path);
    return result;
  }

  public Mustache compile(Reader br) throws MustacheException {
    return compile(br, new Stack<String>(), new AtomicInteger(0), getClass().getClassLoader());
  }

  public Mustache compile(Reader br, Stack<String> scope, AtomicInteger currentLine, ClassLoader parent) throws MustacheException {
    AtomicInteger num = new AtomicInteger(0);
    Mustache result;
    StringBuilder code = new StringBuilder();
    int startingLines = currentLine.get();
    code.append(" extends ");
    if (superclass == null) {
      code.append("Mustache");
    } else {
      code.append(superclass);
    }
    code.append(" {");
    code.append(middle);
    // Now we grab the mustache template
    String sm = "{{";
    String em = "}}";

    int c;
    try {
      StringBuilder template = new StringBuilder();
      boolean iterable = currentLine.get() != 0;
      currentLine.compareAndSet(0, 1);
      boolean onlywhitespace = true;
      READ:
      while ((c = br.read()) != -1) {
        if (c == '\r') {
          continue;
        }
        // Increment the line
        if (c == '\n') {
          writeText(code, template.toString());
          template = new StringBuilder();
          currentLine.incrementAndGet();
          if (!iterable || (iterable && !onlywhitespace)) {
            code.append("w.write(\"\\n\");\n");
          } else code.append("\n");

          iterable = false;
          onlywhitespace = true;
          continue;
        }
        // Check for a mustache start
        if (c == sm.charAt(0)) {
          br.mark(1);
          if (br.read() == sm.charAt(1)) {
            // Two mustaches, now capture command
            StringBuilder sb = new StringBuilder();
            while ((c = br.read()) != -1) {
              if (c == em.charAt(0)) {
                br.mark(1);
                if (br.read() == em.charAt(1)) {
                  // Matched end
                  break;
                } else {
                  // Only one
                  br.reset();
                }
              }
              sb.append((char) c);
            }
            String command = sb.toString().trim();
            char ch = command.charAt(0);
            switch (ch) {
              case '#':
              case '^':
              case '?':
                // Tag start
                String startTag = sb.substring(1).trim();
                scope.push(startTag);
                int start = currentLine.get();
                Mustache sub = compile(br, scope, currentLine, parent);
                int lines = currentLine.get() - start;

                if (!onlywhitespace || lines == 0) {
                  writeText(code, template.toString());
                }
                template = new StringBuilder();

                ClassLoader classLoader = sub.getClass().getClassLoader();
                if (parent == null || !(parent instanceof RuntimeJavaCompiler.CompilerClassLoader) ||
                        !(classLoader instanceof RuntimeJavaCompiler.CompilerClassLoader)) {
                  parent = classLoader;
                } else {
                  RuntimeJavaCompiler.CompilerClassLoader rcc = (RuntimeJavaCompiler.CompilerClassLoader) parent;
                  rcc.merge((RuntimeJavaCompiler.CompilerClassLoader) classLoader);
                }
                if (debug) {
                  code.append("System.err.println(\"#").append(startTag).append("\");");
                }
                int variableNum = num.incrementAndGet();
                switch(ch) {
                  case '#':
                    code.append("iterable(w, s, \"");
                    code.append(startTag);
                    code.append("\", ").append(sub.getClass().getName()).append(".class);");
                    break;
                  case '^':
                    code.append("for (Scope s").append(variableNum);
                    code.append(":inverted(s, \"");
                    code.append(startTag);
                    code.append("\")) {");
                    code.append("enqueue(w, new ").append(sub.getClass().getName());
                    code.append("(), s").append(variableNum).append(");");
                    code.append("}");
                    break;
                  case '?':
                    code.append("for (Scope s").append(variableNum);
                    code.append(":ifiterable(s, \"");
                    code.append(startTag);
                    code.append("\")) {");
                    code.append("enqueue(w, new ").append(sub.getClass().getName());
                    code.append("(), s").append(variableNum).append(");");
                    code.append("}");
                    break;
                }
                for (int i = 0; i < lines; i++) {
                  code.append("/* sub */\n");
                }
                iterable = lines != 0;
                break;
              case '/':
                if (!onlywhitespace) {
                  writeText(code, template.toString());
                }
                template = new StringBuilder();
                // Tag end
                String endTag = sb.substring(1).trim();
                String expected = scope.pop();
                if (!endTag.equals(expected)) {
                  throw new MustacheException("Mismatched start/end tags: " + expected + " != " + endTag + " at " + currentLine);
                }
                if (debug) {
                  code.append("System.err.println(\"/").append(endTag).append("\");");
                }
                break READ;
              case '>':
                // Partial
                writeText(code, template.toString());
                template = new StringBuilder();
                String partialName = sb.substring(1).trim();
                code.append("partial(w, s, \"").append(partialName).append("\", partial(\"").append(partialName).append("\"));");
                break;
              case '{':
                // Not escaped
                writeText(code, template.toString());
                template = new StringBuilder();
                if (em.charAt(1) != '}' || br.read() == '}') {
                  code.append("write(w, s, \"").append(sb.substring(1).trim()).append("\", false);");
                } else {
                  throw new MustacheException("Unescaped section not terminated properly: " + sb + " at " + currentLine);
                }
                break;
              case '&':
                // Not escaped
                writeText(code, template.toString());
                template = new StringBuilder();
                code.append("write(w, s, \"").append(sb.substring(1).trim()).append("\", false);");
                break;
              case '%':
                writeText(code, template.toString());
                template = new StringBuilder();
                // Pragmas
                logger.warning("Pragmas are unsupported");
                break;
              case '!':
                writeText(code, template.toString());
                template = new StringBuilder();
                // Comment
                break;
              default:
                // Reference
                writeText(code, template.toString());
                template = new StringBuilder();
                code.append("write(w, s, \"").append(command).append("\", true);");
                break;
            }
            continue;
          } else {
            // Only one
            br.reset();
          }
        }
        onlywhitespace = (c == ' ' || c == '\t') && onlywhitespace;
        append(template, (char) c, onlywhitespace);
      }
      writeText(code, template.toString());
      code.append(footer);
    } catch (IOException e) {
      throw new MustacheException("Failed to read: " + e);
    }
    try {
      MessageDigest md = MessageDigest.getInstance("SHA1");
      byte[] digest = md.digest(code.toString().getBytes(Charsets.UTF_8));
      StringBuilder hash = new StringBuilder();
      for (byte aDigest : digest) {
        hash.append(Integer.toHexString(0xFF & aDigest));
      }
      String className = "Mustache" + hash;
      try {
        ClassLoader classLoader = parent == null ? MustacheCompiler.class.getClassLoader() : parent;
        Mustache mustache = (Mustache) classLoader.loadClass("com.sampullara.mustaches." + className).newInstance();
        mustache.setRoot(root);
        mustache.setMustacheJava(this);
        return mustache;
      } catch (Exception e) {
        StringBuilder declaration = new StringBuilder();
        for (int i = 0; i < startingLines; i++) {
          declaration.append("\n");
        }
        declaration.append(header);
        declaration.append(className);
        code.insert(0, declaration);
        if (debug) {
          File dir = new File("src/main/java/com/sampullara/mustaches/");
          dir.mkdirs();
          File file = new File(dir, className + ".java");
          Writer fw = new OutputStreamWriter(new FileOutputStream(file),ENCODING);
          fw.write(code.toString());
          fw.close();
        }
        try {
          RuntimeJavaCompiler.CompilerClassLoader loader = (RuntimeJavaCompiler.CompilerClassLoader) RuntimeJavaCompiler.compile(new PrintWriter(System.out, true), className, code.toString(), parent);
          Class<?> aClass = loader.loadClass("com.sampullara.mustaches." + className);
          if (outputDirectory != null) {
            for (Map.Entry<String, RuntimeJavaCompiler.JavaClassOutput> entry : loader.getJavaClassMap().entrySet()) {
              String outputName = entry.getKey();
              int dot = outputName.lastIndexOf(".");
              String outputPackage = outputName.substring(0, dot);
              File outputDir = new File(outputDirectory, outputPackage.replace(".", "/"));
              outputDir.mkdirs();
              File outputFile = new File(outputDir, outputName.substring(dot + 1) + ".class");
              FileOutputStream fos = new FileOutputStream(outputFile);
              fos.write(entry.getValue().getBytes());
              fos.close();
            }
          }
          result = (Mustache) aClass.newInstance();
          result.setRoot(root);
        } catch (ClassNotFoundException cnfe) {
          logger.info("Compiling: " + className);
          logger.info(String.valueOf(parent));
          throw cnfe;
        }
      }
    } catch (Exception e) {
      throw new MustacheException("Failed to compile code: " + e);
    }
    result.setMustacheJava(this);
    return result;
  }

  protected void append(StringBuilder template, char c, boolean onlywhitespace) {
    template.append(c);
  }

  protected void writeText(StringBuilder sb, String text) {
    if (text.length() != 0) {
      text = text.replace("\\", "\\\\");
      text = text.replace("\"", "\\\"");
      sb.append("w.write(\"").append(text).append("\");");
    }
  }
}
