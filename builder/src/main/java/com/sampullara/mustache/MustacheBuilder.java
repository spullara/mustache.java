package com.sampullara.mustache;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.sampullara.util.FutureWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sampullara.mustache.Mustache.truncate;

/**
 * A pseudo interpreter / compiler. Instead of compiling to Java code, it compiles to a
 * list of instructions to execute.
 * <p/>
 * User: sam
 * Date: 5/14/11
 * Time: 3:52 PM
 */
public class MustacheBuilder implements MustacheJava {

  private final String classpathRoot;
  private final File root;
  private Class<? extends Mustache> superclass;

  public MustacheBuilder() {
    this.root = null;
    this.classpathRoot = null;
  }

  public MustacheBuilder(String classpathRoot) {
    this.root = null;
    this.classpathRoot = classpathRoot;
  }

  public MustacheBuilder(File root) {
    this.root = root;
    this.classpathRoot = null;
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
      BufferedReader br;
      if (root == null) {
        String fullPath = classpathRoot == null ? path : classpathRoot + "/" + path;
        InputStream resourceAsStream =
            MustacheBuilder.class.getClassLoader().getResourceAsStream(fullPath);
        if (resourceAsStream == null) {
          throw new MustacheException(path + " not found in classpath");
        }
        br = new BufferedReader(new InputStreamReader(resourceAsStream, Charsets.UTF_8));
      } else {
        br = new BufferedReader(new FileReader(new File(root, path)));
      }
      compile = build(br, path);
      br.close();
    } catch (IOException e) {
      throw new MustacheException("Failed to read " + new File(root, path), e);
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
                    list.add(new IterableCode(m, variable, codes, file, currentLine.get()));
                    break;
                  case '^':
                    list.add(new InvertedIterableCode(m, variable, codes, file, currentLine.get()));
                    break;
                  case '?':
                    list.add(new IfIterableCode(m, variable, codes, file, currentLine.get()));
                    break;
                  case '_':
                    list.add(new FunctionCode(m, variable, codes, file, currentLine.get()));
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
                list.add(new PartialCode(variable, m, file, currentLine.get()));
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
                list.add(new WriteValueCode(m, finalName, false, currentLine.intValue()));
                break;
              }
              case '&': {
                // Not escaped
                out = write(list, out, currentLine.intValue());
                list.add(new WriteValueCode(m, variable, false, currentLine.intValue()));
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
                list.add(new WriteValueCode(m, command, true, currentLine.intValue()));
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
    } catch (IOException e) {
      throw new MustacheException("Failed to read", e);
    }
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
        list.add(new WriteCode(text, line));
      }
    }
    return new StringBuilder();
  }

  private abstract static class SubCode implements Code {
    protected final Mustache m;
    protected final String variable;
    protected final Code[] codes;
    private final int line;
    private final String file;

    public SubCode(Mustache m, String variable, List<Code> codes, String file, int line) {
      this.m = m;
      this.variable = variable;
      this.codes = new ArrayList<Code>(codes).toArray(new Code[codes.size()]);
      this.line = line;
      this.file = file;
    }

    @Override
    public abstract void execute(FutureWriter fw, Scope scope) throws MustacheException;

    protected void execute(FutureWriter fw, Iterable<Scope> iterable) throws MustacheException {
      if (iterable != null) {
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
                  if (Mustache.debug) {
                    Mustache.line.set(code.getLine());
                  }
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

    public int getLine() {
      return line;
    }
  }

  private static class IterableCode extends SubCode {
    public IterableCode(Mustache m, String variable, List<Code> codes, String file, int line) {
      super(m, variable, codes, file, line);
    }

    @Override
    public void execute(FutureWriter fw, Scope scope) throws MustacheException {
      execute(fw, m.iterable(scope, variable));
    }

    @Override
    public Scope unparse(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
      // I think we have to make iteration greedy and match until we can't find a match
      List<Scope> results = new ArrayList<Scope>();
      Scope result;
      do {
        result = new Scope();
        for (int i = 0; i < codes.length && result != null; i++) {
          if (Mustache.debug) {
            Mustache.line.set(codes[i].getLine());
          }
          Code[] truncate = truncate(codes, i + 1);
          result = codes[i].unparse(result, text, position, truncate);
        }
        if (result != null && result.size() > 0) {
          results.add(result);
        } else break;
      } while (true);
      if (results.size() != 0) {
        current.put(variable, results);
      }
      return current;
    }
  }

  private static class FunctionCode extends SubCode {
    public FunctionCode(Mustache m, String variable, List<Code> codes, String file, int line) {
      super(m, variable, codes, file, line);
    }

    @Override
    public void execute(FutureWriter fw, Scope scope) throws MustacheException {
      Object function = m.getValue(scope, variable);
      if (function instanceof Function) {
        execute(fw, m.function(scope, (Function) function));
      } else if (function == null) {
        execute(fw, Lists.newArrayList(scope));
      } else {
        throw new MustacheException("Not a function: " + function);
      }
    }

    static class MapFunction implements Function<String, String> {
      private Map<String, String> map = new HashMap<String, String>();

      void put(String input, String value) {
        map.put(input, value);
      }

      @Override
      public String apply(String input) {
        return map.get(input);
      }

      public String toString() {
        return map.toString();
      }
    }

    @Override
    public Scope unparse(Scope current, final String text, final AtomicInteger position, Code[] next) throws MustacheException {
      final String value = unparseValueCode(current, text, position, next, false);
      if (value == null) return null;
      MapFunction function = (FunctionCode.MapFunction) current.get(variable);
      if (function == null) {
        function = new MapFunction();
        put(current, variable, function);
      }
      StringWriter sw = new StringWriter();
      FutureWriter fw = new FutureWriter(sw);
      try {
        for (Code code : codes) {
          code.execute(fw, current);
        }
        fw.flush();
      } catch (IOException e) {
        throw new MustacheException("Failed to evaluate function body", e);
      }
      function.put(sw.toString(), value);
      return current;
    }
  }

  private static class IfIterableCode extends SubCode {
    public IfIterableCode(Mustache m, String variable, List<Code> codes, String file, int line) {
      super(m, variable, codes, file, line);
    }

    @Override
    public void execute(FutureWriter fw, Scope scope) throws MustacheException {
      if (m.capturedWriter.get() != null) {
        m.actual.set(fw);
        fw = m.capturedWriter.get();
      }
      execute(fw, m.ifiterable(scope, variable));
    }

    @Override
    public Scope unparse(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
      // Like the iterable version with only one
      Scope result = new Scope();
      for (int i = 0; i < codes.length && result != null; i++) {
        if (Mustache.debug) {
          Mustache.line.set(codes[i].getLine());
        }
        Code[] truncate = truncate(codes, i + 1);
        result = codes[i].unparse(result, text, position, truncate);
      }
      if (result != null && result.size() > 0) {
        put(current, variable, result);
      }
      return current;
    }
  }

  private static class InvertedIterableCode extends SubCode {
    public InvertedIterableCode(Mustache m, String variable, List<Code> codes, String file, int line) {
      super(m, variable, codes, file, line);
    }

    @Override
    public void execute(FutureWriter fw, Scope scope) throws MustacheException {
      if (m.capturedWriter.get() != null) {
        m.actual.set(fw);
        fw = m.capturedWriter.get();
      }
      execute(fw, m.inverted(scope, variable));
    }

    @Override
    public Scope unparse(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
      // Like the iterable version with only one
      Scope result = new Scope();
      for (int i = 0; i < codes.length && result != null; i++) {
        if (Mustache.debug) {
          Mustache.line.set(codes[i].getLine());
        }
        Code[] truncate = truncate(codes, i + 1);
        result = codes[i].unparse(result, text, position, truncate);
      }
      if (result != null) {
        current.putAll(result);
        put(current, variable, false);
      }
      return current;
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

    @Override
    public int getLine() {
      return line;
    }

    @Override
    public Scope unparse(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
      Scope unparse = m.partial(variable).unparse(text, position);
      if (unparse == null) return null;
      put(current, variable, unparse);
      return current;
    }
  }

  private static class WriteValueCode implements Code {
    private final Mustache m;
    private final String name;
    private final boolean encoded;
    private final int line;

    public WriteValueCode(Mustache m, String name, boolean encoded, int line) {
      this.m = m;
      this.name = name;
      this.encoded = encoded;
      this.line = line;
    }

    @Override
    public void execute(FutureWriter fw, Scope scope) throws MustacheException {
      m.write(fw, scope, name, encoded);
    }

    @Override
    public int getLine() {
      return line;
    }

    @Override
    public Scope unparse(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
      String value = unparseValueCode(current, text, position, next, encoded);
      if (value != null) {
        put(current, name, value);
        return current;
      }
      return null;
    }

  }

  private static String unparseValueCode(Scope current, String text, AtomicInteger position, Code[] next, boolean encoded) throws MustacheException {
    AtomicInteger probePosition = new AtomicInteger(position.get());
    Code[] truncate = truncate(next, 1);
    Scope result = null;
    int lastposition = position.get();
    while (next.length != 0 && probePosition.get() < text.length()) {
      lastposition = probePosition.get();
      result = next[0].unparse(current, text, probePosition, truncate);
      if (result == null) {
        probePosition.incrementAndGet();
      } else {
        break;
      }
    }
    if (result != null) {
      String value = text.substring(position.get(), lastposition);
      if (encoded) {
        // Decode
      }
      position.set(lastposition);
      return value;
    }
    return null;
  }

  private static void put(Scope result, String name, Object value) {
    String[] splits = name.split("[.]");
    Scope depth = result;
    for (int i = 0; i < splits.length; i++) {
      if (i < splits.length - 1) {
        Scope tmp = (Scope) result.get(splits[i]);
        if (tmp == null) {
          tmp = new Scope();
        }
        depth.put(splits[i], tmp);
        depth = tmp;
      } else {
        depth.put(splits[i], value);
      }
    }
  }

  private static class WriteCode implements Code {
    private final StringBuffer rest;
    private final int line;

    public WriteCode(String rest, int line) {
      this.rest = new StringBuffer(rest);
      this.line = line;
    }

    public void execute(FutureWriter fw, Scope scope) throws MustacheException {
      try {
        fw.write(rest.toString());
      } catch (IOException e) {
        throw new MustacheException("Failed to write", e);
      }
    }

    @Override
    public int getLine() {
      return line;
    }

    @Override
    public Scope unparse(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
      if (position.get() + rest.length() <= text.length()) {
        String substring = text.substring(position.get(), position.get() + rest.length());
        if (rest.toString().equals(substring)) {
          position.addAndGet(rest.length());
          return current;
        }
      }
      return null;
    }

    public void append(String append) {
      rest.append(append);
    }
  }
}
