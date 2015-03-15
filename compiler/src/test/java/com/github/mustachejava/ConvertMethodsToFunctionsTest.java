package com.github.mustachejava;

import com.github.mustachejava.reflect.Guard;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

import static junit.framework.Assert.assertEquals;

public class ConvertMethodsToFunctionsTest {

  private static ReflectionObjectHandler roh;

  @Retention(RetentionPolicy.RUNTIME)
  @interface TemplateMethod {

  }

  @BeforeClass
  public static void setup() {
    roh = new ReflectionObjectHandler() {
      /**
       * Find a wrapper given the current context. If not found, return null.
       *
       * @param scopeIndex the index into the scope array
       * @param wrappers   the current set of wrappers to get here
       * @param guards     the list of guards used to find this
       * @param scope      the current scope
       * @param name       the name in the scope
       * @return null if not found, otherwise a wrapper for this scope and name
       */
      @Override
      protected Wrapper findWrapper(int scopeIndex, Wrapper[] wrappers, List<Guard> guards, Object scope, String name) {
        Wrapper wrapper = super.findWrapper(scopeIndex, wrappers, guards, scope, name);
        if (wrapper == null) {
          // Now check to see if there is a method that takes a string
          return getWrapper(scopeIndex, wrappers, guards, scope, name, scope.getClass());
        }
        return wrapper;
      }

      private Wrapper getWrapper(final int scopeIndex, final Wrapper[] wrappers, final List<Guard> guards, Object scope, String name, Class<?> aClass) {
        try {
          Method method = aClass.getDeclaredMethod(name, String.class);
          method.setAccessible(true);
          return new ReflectionWrapper(scopeIndex, wrappers, guards.toArray(new Guard[guards.size()]), method, null, this) {
            @Override
            public Object call(List<Object> scopes) throws GuardException {
              guardCall(scopes);
              final Object scope1 = unwrap(scopes);
              if (scope1 == null) return null;
              if (method.getAnnotation(TemplateMethod.class) == null) {
                return new Function<String, String>() {
                  @Override
                  public String apply(String input) {
                    return getString(input, scope1);
                  }
                };
              } else {
                return new TemplateFunction() {
                  @Override
                  public String apply(String input) {
                    return getString(input, scope1);
                  }
                };
              }
            }

            private String getString(String input, Object scope1) {
              try {
                Object invoke = method.invoke(scope1, input);
                return invoke == null ? null : String.valueOf(invoke);
              } catch (InvocationTargetException e) {
                throw new MustacheException("Failed to execute method: " + method, e.getTargetException());
              } catch (IllegalAccessException e) {
                throw new MustacheException("Failed to execute method: " + method, e);
              }
            }
          };
        } catch (NoSuchMethodException e) {
          Class<?> superclass = aClass.getSuperclass();
          return superclass == Object.class ? null : getWrapper(scopeIndex, wrappers, guards, scope, name, superclass);
        }
      }
    };
  }


  @Test
  public void testConvert() throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    dmf.setObjectHandler(roh);
    Mustache uppertest = dmf.compile(new StringReader("{{#upper}}{{test}}{{/upper}}"), "uppertest");
    StringWriter sw = new StringWriter();
    uppertest.execute(sw, new Object() {
      String test = "test";
      String upper(String s) {
        return s.toUpperCase();
      }
    }).close();
    assertEquals("TEST", sw.toString());
    sw = new StringWriter();
    uppertest.execute(sw, new Object() {

      String test2 = "test";

      @TemplateMethod
      String upper(String s) {
        return "{{test2}}";
      }
    }).close();
    assertEquals("test", sw.toString());
  }

}
