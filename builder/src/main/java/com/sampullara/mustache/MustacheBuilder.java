package com.sampullara.mustache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Charsets;

/**
 * A pseudo interpreter / compiler. Instead of compiling to Java code, it compiles to a
 * list of instructions to execute.
 * <p/>
 * User: sam
 * Date: 5/14/11
 * Time: 3:52 PM
 */
public class MustacheBuilder implements MustacheJava {

  private Class<? extends Mustache> superclass;
  private CodeFactory cf = new BuilderCodeFactory();
  private MustacheContext mc;

  public MustacheBuilder() {
    this((String)null);
  }

  public MustacheBuilder(final String classpath) {
    this(new MustacheContext() {
      @Override
      public BufferedReader getReader(String path) throws MustacheException {
        String fullPath = classpath == null ? path : classpath + "/" + path;
        InputStream resourceAsStream =
                MustacheBuilder.class.getClassLoader().getResourceAsStream(fullPath);
        if (resourceAsStream == null) {
          throw new MustacheException(path + " not found in classpath");
        }
        return new BufferedReader(new InputStreamReader(resourceAsStream, Charsets.UTF_8));
      }
    });
  }

  public MustacheBuilder(final File root) {
    this(new MustacheContext() {
      @Override
      public BufferedReader getReader(String path) throws MustacheException {
        try {
          return new BufferedReader(new FileReader(new File(root, path)));
        } catch (FileNotFoundException e) {
          throw new MustacheException("Failed to find path: " + path, e);
        }
      }
    });
  }

  public MustacheBuilder(MustacheContext mc) {
    this.mc = mc;
  }

  public void setSuperclass(String superclass) {
    try {
      this.superclass = (Class<? extends Mustache>) Class.forName(superclass);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid class", e);
    }
  }

  public Mustache parse(String template, String path) throws MustacheException {
    return build(new StringReader(template), path);
  }

  public Mustache build(final Reader br, String path) throws MustacheException {
    Mustache mustache;
    try {
      mustache = superclass == null ? new Mustache() : superclass.newInstance();
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not instantiate", e);
    }
    mustache.setMustacheJava(this);
    mustache.setName(path);
    mustache.setCompiled(compile(mustache, br, null, new AtomicInteger(0), path));
    return mustache;
  }

  public Mustache parseFile(String path) throws MustacheException {
    return build(mc.getReader(path), path);
  }

  protected List<Code> compile(final Mustache m, final Reader br, String tag, final AtomicInteger currentLine, String file) throws MustacheException {
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
            final String command = sb.toString().trim();
            final char ch = command.charAt(0);
            final String variable = command.substring(1).trim();
            switch (ch) {
              case '#':
              case '^':
              case '_':
              case '<':
              case '$':
              case '=':
              case '?': {
                int start = currentLine.get();
                final List<Code> codes = compile(m, br, variable, currentLine, file);
                int lines = currentLine.get() - start;
                if (!onlywhitespace || lines == 0) {
                  write(list, out, currentLine.intValue());
                }
                out = new StringBuilder();
                switch (ch) {
                  case '#':
                    list.add(cf.iterable(m, variable, codes, file, currentLine.get()));
                    break;
                  case '^':
                    list.add(cf.notIterable(m, variable, codes, file, currentLine.get()));
                    break;
                  case '?':
                    list.add(cf.ifIterable(m, variable, codes, file, currentLine.get()));
                    break;
                  case '_':
                    list.add(cf.function(m, variable, codes, file, currentLine.get()));
                    break;
                  case '<':
                    list.add(cf.extend(m, variable, codes, file, currentLine.get()));
                    break;
                  case '$':
                    list.add(cf.name(m, variable, codes, file, currentLine.get()));
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
                // Reference
                out = write(list, out, currentLine.intValue());
                list.add(cf.value(m, command, true, currentLine.intValue()));
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
      if (size > 0 && (code = list.get(size - 1)) instanceof WriteCode) {
        WriteCode writeCode = (WriteCode) code;
        writeCode.append(text);
      } else {
        list.add(cf.write(text, line));
      }
    }
    return new StringBuilder();
  }

}
