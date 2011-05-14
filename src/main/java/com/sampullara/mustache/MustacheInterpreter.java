package com.sampullara.mustache;

import com.sampullara.util.FutureWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: 5/14/11
 * Time: 3:52 PM
 */
public class MustacheInterpreter {

  private final Class<? extends Mustache> superclass;

  public MustacheInterpreter() {
    superclass = null;
  }

  public MustacheInterpreter(Class<? extends Mustache> superclass) {
    this.superclass = superclass;
  }

  public void interpret(FutureWriter fw, ExecutorService es, Scope scope, Reader template) throws MustacheException {
    char[] chars = new char[8192];
    StringWriter sw = new StringWriter();
    int read;
    try {
      while ((read = template.read(chars)) != -1) {
        sw.write(chars, 0, read);
      }
    } catch (IOException e) {
      throw new MustacheException("Failed to read template", e);
    }
    interpret(fw, es, scope, sw.toString(), null);
  }

  public void interpret(FutureWriter fw, ExecutorService es, Scope scope, String template) throws MustacheException {
    interpret(fw, es, scope, template, null);
  }

  private Mustache newMustache() throws MustacheException {
    if (superclass == null) {
      return new Mustache() {
        @Override
        public void execute(FutureWriter writer, Scope ctx) throws MustacheException {
        }
      };
    }
    try {
      return superclass.newInstance();
    } catch (Exception e) {
      throw new MustacheException("Could not create superclass", e);
    }
  }

  public int interpret(FutureWriter fw, final ExecutorService es, final Scope scope, final String template, String tag) throws MustacheException {
    // Base level
    Mustache m = newMustache();

    // Now we grab the mustache template
    String sm = "{{";
    String em = "}}";

    try {
      int c;
      final AtomicInteger pos = new AtomicInteger(0);
      boolean onlywhitespace = true;
      while (pos.get() < template.length()) {
        c = template.charAt(pos.getAndIncrement());
        if (c == '\r') {
          continue;
        }
        // Increment the line
        if (c == '\n') {
          if (!onlywhitespace) {
            fw.write("\n");
          }

          onlywhitespace = true;
          continue;
        }
        // Check for a mustache start
        if (c == sm.charAt(0)) {
          if (template.charAt(pos.getAndIncrement()) == sm.charAt(1)) {
            // Two mustaches, now capture command
            StringBuilder sb = new StringBuilder();
            while (pos.get() < template.length()) {
              c = template.charAt(pos.getAndIncrement());
              if (c == em.charAt(0)) {
                if (template.charAt(pos.getAndIncrement()) == em.charAt(1)) {
                  // Matched end
                  break;
                } else {
                  // Only one
                  pos.getAndDecrement();
                }
              }
              sb.append((char) c);
            }
            String command = sb.toString().trim();
            char ch = command.charAt(0);
            final String variable = command.substring(1);
            switch (ch) {
              case '#': {
                final String substring = template.substring(pos.get());
                boolean first = true;
                for (final Scope subScope : m.iterable(scope, variable)) {
                  if (first) {
                    pos.getAndAdd((interpret(fw, es, subScope, substring, variable)));
                  } else {
                    fw.enqueue(new Callable<Object>() {
                      @Override
                      public Object call() throws Exception {
                        FutureWriter fw = new FutureWriter();
                        interpret(fw, es, subScope, substring, variable);
                        return fw;
                      }
                    });
                  }
                }
                break;
              }
              case '^':  {
                for (final Scope subScope : m.inverted(scope, variable)) {
                  pos.getAndAdd(interpret(fw, es, subScope, template.substring(pos.get()), variable));
                }
                break;
              }
              case '?': {
                for (final Scope subScope : m.ifiterable(scope, variable)) {
                  pos.getAndAdd(interpret(fw, es, subScope, template.substring(pos.get()), variable));
                }
                break;
              }
              case '/':
                // Tag end
                if (!variable.equals(tag)) {
                  throw new MustacheException("Mismatched start/end tags: " + tag + " != " + variable);
                }
                return pos.get();
              case '>':
                break;
              case '{': {
                // Not escaped
                String name = variable;
                if (em.charAt(1) != '}') {
                  name = variable.substring(0, variable.length() - 1);
                } else {
                  if (template.charAt(pos.getAndIncrement()) != '}') {
                    throw new MustacheException("Improperly closed variable");
                  }
                }
                Object o = scope.get(name);
                if (o != null) {
                  fw.write(o.toString());
                }
                break;
              }
              case '&': {
                // Not escaped
                Object o = scope.get(variable);
                if (o != null) {
                  fw.write(o.toString());
                }
                break;
              }
              case '%':
                // Pragmas
                break;
              case '!':
                // Comment
                break;
              default: {
                // Reference
                Object o = scope.get(command);
                if (o != null) {
                  fw.write(Mustache.encode(o.toString()));
                }
                break;
              }
            }
            continue;
          } else {
            // Only one
            pos.getAndDecrement();
          }
        }
        onlywhitespace = (c == ' ' || c == '\t') && onlywhitespace;
        if (!onlywhitespace) {
          fw.write(c);
        }
      }
    } catch (IOException e) {
      throw new MustacheException("Failed to read template", e);
    }
    return 0;
  }
}
