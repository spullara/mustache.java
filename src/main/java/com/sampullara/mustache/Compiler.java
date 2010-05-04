package com.sampullara.mustache;

import java.io.*;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Compile mustache-like templates to java bytecode
 * <p/>
 * User: sam
 * Date: May 4, 2010
 * Time: 9:59:58 AM
 */
public class Compiler {
  private File root;
  private static String header, middle, footer;
  private static AtomicInteger num = new AtomicInteger(0);
  private Logger logger = Logger.getLogger(getClass().getName());
  public static final int MARK = 1024;

  static {
    header = getText("/header.txt");
    middle = getText("/middle.txt");
    footer = getText("/footer.txt");
  }

  private static String getText(String template) {
    InputStream stream = Compiler.class.getResourceAsStream(template);
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

  public Compiler() {
    this.root = new File(".");
  }

  public Compiler(File root) {
    this.root = root;
  }

  private Map<File, Mustache> filecache = new ConcurrentHashMap<File, Mustache>();
  private Map<String, Mustache> partialcache = new ConcurrentHashMap<String, Mustache>();

  public synchronized Mustache parse(String partial) throws MustacheException {
    AtomicInteger currentLine = new AtomicInteger(0);
    Mustache result = partialcache.get(partial);
    if (result == null) {
      BufferedReader br = new BufferedReader(new StringReader(partial));
      result = compile(br, new Stack<String>(), currentLine, null);
      partialcache.put(partial, result);
    }
    return result;
  }

  public synchronized Mustache parseFile(String path) throws MustacheException {
    AtomicInteger currentLine = new AtomicInteger(0);
    File file = new File(root, path);
    Mustache result = filecache.get(file);
    if (result == null) {
      BufferedReader br;
      try {
        br = new BufferedReader(new FileReader(file));
      } catch (FileNotFoundException e) {
        throw new MustacheException("Mustache file not found: " + file);
      }
      result = compile(br, new Stack<String>(), currentLine, null);
      filecache.put(file, result);
    }
    return result;
  }

  public Mustache compile(BufferedReader br, Stack<String> scope, AtomicInteger currentline, ClassLoader parent) throws MustacheException {
    Mustache result;
    StringBuilder code = new StringBuilder();
    code.append(header);
    String className = "Mustache" + num.getAndIncrement();
    code.append(className);
    code.append(middle);
    // Now we grab the mustache template
    String sm = "{{";
    String em = "}}";

    int c;
    try {
      StringBuilder template = new StringBuilder();
      boolean startOfLine = true;
      READ:
      while ((c = br.read()) != -1) {
        // Increment the line
        if (c == '\n') {
          writeText(code, template.toString(), true);
          template = new StringBuilder();
          currentline.incrementAndGet();
          startOfLine = true;
          continue;
        }
        // Check for a mustache start
        if (c == sm.charAt(0)) {
          br.mark(1);
          if (br.read() == sm.charAt(1)) {
            // Two mustaches, now capture command
            StringBuilder sb = new StringBuilder();
            while ((c = br.read()) != -1) {
              if (c == '\n') currentline.incrementAndGet();
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
            writeText(code, template.toString(), false);
            template = new StringBuilder();
            String command = sb.toString().trim();
            switch (command.charAt(0)) {
              case '#':
                tagonly(br, startOfLine);
                // Tag start
                String startTag = sb.substring(1).trim();
                scope.push(startTag);
                Mustache sub = compile(br, scope, currentline, parent);
                tagonly(br, startOfLine);
                parent = sub.getClass().getClassLoader();
                code.append("for (Scope s").append(num.incrementAndGet());
                code.append(":iterable(s, \"");
                code.append(startTag);
                code.append("\")) {");
                code.append("new ").append(sub.getClass().getName());
                code.append("().execute(w, s").append(num.get()).append(");");
                code.append("}");
                break;
              case '^':
                tagonly(br, startOfLine);
                // Inverted tag
                startTag = sb.substring(1).trim();
                scope.push(startTag);
                sub = compile(br, scope, currentline, parent);
                tagonly(br, startOfLine);
                parent = sub.getClass().getClassLoader();
                code.append("for (Scope s").append(num.incrementAndGet());
                code.append(":inverted(s, \"");
                code.append(startTag);
                code.append("\")) {");
                code.append("new ").append(sub.getClass().getName());
                code.append("().execute(w, s").append(num.get()).append(");");
                code.append("}");
                break;
              case '/':
                br.mark(1);
                if (br.read() == '\n') {
                  writeText(code, "", true);
                } else br.reset();
                // Tag end
                String endTag = sb.substring(1).trim();
                String expected = scope.pop();
                if (!endTag.equals(expected)) {
                  throw new MustacheException("Mismatched start/end tags: " + expected + " != " + endTag + " at " + currentline);
                }
                break READ;
              case '>':
                // Partial
                String partialName = sb.substring(1).trim();
                code.append("partial(w, s, \"").append(partialName).append("\");");
                break;
              case '{':
                // Not escaped
                if (em.charAt(1) != '}' || br.read() == '}') {
                  code.append("write(w, s, \"").append(sb.substring(1).trim()).append("\", false);");
                } else {
                  throw new MustacheException("Unescaped section not terminated properly: " + sb + " at " + currentline);
                }
                break;
              case '&':
                // Not escaped
                code.append("write(w, s, \"").append(sb.substring(1).trim()).append("\", false);");
                break;
              case '%':
                tagonly(br, startOfLine);
                // Pragmas
                logger.warning("Pragmas are unsupported");
                break;
              case '!':
                tagonly(br, startOfLine);
                // Comment
                break;
              default:
                // Reference
                code.append("write(w, s, \"").append(command).append("\", true);");
                break;
            }
            continue;
          } else {
            // Only one
            br.reset();
          }
        }
        startOfLine = false;
        template.append((char) c);
      }
      writeText(code, template.toString(), false);
      code.append(footer);
    } catch (IOException e) {
      throw new MustacheException("Failed to read: " + e);
    }
    try {
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

  private void tagonly(BufferedReader br, boolean startOfLine) throws IOException {
    if (startOfLine) {
      br.mark(1);
      if (br.read() != '\n') {
        br.reset();
      }
    }
  }

  private void writeText(StringBuilder sb, String text, boolean endline) {
    if (text.length() != 0) {
      text = text.replaceAll("\"", "\\\"");
      sb.append("w.write(\"").append(text).append(endline ? "\\n" : "").append("\");");
    } else if (endline) {
      sb.append("w.write(\"\\n\");\n");
    }
  }
}
