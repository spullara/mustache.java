package com.sampullara.mustache;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:00:07 AM
 */
public class Compiler {
  private File root;
  private static String header, middle, footer;
  private static AtomicInteger num = new AtomicInteger(0);

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

  public Compiler(File root) {
    this.root = root;
  }

  private Map<File, Mustache> cache = new ConcurrentHashMap<File, Mustache>();

  public synchronized Mustache parse(String path) throws MustacheException {
    File file = new File(root, path);
    Mustache result = cache.get(file);
    if (result == null) {
      try {
        StringBuilder sb = new StringBuilder();
        sb.append(header);
        String className = "Mustache" + num.getAndIncrement();
        sb.append(className);
        sb.append(middle);
        // Now we grab the mustache template
        String startMustache = "{{";
        String endMustache = "}}";
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        int currentline = 0;
        while ((line = br.readLine()) != null) {
          currentline++;
          int last = 0;
          int foundStart;
          boolean tagonly = false;
          line = line.trim();
          while ((foundStart = line.indexOf(startMustache)) != -1) {
            int foundEnd = line.indexOf(endMustache);
            // Look for the 3rd ending mustache
            if (line.length() > foundEnd + 2 && line.charAt(foundEnd + 2) == '}') {
              foundEnd++;
            }
            // Unterminated mustache
            if (foundEnd < foundStart) {
              throw new MustacheException("Found unmatched end mustache: " + currentline + ":" + foundEnd);
            }
            // If there is only a tag on a line, don't insert a newline
            if (foundStart == 0 && foundEnd + endMustache.length() == line.length()) {
              tagonly = true;
            }
            String pre = line.substring(last, foundStart);
            writeText(sb, pre, false);
            String command = line.substring(foundStart + startMustache.length(), foundEnd);
            switch (command.charAt(0)) {
              case '!':
                // Comment, do nothing with the content
                break;
              case '#':
                // Tag start
                System.out.println("Tag start: " + command);
                break;
              case '^':
                // Inverted tag
                System.out.println("Inverted tag start: " + command);
                break;
              case '/':
                // Tag end
                System.out.println("End tag: " + command);
                break;
              case '>':
                // Partial
                System.out.println("Partial: " + command);
                break;
              case '{':
                // Not escaped
                if (command.endsWith("}")) {
                  sb.append("write(w, c, \"").append(command.substring(1, command.length() - 1).trim()).append("\", false);");
                } else {
                  throw new MustacheException("Unescaped section not terminated properly: " + command + " at " + currentline + ":" + foundStart);
                }
                break;
              case '&':
                // Not escaped
                sb.append("write(w, c, \"").append(command.substring(1).trim()).append("\", false);");
                break;
              default:
                // Reference
                sb.append("write(w, c, \"").append(command.trim()).append("\", true);");
                break;
            }
            line = line.substring(foundEnd + endMustache.length());
          }
          if (!tagonly) writeText(sb, last == 0 ? line : line.substring(last), true);
        }
        sb.append(footer);
        ClassLoader loader;
        loader = RuntimeJavaCompiler.compile(new PrintWriter(System.out, true), className, sb.toString());
        Class<?> aClass = loader.loadClass("com.sampullara.mustaches." + className);
        result = (Mustache) aClass.newInstance();
        cache.put(file, result);
      } catch (Exception e) {
        throw new MustacheException(e);
      }
    }
    return result;
  }

  private void writeText(StringBuilder sb, String text, boolean endline) {
    text = text.replaceAll("\"", "\\\"");
    sb.append("w.write(\"").append(text);
    if (endline) sb.append("\\n");
    sb.append("\");\n");
  }
}
