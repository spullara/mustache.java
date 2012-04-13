package com.github.mustachejava.jruby;

import com.github.mustachejava.Iteration;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.Wrapper;
import org.jruby.RubyHash;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;

import java.io.Writer;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.common.base.Predicate;

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
  protected Wrapper findWrapper(int scopeIndex, Wrapper[] wrappers, List<Predicate<Object[]>> guards, Object scope, String name) {
    Wrapper wrapper = super.findWrapper(scopeIndex, wrappers, guards, scope, name);
    if (wrapper == null) {
      if (scope instanceof RubyHash) {
        RubyHash hash = (RubyHash) scope;
        if (hash.get(name) != null) {
          return createWrapper(scopeIndex, wrappers, guards, MAP_METHOD, new Object[]{name});
        }
        RubySymbol rs = RubySymbol.newSymbol(hash.getRuntime(), name);
        if (hash.get(rs) != null) {
          return createWrapper(scopeIndex, wrappers, guards, MAP_METHOD, new Object[]{rs});
        }
      }
      if (scope instanceof RubyObject) {
        return createWrapper(scopeIndex, wrappers, guards, CALL_METHOD, new Object[]{ name });
      }
    }
    return wrapper;
  }
}
