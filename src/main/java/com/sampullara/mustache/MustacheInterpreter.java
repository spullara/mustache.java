package com.sampullara.mustache;

import com.sampullara.util.FutureWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A pseudo interpreter / compiler. Instead of compiling to Java code, it compiles to a
 * list of instructions to execute.
 * <p/>
 * User: sam
 * Date: 5/14/11
 * Time: 3:52 PM
 */
public class MustacheInterpreter {

  private final File root;

  public MustacheInterpreter(File root) {
    this.root = root;
  }

  public static interface Code {
    void execute(FutureWriter fw, Scope scope) throws MustacheException;
  }

  public Mustache compile(final Reader br) throws MustacheException {
    return new Mustache() {
      List<Code> compiled = compile(this, br, null, new AtomicInteger(0));

      @Override
      public void execute(FutureWriter writer, Scope ctx) throws MustacheException {
        for (Code code : compiled) {
          code.execute(writer, ctx);
        }
      }
    };
  }

  public Mustache parseFile(String path) throws MustacheException {
    Mustache compile;
    try {
      BufferedReader br = new BufferedReader(new FileReader(new File(root, path)));
      compile = compile(br);
      br.close();
    } catch (IOException e) {
      throw new MustacheException("Failed to read", e);
    }
    return compile;
  }

  protected List<Code> compile(final Mustache m, final Reader br, String tag, final AtomicInteger currentLine) throws MustacheException {
    final List<Code> list = new ArrayList<Code>();

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
          out = write(list, out);

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
            final String variable = command.substring(1);
            switch (ch) {
              case '#':
              case '^':
              case '?': {
                int start = currentLine.get();
                final List<Code> codes = compile(m, br, variable, currentLine);
                list.add(new Code() {
                  @Override
                  public void execute(FutureWriter fw, Scope scope) throws MustacheException {
                    Iterable<Scope> iterable = null;
                    switch (ch) {
                      case '#':
                        iterable = m.iterable(scope, variable);
                        break;
                      case '^':
                        if (m.capturedWriter.get() != null) {
                          m.actual.set(fw);
                          fw = m.capturedWriter.get();
                        }
                        iterable = m.inverted(scope, variable);
                        break;
                      case '?':
                        if (m.capturedWriter.get() != null) {
                          m.actual.set(fw);
                          fw = m.capturedWriter.get();
                        }
                        iterable = m.ifiterable(scope, variable);
                        break;
                    }
                    for (final Scope subScope : iterable) {
                      try {
                        if (m.capturedWriter.get() != null) {
                          m.actual.set(fw);
                          fw = m.capturedWriter.get();
                        }
                        fw.enqueue(new Callable<Object>() {
                          @Override
                          public Object call() throws Exception {
                            FutureWriter writer = new FutureWriter();
                            for (Code code : codes) {
                              code.execute(writer, subScope);
                            }
                            return writer;
                          }
                        });
                      } catch (IOException e) {
                        throw new MustacheException("Failed to enqueue", e);
                      }
                    }
                  }
                });
                int lines = currentLine.get() - start;
                if (!onlywhitespace || lines == 0) {
                  write(list, out);
                }
                out = new StringBuilder();
                iterable = lines != 0;
                break;
              }
              case '/': {
                // Tag end
                if (!onlywhitespace) {
                  write(list, out);
                }
                if (!variable.equals(tag)) {
                  throw new MustacheException("Mismatched start/end tags: " + tag + " != " + variable);
                }

                return list;
              }
              case '>': {
                out = write(list, out);
                final Mustache partial = compile(new BufferedReader(new FileReader(new File(root, variable + ".html"))));
                list.add(new Code() {
                  @Override
                  public void execute(FutureWriter fw, final Scope s) throws MustacheException {
                    try {
                      fw.enqueue(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                          Object parent = s.get(variable);
                          Scope scope = parent == null ? s : new Scope(parent, s);
                          FutureWriter fw = new FutureWriter();
                          partial.execute(fw, scope);
                          return fw;
                        }
                      });
                    } catch (IOException e) {
                      throw new MustacheException("Failed to write", e);
                    }
                  }
                });
                break;
              }
              case '{': {
                out = write(list, out);
                // Not escaped
                String name = variable;
                if (em.charAt(1) != '}') {
                  name = variable.substring(0, variable.length() - 1);
                } else {
                  if (br.read() != '}') {
                    throw new MustacheException("Improperly closed variable");
                  }
                }
                final String finalName = name;
                list.add(new Code() {
                  @Override
                  public void execute(FutureWriter fw, Scope scope) throws MustacheException {
                    Object o = m.getValue(scope, finalName);
                    if (o != null) {
                      try {
                        fw.write(o.toString());
                      } catch (IOException e) {
                        throw new MustacheException("Failed to write", e);
                      }
                    }
                  }
                });
                break;
              }
              case '&': {
                // Not escaped
                out = write(list, out);
                list.add(new Code() {
                  @Override
                  public void execute(FutureWriter fw, Scope scope) throws MustacheException {
                    Object o = m.getValue(scope, variable);
                    if (o != null) {
                      try {
                        fw.write(o.toString());
                      } catch (IOException e) {
                        throw new MustacheException("Failed to write", e);
                      }
                    }
                  }
                });
                break;
              }
              case '%':
                // Pragmas
                out = write(list, out);
                break;
              case '!':
                // Comment
                out = write(list, out);
                break;
              default: {
                // Reference
                out = write(list, out);
                list.add(new Code() {
                  @Override
                  public void execute(FutureWriter fw, Scope scope) throws MustacheException {
                    Object o = m.getValue(scope, command);
                    if (o != null) {
                      try {
                        fw.write(m.encode(o.toString()));
                      } catch (IOException e) {
                        throw new MustacheException("Failed to write", e);
                      }
                    }
                  }
                });
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
      write(list, out);
    } catch (IOException e) {
      throw new MustacheException("Failed to read", e);
    }
    return list;
  }

  private StringBuilder write(List<Code> list, StringBuilder out) {
    final String rest = out.toString();
    list.add(new Code() {
      public void execute(FutureWriter fw, Scope scope) throws MustacheException {
        try {
          fw.write(rest);
        } catch (IOException e) {
          throw new MustacheException("Failed to write", e);
        }
      }
    });
    return new StringBuilder();
  }
}
