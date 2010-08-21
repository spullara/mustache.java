package com.sampullara.mustache;

import com.sampullara.util.RuntimeJavaCompiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
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
public class MustacheCompiler {
  private File root;
  private static String header, middle, footer;
  private static AtomicInteger num = new AtomicInteger(0);
  private Logger logger = Logger.getLogger(getClass().getName());
  private boolean debug = false;
  private String superclass;

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
    BufferedReader br = new BufferedReader(new InputStreamReader(stream));
    return getText(template, br);
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

  public void setSuperclass(String superclass) {
    this.superclass = superclass;
  }

  public synchronized Mustache parse(String partial) throws MustacheException {
    AtomicInteger currentLine = new AtomicInteger(0);
    BufferedReader br = new BufferedReader(new StringReader(partial));
    return compile(br, new Stack<String>(), currentLine, null);
  }

  public synchronized Mustache parseFile(String path) throws MustacheException {
    AtomicInteger currentLine = new AtomicInteger(0);
    File file = new File(root, path);
    BufferedReader br;
    try {
      br = new BufferedReader(new FileReader(file));
    } catch (FileNotFoundException e) {
      throw new MustacheException("Mustache file not found: " + file);
    }
    Mustache result = compile(br, new Stack<String>(), currentLine, null);
    result.setPath(path);
    return result;
  }

  public Mustache compile(BufferedReader br) throws MustacheException {
    return compile(br, new Stack<String>(), new AtomicInteger(0), getClass().getClassLoader());
  }

  public Mustache compile(BufferedReader br, Stack<String> scope, AtomicInteger currentLine, ClassLoader parent) throws MustacheException {
    Mustache result;
    StringBuilder code = new StringBuilder();
    for (int i = 0; i < currentLine.get(); i++) {
      code.append("\n");
    }
    code.append(header);
    String className = "Mustache" + num.getAndIncrement();
    code.append(className);
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

                parent = sub.getClass().getClassLoader();
                if (debug) {
                  code.append("System.err.println(\"#").append(startTag).append("\");");
                }
                int variableNum = num.incrementAndGet();
                code.append("for (Scope s").append(variableNum);
                if (ch == '#') {
                  code.append(":iterable(s, \"");
                } else {
                  code.append(":inverted(s, \"");
                }
                code.append(startTag);
                code.append("\")) {");
                code.append("enqueue(w, new ").append(sub.getClass().getName());
                code.append("(), s").append(variableNum).append(");");
                code.append("}");
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
                code.append("partial(w, s, \"").append(partialName).append("\");");
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
        template.append((char) c);
      }
      writeText(code, template.toString());
      code.append(footer);
    } catch (IOException e) {
      throw new MustacheException("Failed to read: " + e);
    }
    try {
      if (debug) {
        File dir = new File("src/main/java/com/sampullara/mustaches/");
        dir.mkdirs();
        File file = new File(dir, className + ".java");
        FileWriter fw = new FileWriter(file);
        fw.write(code.toString());
        fw.close();
      }
      ClassLoader loader = RuntimeJavaCompiler.compile(new PrintWriter(System.out, true), className, code.toString(), parent);
      Class<?> aClass = loader.loadClass("com.sampullara.mustaches." + className);
      result = (Mustache) aClass.newInstance();
      result.setRoot(root);
    } catch (Exception e) {
      e.printStackTrace();
      throw new MustacheException("Failed to compile code: " + e);
    }
    return result;
  }

  private void writeText(StringBuilder sb, String text) {
    if (text.length() != 0) {
      text = text.replace("\\", "\\\\");
      text = text.replace("\"", "\\\"");
      sb.append("w.write(\"").append(text).append("\");");
    }
  }
}
