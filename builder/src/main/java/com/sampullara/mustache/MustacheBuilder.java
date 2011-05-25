package com.sampullara.mustache;

import com.sampullara.util.FutureWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
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
public class MustacheBuilder implements MustacheJava {

  private final File root;
  private Class<? extends Mustache> superclass;

  public MustacheBuilder(File root) {
    this.root = root;
  }

  public void setSuperclass(String superclass) {
    try {
      this.superclass = (Class<? extends Mustache>) Class.forName(superclass);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid class", e);
    }
  }

  public Mustache parse(String template) throws MustacheException {
    return build(new StringReader(template), template);
  }

  public Mustache build(final Reader br, String file) throws MustacheException {
    Mustache mustache;
    try {
      mustache = superclass == null ? new Mustache() : superclass.newInstance();
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not instantiate", e);
    }
    mustache.setRoot(root);
    mustache.setPath(file);
    mustache.setMustacheJava(this);
    mustache.setCompiled(compile(mustache, br, null, new AtomicInteger(0), file));
    return mustache;
  }

  public Mustache parseFile(String path) throws MustacheException {
    Mustache compile;
    try {
      BufferedReader br = new BufferedReader(new FileReader(new File(root, path)));
      compile = build(br, path);
      br.close();
    } catch (IOException e) {
      throw new MustacheException("Failed to read", e);
    }
    return compile;
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
                final List<Code> codes = compile(m, br, variable, currentLine, file);
                int lines = currentLine.get() - start;
                if (!onlywhitespace || lines == 0) {
                  write(list, out);
                }
                out = new StringBuilder();
                list.add(new SubCode(ch, m, variable, codes, file, currentLine.get()));
                iterable = lines != 0;
                break;
              }
              case '/': {
                // Tag end
                if (!onlywhitespace) {
                  write(list, out);
                }
                if (!variable.equals(tag)) {
                  throw new MustacheException("Mismatched start/end tags: " + tag + " != " + variable + " in " + file + ":" + currentLine);
                }

                return list;
              }
              case '>': {
                out = write(list, out);
                list.add(new PartialCode(variable, m, file, currentLine.get()));
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
                    throw new MustacheException("Improperly closed variable in " + file + ":" + currentLine);
                  }
                }
                final String finalName = name;
                list.add(new WriteValueCode(m, finalName, false));
                break;
              }
              case '&': {
                // Not escaped
                out = write(list, out);
                list.add(new WriteValueCode(m, variable, false));
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
                list.add(new WriteValueCode(m, command, true));
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
    list.add(new WriteCode(out.toString()));
    return new StringBuilder();
  }

  private static class SubCode implements Code {
    private final char ch;
    private final Mustache m;
    private final String variable;
    private final Code[] codes;
    private final int line;
    private final String file;

    public SubCode(char ch, Mustache m, String variable, List<Code> codes, String file, int line) {
      this.ch = ch;
      this.m = m;
      this.variable = variable;
      this.codes = new ArrayList<Code>(codes).toArray(new Code[codes.size()]);
      this.line = line;
      this.file = file;
    }

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
          throw new MustacheException("Execution failed: " + file + ":" + line, e);
        }
      }
    }
  }

  private static class PartialCode implements Code {
    private final String variable;
    private Mustache m;
    private final String file;
    private final int line;

    public PartialCode(String variable, Mustache m, String file, int line) throws MustacheException {
      this.variable = variable;
      this.m = m;
      this.file = file;
      this.line = line;
    }

    @Override
    public void execute(FutureWriter fw, final Scope s) throws MustacheException {
      try {
        final Mustache partial = m.partial(variable);
        fw.enqueue(new Callable<Object>() {
          @Override
          public Object call() throws Exception {
            FutureWriter fw = new FutureWriter();
            partial.partial(fw, s, variable, partial);
            return fw;
          }
        });
      } catch (IOException e) {
        throw new MustacheException("Execution failed: " + file + ":" + line, e);
      }
    }
  }

  private static class WriteValueCode implements Code {
    private final Mustache m;
    private final String name;
    private final boolean encoded;

    public WriteValueCode(Mustache m, String name, boolean encoded) {
      this.m = m;
      this.name = name;
      this.encoded = encoded;
    }

    @Override
    public void execute(FutureWriter fw, Scope scope) throws MustacheException {
      m.write(fw, scope, name, encoded);
    }
  }

  private static class WriteCode implements Code {
    private final String rest;

    public WriteCode(String rest) {
      this.rest = rest;
    }

    public void execute(FutureWriter fw, Scope scope) throws MustacheException {
      try {
        fw.write(rest);
      } catch (IOException e) {
        throw new MustacheException("Failed to write", e);
      }
    }
  }
}
