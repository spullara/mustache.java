package com.github.mustachejava;

import com.github.mustachejava.reflect.GuardedBinding;
import com.github.mustachejava.reflect.MissingWrapper;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.Wrapper;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FailOnMissingTest {
  @Test
  public void testFail() {
    ReflectionObjectHandler roh = new ReflectionObjectHandler() {
      @Override
      public Binding createBinding(String name, final TemplateContext tc, Code code) {
        return new GuardedBinding(this, name, tc, code) {
          @Override
          protected synchronized Wrapper getWrapper(String name, List<Object> scopes) {
            Wrapper wrapper = super.getWrapper(name, scopes);
            if (wrapper instanceof MissingWrapper) {
              throw new MustacheException(name + " not found in " + tc);
            }
            return wrapper;
          }
        };
      }
    };
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    dmf.setObjectHandler(roh);
    try {
      Mustache test = dmf.compile(new StringReader("{{test}}"), "test");
      StringWriter sw = new StringWriter();
      test.execute(sw, new Object() {
        String test = "ok";
      }).close();
      assertEquals("ok", sw.toString());
      test.execute(new StringWriter(), new Object());
      fail("Should have failed");
    } catch (MustacheException me) {
      assertEquals("test not found in [test:1] @[test:1]", me.getCause().getMessage());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
