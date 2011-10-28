package com.sampullara.mustache;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import com.sampullara.util.FutureWriter;

import static com.sampullara.mustache.Mustache.truncate;

/**
 * The default code factory is designed to work well with the default Mustache implementation.
 * In many cases where you significantly change the behavior of the default Mustache class you
 * may want to have your own CodeFactory that works with it.
 */
public class DefaultCodeFactory implements CodeFactory {

  @Override
  public Code iterable(Mustache m, String variable, List<Code> codes, String file, int line) {
    return new IterableCode(m, variable, codes, file, line);
  }

  @Override
  public Code function(Mustache m, String variable, List<Code> codes, String file, int line) {
    return new FunctionCode(m, variable, codes, file, line);
  }

  @Override
  public Code ifIterable(Mustache m, String variable, List<Code> codes, String file, int line) {
    return new IfIterableCode(m, variable, codes, file, line);
  }

  @Override
  public Code notIterable(Mustache m, String variable, List<Code> codes, String file, int line) {
    return new InvertedIterableCode(m, variable, codes, file, line);
  }

  @Override
  public Code partial(Mustache m, String variable, String file, int line) {
    return new PartialCode(m, variable, file, line);
  }

  @Override
  public Code value(Mustache m, String name, boolean encode, int line) {
    return new WriteValueCode(m, name, encode, line);
  }

  @Override
  public Code write(String s, int line) {
    return new DefaultWriteCode(s, line);
  }

  @Override
  public Code eof(int line) {
    return new EOFCode(line);
  }

  @Override
  public Code extend(Mustache m, String variable, List<Code> codes, String file, int i) {
    return new ExtendCode(m, variable, codes, file, i);
  }

  @Override
  public Code name(Mustache m, String variable, List<Code> codes, String file, int i) {
    return new ExtendNameCode(m, variable, codes, file, i);
  }

  @Override
  public Code replace(Mustache m, String variable, List<Code> codes, String file, int i) {
    return new ExtendReplaceCode(m, variable, codes, file, i);
  }

  private abstract static class SubCode implements Code {
    protected final Mustache m;
    protected final String variable;
    protected final Code[] codes;
    protected final int line;
    protected final String file;

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
            fw = m.pushWriter(fw);
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

    protected void identity(String marker, FutureWriter fw, Scope scope) throws IOException, MustacheException {
      fw.append("{{").append(marker).append(variable).append("}}");
      execute(fw, m.iterable(scope, variable));
      fw.append("{{/").append(variable).append("}}");
    }
  }

  private static class IterableCode extends SubCode {
    public IterableCode(Mustache m, String variable, List<Code> codes, String file, int line) {
      super(m, variable, codes, file, line);
    }

    @Override
    public void execute(FutureWriter fw, Scope scope) throws MustacheException {
      try {
        if (scope == IdentityScope.one) {
          identity("#", fw, scope);
        } else {
          execute(fw, m.iterable(scope, variable));
        }
      } catch (IOException e) {
        throw new MustacheException(e);
      }
    }

    @Override
    public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
      // I think we have to make iteration greedy and match until we can't find a match
      List<Scope> results = new ArrayList<Scope>();
      Scope result;
      do {
        int start = position.get();
        result = new Scope();
        for (int i = 0; i < codes.length && result != null; i++) {
          if (Mustache.debug) {
            Mustache.line.set(codes[i].getLine());
          }
          Code[] truncate = truncate(codes, i + 1);
          result = codes[i].unexecute(result, text, position, truncate);
        }
        if (result != null && result.size() > 0) {
          results.add(result);
        } else {
          position.set(start);
          break;
        }
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
      try {
        if (scope == IdentityScope.one) {
          identity("_", fw, scope);
        } else {
          Object function = m.getValue(scope, variable);
          if (function instanceof Function) {
            execute(fw, m.function(scope, (Function) function));
          } else if (function == null) {
            execute(fw, Lists.newArrayList(scope));
          } else {
            throw new MustacheException("Not a function: " + function);
          }
        }
      } catch (IOException e) {
        throw new MustacheException(e);
      }
    }

    @Override
    public Scope unexecute(Scope current, final String text, final AtomicInteger position, Code[] next) throws MustacheException {
      final String value = unexecuteValueCode(current, text, position, next, false);
      if (value == null) return null;
      Scope function = (Scope) current.get(variable);
      if (function == null) {
        function = new UnexecuteFunction();
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

    private static class UnexecuteFunction extends Scope implements Function<String, String> {
      @Override
      public String apply(String input) {
        Object o = get(input);
        return o == null ? "" : o.toString();
      }
    }

  }

  private static class IfIterableCode extends SubCode {
    public IfIterableCode(Mustache m, String variable, List<Code> codes, String file, int line) {
      super(m, variable, codes, file, line);
    }

    @Override
    public void execute(FutureWriter fw, Scope scope) throws MustacheException {
      fw = m.pushWriter(fw);
      try {
        if (scope == IdentityScope.one) {
          identity("?", fw, scope);
        } else {
          execute(fw, m.ifiterable(scope, variable));
        }
      } catch (IOException e) {
        throw new MustacheException(e);
      }
    }

    @Override
    public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
      // Like the iterable version with only one
      Scope result = new Scope();
      for (int i = 0; i < codes.length && result != null; i++) {
        if (Mustache.debug) {
          Mustache.line.set(codes[i].getLine());
        }
        Code[] truncate = truncate(codes, i + 1);
        result = codes[i].unexecute(result, text, position, truncate);
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
      fw = m.pushWriter(fw);
      try {
        if (scope == IdentityScope.one) {
          identity("^", fw, scope);
        } else {
          execute(fw, m.inverted(scope, variable));
        }
      } catch (IOException e) {
        throw new MustacheException(e);
      }
    }

    @Override
    public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
      // Like the iterable version with only one
      Scope result = new Scope();
      for (int i = 0; i < codes.length && result != null; i++) {
        if (Mustache.debug) {
          Mustache.line.set(codes[i].getLine());
        }
        Code[] truncate = truncate(codes, i + 1);
        result = codes[i].unexecute(result, text, position, truncate);
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

    public PartialCode(Mustache m, String variable, String file, int line) {
      this.variable = variable;
      this.m = m;
      this.file = file;
      this.line = line;
    }

    @Override
    public void execute(FutureWriter fw, final Scope scope) throws MustacheException {
      try {
        if (scope == IdentityScope.one) {
          fw.append("{{>").append(variable).append("}}");
        } else {
          final Mustache partial = m.partial(variable);
          fw.enqueue(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
              FutureWriter fw = new FutureWriter();
              partial.partial(fw, scope, variable, partial);
              return fw;
            }
          });
        }
      } catch (IOException e) {
        throw new MustacheException("Execution failed: " + file + ":" + line, e);
      }
    }

    @Override
    public int getLine() {
      return line;
    }

    @Override
    public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
      String partialText = unexecuteValueCode(current, text, position, next, false);
      AtomicInteger partialPosition = new AtomicInteger(0);
      Scope unexecuted = m.partial(variable).unexecute(partialText, partialPosition);
      if (unexecuted == null) return null;
      put(current, variable, unexecuted);
      return current;
    }
  }

  /**
   * Implementation strategy:
   * - Load extension template codes
   * - Load local template codes
   * - Execute extension template codes, replacing named sections with local replacements
   */

  private static abstract class ExtendBaseCode extends SubCode {

    public ExtendBaseCode(Mustache m, String variable, List<Code> codes, String file, int line) {
      super(m, variable, codes, file, line);
    }

    public String getName() {
      return variable;
    }
  }

  private static class ExtendCode extends ExtendBaseCode {

    private Map<String, ExtendReplaceCode> replaceMap;

    public ExtendCode(Mustache m, String variable, List<Code> codes, String file, int line) {
      super(m, variable, codes, file, line);
      replaceMap = new HashMap<String, ExtendReplaceCode>();
      for (Code code : codes) {
        if (code instanceof ExtendReplaceCode) {
          ExtendReplaceCode erc = (ExtendReplaceCode) code;
          replaceMap.put(erc.getName(), erc);
        } else if (code instanceof WriteCode) {
          // ignore text
        } else {
          throw new IllegalArgumentException(
                  "Illegal code in extend section: " + code.getClass().getName());
        }
      }
    }

    @Override
    public void execute(FutureWriter fw, Scope scope) throws MustacheException {
      if (scope == IdentityScope.one) {
        try {
          fw.append("{{<").append(variable).append("}}");
        } catch (IOException e) {
          throw new MustacheException("Execution failed: " + file + ":" + line, e);
        }
      } else {
        Map<String, ExtendReplaceCode> debugMap = null;
        if (Mustache.debug) {
          debugMap = new HashMap<String, ExtendReplaceCode>(replaceMap);
        }
        Mustache partial = m.partial(variable);
        Code[] supercodes = partial.getCompiled();
        for (Code code : supercodes) {
          if (code instanceof ExtendNameCode) {
            ExtendNameCode enc = (ExtendNameCode) code;
            ExtendReplaceCode extendReplaceCode = replaceMap.get(enc.getName());
            if (extendReplaceCode != null) {
              if (Mustache.debug) {
                debugMap.remove(enc.getName());
              }
              extendReplaceCode.execute(fw, scope);
              continue;
            }
          }
          code.execute(fw, scope);
        }
        if (Mustache.debug) {
          if (debugMap.size() > 0) {
            throw new MustacheException(
                    "Replacement sections failed to match named sections: " + debugMap.keySet());
          }
        }
      }
    }

    @Override
    public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
      Mustache partial = m.partial(variable);
      Code[] supercodes = partial.getCompiled();
      for (int i = 0; i < supercodes.length; i++) {
        Code[] truncate = truncate(supercodes, i + 1);
        Code code = supercodes[i];
        if (code instanceof ExtendNameCode) {
          ExtendNameCode enc = (ExtendNameCode) code;
          ExtendReplaceCode extendReplaceCode = replaceMap.get(enc.getName());
          if (extendReplaceCode != null) {
            extendReplaceCode.unexecute(current, text, position, truncate);
            continue;
          }
        }
        code.unexecute(current, text, position, truncate);
      }
      return current;
    }
  }

  private static class ExtendReplaceCode extends ExtendBaseCode {

    public ExtendReplaceCode(Mustache m, String variable, List<Code> codes, String file, int line) {
      super(m, variable, codes, file, line);
    }

    @Override
    public void execute(FutureWriter fw, Scope scope) throws MustacheException {
      if (scope == IdentityScope.one) {
        try {
          identity("=", fw, scope);
        } catch (IOException e) {
          throw new MustacheException("Execution failed: " + file + ":" + line, e);
        }
      } else {
        execute(fw, Arrays.asList(scope));
      }
    }

    @Override
    public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
      for (int i = 0; i < codes.length; i++) {
        if (Mustache.debug) {
          Mustache.line.set(codes[i].getLine());
        }
        Code[] truncate = truncate(codes, i + 1);
        current = codes[i].unexecute(current, text, position, truncate);
      }
      return current;
    }
  }

  private static class ExtendNameCode extends ExtendReplaceCode {
    public ExtendNameCode(Mustache m, String variable, List<Code> codes, String file, int line) {
      super(m, variable, codes, file, line);
    }

    @Override
    public void execute(FutureWriter fw, Scope scope) throws MustacheException {
      if (scope == IdentityScope.one) {
        try {
          identity("$", fw, scope);
        } catch (IOException e) {
          throw new MustacheException("Execution failed: " + file + ":" + line, e);
        }
      } else {
        execute(fw, Arrays.asList(scope));
      }
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
      if (scope == IdentityScope.one) {
        try {
          if (!encoded) fw.append("{");
          fw.append("{{").append(name).append("}}");
          if (!encoded) fw.append("}");
        } catch (IOException e) {
          throw new MustacheException(e);
        }
      } else {
        m.write(fw, scope, name, encoded);
      }
    }

    @Override
    public int getLine() {
      return line;
    }

    @Override
    public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
      String value = unexecuteValueCode(current, text, position, next, encoded);
      if (value != null) {
        put(current, name, value);
        return current;
      }
      return null;
    }

  }

  private static String unexecuteValueCode(Scope current, String text, AtomicInteger position, Code[] next, boolean encoded) throws MustacheException {
    AtomicInteger probePosition = new AtomicInteger(position.get());
    Code[] truncate = truncate(next, 1);
    Scope result = null;
    int lastposition = position.get();
    while (next.length != 0 && probePosition.get() < text.length()) {
      lastposition = probePosition.get();
      result = next[0].unexecute(current, text, probePosition, truncate);
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

  private static class EOFCode implements Code {

    private final int line;

    public EOFCode(int line) {
      this.line = line;
    }

    @Override
    public void execute(FutureWriter fw, Scope scope) throws MustacheException {
      // NOP
    }

    @Override
    public int getLine() {
      return line;
    }

    @Override
    public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
      // End of text
      position.set(text.length());
      return current;
    }
  }

  private static class DefaultWriteCode implements WriteCode {
    private final StringBuffer rest;
    private final int line;

    public DefaultWriteCode(String rest, int line) {
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
    public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
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
