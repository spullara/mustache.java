package com.github.mustachejava;

import com.github.mustachejava.codes.ValueCode;
import com.github.mustachejava.reflect.SimpleObjectHandler;
import com.github.mustachejava.resolver.DefaultResolver;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.github.mustachejava.util.HtmlEscaper.escape;

public class SafeMustacheFactory extends DefaultMustacheFactory {

  private final static Set<String> disallowedMethods = new HashSet<>(Arrays.asList(
          "getClass",
          "hashCode",
          "clone",
          "toString",
          "notify",
          "notifyAll",
          "finalize",
          "wait"
  ));

  // Only allow public access
  public static final SimpleObjectHandler OBJECT_HANDLER = new SimpleObjectHandler() {
    @Override
    protected void checkMethod(Method member) throws NoSuchMethodException {
      if (disallowedMethods.contains(member.getName())) {
        throw new MustacheException("Disallowed: method " + member.getName() + " cannot be accessed");
      }
      if ((member.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC) {
        throw new NoSuchMethodException("Only public members allowed");
      }
    }

    @Override
    protected void checkField(Field member) throws NoSuchFieldException {
      if ((member.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC) {
        throw new NoSuchFieldException("Only public members allowed");
      }
    }
  };

  public SafeMustacheFactory(Set<String> allowedResourceNames, String resourceRoot) {
    super(new DefaultResolver(resourceRoot) {
      @Override
      public Reader getReader(String resourceName) {
        // Only allow allowed resources
        if (allowedResourceNames.contains(resourceName)) {
          return super.getReader(resourceName);
        }
        throw new MustacheException("Disallowed: resource requested");
      }
    });
    setup();
  }

  public SafeMustacheFactory(Set<String> allowedResourceNames, File fileRoot) {
    super(new DefaultResolver(fileRoot) {
      @Override
      public Reader getReader(String resourceName) {
        // Only allow allowed resources
        if (allowedResourceNames.contains(resourceName)) {
          return super.getReader(resourceName);
        }
        throw new MustacheException("Disallowed: resource requested");
      }
    });
    setup();
  }

  private void setup() {
    setObjectHandler(OBJECT_HANDLER);
    mc.setAllowChangingDelimeters(false);
  }

  @Override
  public MustacheVisitor createMustacheVisitor() {
    return new DefaultMustacheVisitor(this) {
      @Override
      public void pragma(TemplateContext tc, String pragma, String args) {
        throw new MustacheException("Disallowed: pragmas in templates");
      }

      @Override
      public void value(TemplateContext tc, String variable, boolean encoded) {
        if (!encoded) {
          throw new MustacheException("Disallowed: non-encoded text in templates");
        }
        list.add(new ValueCode(tc, df, variable, encoded));
      }
    };
  }

  @Override
  public void encode(String value, Writer writer) {
    escape(value, writer);
  }
}
