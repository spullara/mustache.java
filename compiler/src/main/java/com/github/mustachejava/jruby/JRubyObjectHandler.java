package com.github.mustachejava.jruby;

import java.lang.reflect.Method;
import java.util.List;
import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.Wrapper;
import org.jruby.RubyBoolean;
import org.jruby.RubyHash;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;

public class JRubyObjectHandler extends ReflectionObjectHandler {

  private static final Method CALL_METHOD;

  static {
    try {
      CALL_METHOD = RubyHash.class.getMethod("callMethod", String.class);
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public Object coerce(Object object) {
    if (object instanceof RubyBoolean) {
      RubyBoolean rb = (RubyBoolean) object;
      return rb.toJava(Boolean.class);
    }
    return object;
  }

  @Override
  protected Wrapper findWrapper(final int scopeIndex, final Wrapper[] wrappers, final List<Predicate<Object[]>> guards, final Object scope, final String name) {
    Wrapper wrapper = super.findWrapper(scopeIndex, wrappers, guards, scope, name);
    if (wrapper == null) {
      if (scope instanceof RubyHash) {
        RubyHash hash = (RubyHash) scope;
        final RubySymbol rs = RubySymbol.newSymbol(hash.getRuntime(), name);
        if (hash.get(rs) != null) {
          guards.add(new Predicate<Object[]>() {
            @Override
            public boolean apply(@Nullable Object[] input) {
              assert input != null;
              return ((RubyHash)input[scopeIndex]).containsKey(rs);
            }
          });
          return createWrapper(scopeIndex, wrappers, guards, MAP_METHOD, new Object[]{rs});
        } else {
          guards.add(new Predicate<Object[]>() {
            @Override
            public boolean apply(@Nullable Object[] input) {
              assert input != null;
              return !((RubyHash)input[scopeIndex]).containsKey(rs);
            }
          });
        }
      }
      if (scope instanceof RubyObject) {
        RubyObject ro = (RubyObject) scope;
        if (ro.respondsTo(name)) {
          guards.add(new Predicate<Object[]>() {
            @Override
            public boolean apply(@Nullable Object[] objects) {
              RubyObject scope = (RubyObject) objects[scopeIndex];
              return scope.respondsTo(name);
            }
          });
          return createWrapper(scopeIndex, wrappers, guards, CALL_METHOD, new Object[]{ name });
        } else {
          guards.add(new Predicate<Object[]>() {
            @Override
            public boolean apply(@Nullable Object[] objects) {
              RubyObject scope = (RubyObject) objects[scopeIndex];
              return !scope.respondsTo(name);
            }
          });
        }
      }
    }
    return wrapper;
  }
}
