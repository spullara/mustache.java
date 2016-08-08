package com.github.mustachejava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The parser generates callbacks into the MustacheFactory to build them. Do not use these
 * directly as you must manage the Mustache object lifecycle as well.
 * <p>
 * User: sam
 * Date: 5/14/11
 * Time: 3:52 PM
 */
public class MustacheParser {
  public static final String DEFAULT_SM = "{{";
  public static final String DEFAULT_EM = "}}";
  private MustacheFactory mf;

  protected MustacheParser(MustacheFactory mf) {
    this.mf = mf;
  }

  public Mustache compile(String file) {
    Reader reader = mf.getReader(file);
    if (reader == null) {
      throw new MustacheNotFoundException(file);
    }
    return compile(reader, file);
  }

  public Mustache compile(Reader reader, String file) {
    return compile(reader, file, DEFAULT_SM, DEFAULT_EM);
  }

  public Mustache compile(Reader reader, String file, String sm, String em) {
    return compile(reader, null, new AtomicInteger(0), file, sm, em, true);
  }

  public Mustache compile(Reader reader, String file, String sm, String em, boolean startOfLine) {
    return compile(reader, null, new AtomicInteger(0), file, sm, em, startOfLine);
  }

  @SuppressWarnings("ConstantConditions") // this method is too complex
  protected Mustache compile(final Reader reader, String tag, final AtomicInteger currentLine, String file, String sm, String em, boolean startOfLine) throws MustacheException {
    if (reader == null) {
      throw new MustacheException("Reader is null");
    }
    Reader br;
    if (reader.markSupported()) {
      br = reader;
    } else {
      br = new BufferedReader(reader);
    }
    try {
      boolean sawCR = false;
      int startLine = currentLine.get();
      MustacheVisitor mv = mf.createMustacheVisitor();
      // Now we grab the mustache template
      boolean onlywhitespace = true;
      // Starting a new line
      boolean iterable = currentLine.get() != 0;
      currentLine.compareAndSet(0, 1);
      StringBuilder out = new StringBuilder();
      try {
        int c;
        while ((c = br.read()) != -1) {
          // We remember that we saw a carriage return so we can put it back in later
          if (c == '\r') {
            sawCR = true;
            continue;
          }
          // Increment the line
          if (c == '\n') {
            currentLine.incrementAndGet();
            if (!iterable || (iterable && !onlywhitespace)) {
              if (sawCR) out.append("\r");
              out.append("\n");
            }
            out = write(mv, out, file, currentLine.intValue(), startOfLine);

            iterable = false;
            onlywhitespace = true;
            startOfLine = true;
            continue;
          }
          sawCR = false;
          // Check for a mustache start
          if (c == sm.charAt(0)) {
            br.mark(1);
            if (sm.length() == 1 || br.read() == sm.charAt(1)) {
              // Two mustaches, now capture command
              StringBuilder sb = new StringBuilder();
              while ((c = br.read()) != -1) {
                br.mark(1);
                if (c == em.charAt(0)) {
                  if (em.length() > 1) {
                    if (br.read() == em.charAt(1)) {
                      // Matched end
                      break;
                    } else {
                      // Only one
                      br.reset();
                    }
                  } else break;
                }
                sb.append((char) c);
              }
              final String command = mf.translate(sb.toString());
              if (command.length() == 0) {
                TemplateContext tc = new TemplateContext(sm, em, file, currentLine.get(), startOfLine);
                throw new MustacheException("Empty mustache", tc);
              }
              final char ch = command.charAt(0);
              final String variable = command.substring(1).trim();
              switch (ch) {
                case '#':
                case '^':
                case '<':
                case '$': {
                  boolean oldStartOfLine = startOfLine;
                  startOfLine = startOfLine & onlywhitespace;
                  int line = currentLine.get();
                  final Mustache mustache = compile(br, variable, currentLine, file, sm, em, startOfLine);
                  int lines = currentLine.get() - line;
                  if (!onlywhitespace || lines == 0) {
                    write(mv, out, file, currentLine.intValue(), oldStartOfLine);
                  }
                  out = new StringBuilder();
                  switch (ch) {
                    case '#':
                      mv.iterable(new TemplateContext(sm, em, file, line, startOfLine), variable, mustache);
                      break;
                    case '^':
                      mv.notIterable(new TemplateContext(sm, em, file, line, startOfLine), variable, mustache);
                      break;
                    case '<':
                      mv.extend(new TemplateContext(sm, em, file, line, startOfLine), variable, mustache);
                      break;
                    case '$':
                      mv.name(new TemplateContext(sm, em, file, line, startOfLine), variable, mustache);
                      break;
                  }
                  iterable = lines != 0;
                  break;
                }
                case '/': {
                  // Tag end
                  if (!startOfLine || !onlywhitespace) {
                    write(mv, out, file, currentLine.intValue(), startOfLine);
                  }
                  if (!variable.equals(tag)) {
                    TemplateContext tc = new TemplateContext(sm, em, file, currentLine.get(), startOfLine);
                    throw new MustacheException(
                            "Mismatched start/end tags: " + tag + " != " + variable + " in " + file + ":" + currentLine, tc);
                  }

                  return mv.mustache(new TemplateContext(sm, em, file, 0, startOfLine));
                }
                case '>': {
                  out = write(mv, out, file, currentLine.intValue(), startOfLine);
                  startOfLine = startOfLine & onlywhitespace;
                  mv.partial(new TemplateContext(sm, em, file, currentLine.get(), startOfLine), variable);
                  break;
                }
                case '{': {
                  out = write(mv, out, file, currentLine.intValue(), startOfLine);
                  // Not escaped
                  String name = variable;
                  if (em.charAt(1) != '}') {
                    name = variable.substring(0, variable.length() - 1);
                  } else {
                    if (br.read() != '}') {
                      TemplateContext tc = new TemplateContext(sm, em, file, currentLine.get(), startOfLine);
                      throw new MustacheException(
                              "Improperly closed variable in " + file + ":" + currentLine, tc);
                    }
                  }
                  mv.value(new TemplateContext(sm, em, file, currentLine.get(), false), name, false);
                  break;
                }
                case '&': {
                  // Not escaped
                  out = write(mv, out, file, currentLine.intValue(), startOfLine);
                  mv.value(new TemplateContext(sm, em, file, currentLine.get(), false), variable, false);
                  break;
                }
                case '%':
                  // Pragmas
                  out = write(mv, out, file, currentLine.intValue(), startOfLine);
                  int index = variable.indexOf(" ");
                  String pragma;
                  String args;
                  if (index == -1) {
                    pragma = variable;
                    args = null;
                  } else {
                    pragma = variable.substring(0, index);
                    args = variable.substring(index + 1);
                  }
                  mv.pragma(new TemplateContext(sm, em, file, currentLine.get(), startOfLine), pragma, args);
                  break;
                case '!':
                  // Comment
                  mv.comment(new TemplateContext(sm, em, file, currentLine.get(), startOfLine), variable);
                  out = write(mv, out, file, currentLine.intValue(), startOfLine);
                  break;
                case '=':
                  // Change delimiters
                  out = write(mv, out, file, currentLine.intValue(), startOfLine);
                  String delimiters = command.replaceAll("\\s+", "");
                  int length = delimiters.length();
                  if (length > 6 || length / 2 * 2 != length) {
                    TemplateContext tc = new TemplateContext(sm, em, file, currentLine.get(), startOfLine);
                    throw new MustacheException("Invalid delimiter string", tc);
                  }
                  sm = delimiters.substring(1, length / 2);
                  em = delimiters.substring(length / 2, length - 1);
                  break;
                default: {
                  if (c == -1) {
                    TemplateContext tc = new TemplateContext(sm, em, file, currentLine.get(), startOfLine);
                    throw new MustacheException("Improperly closed variable", tc);
                  }
                  // Reference
                  out = write(mv, out, file, currentLine.intValue(), startOfLine);
                  mv.value(new TemplateContext(sm, em, file, currentLine.get(), false), command.trim(), true);
                  break;
                }
              }
              // Additional text is no longer at the start of the line
              startOfLine = false;
              continue;
            } else {
              // Only one
              br.reset();
            }
          }
          onlywhitespace = onlywhitespace && (c == ' ' || c == '\t' || c == '\r');
          out.append((char) c);
        }
        write(mv, out, file, currentLine.intValue(), startOfLine);
        if (tag == null) {
          br.close();
        } else {
          TemplateContext tc = new TemplateContext(sm, em, file, startLine, startOfLine);
          throw new MustacheException("Failed to close '" + tag + "' tag", tc);
        }
      } catch (IOException e) {
        TemplateContext tc = new TemplateContext(sm, em, file, currentLine.get(), startOfLine);
        throw new MustacheException("Failed to read", tc);
      }
      mv.eof(new TemplateContext(sm, em, file, currentLine.get(), startOfLine));
      return mv.mustache(new TemplateContext(sm, em, file, 0, startOfLine));
    } catch (MustacheException me) {
      try {
        // We're going to blow the whole stack of compilation and
        // close the readers on the way up.
        br.close();
      } catch (IOException e) {
        // Ignore IOExceptions on close
      }
      throw me;
    }

  }

  /**
   * Ignore empty strings and append to the previous code if it was also a write.
   */
  private StringBuilder write(MustacheVisitor mv, StringBuilder out, String file, int line, boolean startOfLine) {
    String text = out.toString();
    mv.write(new TemplateContext(null, null, file, line, startOfLine), text);
    return new StringBuilder();
  }

}
