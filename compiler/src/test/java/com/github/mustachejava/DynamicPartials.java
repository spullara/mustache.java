package com.github.mustachejava;

import com.github.mustachejava.codes.PartialCode;
import com.github.mustachejava.util.Wrapper;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DynamicPartials {

  private static final Object view = new Object() {
    final Object[] items = new Object[]{
            new Object() {
              final String partial = "dynamictext";
              final String content = "Some text";
            },
            new Object() {
              final String partial = "dynamicimage";
              final String url = "https://www.example.org";
            }
    };
  };

  @Test
  public void testDynamicPartial() {
    DefaultMustacheFactory mf = new DefaultMustacheFactory() {
      @Override
      public MustacheVisitor createMustacheVisitor() {
        return new DefaultMustacheVisitor(this) {

          private final Map<String, Mustache> partials = new HashMap<>();

          @Override
          public void value(TemplateContext tc, String variable, boolean encoded) {
            if (variable.startsWith("@")) {
              // This is actually a dynamic partial
              String file = tc.file();
              int dotindex = file.lastIndexOf(".");
              String extension = dotindex == -1 ? "" : file.substring(dotindex);
              Mustache dynamicpartial = compile(new StringReader("{{>.}}\n"), "dynamicpartial" + extension);
              iterable(tc, variable.substring(1), dynamicpartial);
              return;
            }
            super.value(tc, variable, encoded);
          }

          @Override
          public void partial(TemplateContext tc, String variable) {
            // Implements {{>.}} which dynamically gets the partial name from the partial field
            if (variable.equals(".")) {
              dynamicPartial(tc, variable);
            } else {
              super.partial(tc, variable);
            }
          }

          private void dynamicPartial(TemplateContext tc, String variable) {
            TemplateContext partialTC = new TemplateContext("{{", "}}", tc.file(), tc.line(), tc.startOfLine());
            list.add(new PartialCode(partialTC, df, variable) {
              @Override
              public void setCodes(Code[] newcodes) {
              }

              @Override
              public Writer execute(Writer writer, List<Object> scopes) {
                // Need to figure out which partial to compile and execute
                Wrapper partialWrapper = getObjectHandler().find("partial", scopes);
                Object call = partialWrapper.call(scopes);
                if (call == null) {
                  throw new MustacheException("No partial field found in scope", tc);
                }
                Mustache partial;
                synchronized (this) {
                  String name = call.toString();
                  partial = partials.get(name);
                  if (partial == null) {
                    partial = compilePartial(df.resolvePartialPath(dir, name, extension));
                    partials.put(name, partial);
                  }
                }
                Writer execute = partial.execute(writer, scopes);
                return appendText(execute);
              }

              @Override
              public synchronized void init() {
                synchronized (this) {
                  filterText();
                }
              }

              @Override
              protected String partialName() {
                throw new MustacheException("Unknown at compile time");
              }

              @Override
              public Code[] getCodes() {
                // We don't know the codes at compile time. We will not support recursion for dynamic partials.
                return new Code[]{};
              }
            });
          }
        };
      }
    };
    {
      Mustache mustache = mf.compile("dynamicbase.mustache");
      StringWriter sw = new StringWriter();
      mustache.execute(sw, view);
      assertEquals("<p>Some text</p>\n" +
              "<p><img src=\"https://www.example.org\"/></p>\n", sw.toString());
    }
    {
      Mustache mustache = mf.compile("dynamicbase2.mustache");
      StringWriter sw = new StringWriter();
      mustache.execute(sw, view);
      assertEquals("<p>Some text</p>\n" +
              "<p><img src=\"https://www.example.org\"/></p>\n", sw.toString());
    }
  }
}
