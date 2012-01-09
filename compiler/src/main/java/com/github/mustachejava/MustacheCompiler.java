package com.github.mustachejava;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A pseudo interpreter / compiler. Instead of compiling to Java code, it compiles to a
 * list of instructions to execute.
 * <p/>
 * User: sam
 * Date: 5/14/11
 * Time: 3:52 PM
 */
public class MustacheCompiler {
  private CodeFactory cf;

  public MustacheCompiler(CodeFactory cf) {
    this.cf = cf;
  }

  public List<Code> compile(final Mustache m, final Reader br, String tag, final AtomicInteger currentLine, String file) throws MustacheException {
    final List<Code> list = new LinkedList<Code>();

    // Now we grab the mustache template
    String sm = "{{";
    String em = "}}";

    int c;
    boolean onlywhitespace = true;
    boolean iterable = currentLine.get() != 0;
    currentLine.compareAndSet(0, 1);
    StringBuilder out = new StringBuilder();
    try {
      while ((c = br.read()) != -1) {
        if (c == '\r') {
          continue;
        }
        // Increment the line
        if (c == '\n') {
          currentLine.incrementAndGet();
          if (!iterable || (iterable && !onlywhitespace)) {
            out.append("\n");
          }
          out = write(list, out, currentLine.intValue());

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
              br.mark(1);
              if (c == em.charAt(0)) {
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
            final String command = sb.toString();
            final char ch = command.charAt(0);
            final String variable = command.substring(1).trim();
            switch (ch) {
              case '#':
              case '^':
              case '<':
              case '$':
              case '=': {
                int start = currentLine.get();
                final List<Code> codes = compile(m, br, variable, currentLine, file);
                int lines = currentLine.get() - start;
                if (!onlywhitespace || lines == 0) {
                  write(list, out, currentLine.intValue());
                }
                out = new StringBuilder();
                switch (ch) {
                  case '#':
                    list.add(cf.iterable(m, variable, codes, file, start));
                    break;
                  case '^':
                    list.add(cf.notIterable(m, variable, codes, file, start));
                    break;
                  case '<':
                    list.add(cf.extend(m, variable, codes, file, start));
                    break;
                  case '$':
                    list.add(cf.name(m, variable, codes, file, start));
                    break;
                }
                iterable = lines != 0;
                break;
              }
              case '/': {
                // Tag end
                if (!onlywhitespace) {
                  write(list, out, currentLine.intValue());
                }
                if (!variable.equals(tag)) {
                  throw new MustacheException(
                          "Mismatched start/end tags: " + tag + " != " + variable + " in " + file + ":" + currentLine);
                }

                return list;
              }
              case '>': {
                out = write(list, out, currentLine.intValue());
                list.add(cf.partial(m, variable, file, currentLine.get()));
                break;
              }
              case '{': {
                out = write(list, out, currentLine.intValue());
                // Not escaped
                String name = variable;
                if (em.charAt(1) != '}') {
                  name = variable.substring(0, variable.length() - 1);
                } else {
                  if (br.read() != '}') {
                    throw new MustacheException(
                            "Improperly closed variable in " + file + ":" + currentLine);
                  }
                }
                final String finalName = name;
                list.add(cf.value(m, finalName, false, currentLine.intValue()));
                break;
              }
              case '&': {
                // Not escaped
                out = write(list, out, currentLine.intValue());
                list.add(cf.value(m, variable, false, currentLine.intValue()));
                break;
              }
              case '%':
                // Pragmas
                out = write(list, out, currentLine.intValue());
                break;
              case '!':
                // Comment
                out = write(list, out, currentLine.intValue());
                break;
              default: {
                if (c == -1) {
                  throw new MustacheException(
                          "Improperly closed variable in " + file + ":" + currentLine);
                }
                // Reference
                out = write(list, out, currentLine.intValue());
                list.add(cf.value(m, command.trim(), true, currentLine.intValue()));
                break;
              }
            }
            continue;
          } else {
            // Only one
            br.reset();
          }
        }
        onlywhitespace = (c == ' ' || c == '\t') && onlywhitespace;
        out.append((char) c);
      }
      write(list, out, currentLine.intValue());
      br.close();
    } catch (IOException e) {
      throw new MustacheException("Failed to read", e);
    }
    list.add(cf.eof(currentLine.intValue()));
    return list;
  }

  /**
   * Ignore empty strings and append to the previous code if it was also a write.
   */
  private StringBuilder write(List<Code> list, StringBuilder out, int line) {
    String text = out.toString();
    if (text.length() > 0) {
      int size = list.size();
      Code code;
      if (size > 0) {
        code = list.get(size - 1);
        code.append(text);
      } else {
        list.add(cf.write(text, line));
      }
    }
    return new StringBuilder();
  }

}
