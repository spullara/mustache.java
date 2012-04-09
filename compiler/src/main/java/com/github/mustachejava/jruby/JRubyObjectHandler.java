package com.github.mustachejava.jruby;

import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.Wrapper;
import org.jruby.RubyHash;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;

import java.lang.reflect.Method;

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
  protected Wrapper findWrapper(int scopeIndex, Wrapper[] wrappers, Class[] guard, Object scope, String name) {
    Wrapper wrapper = super.findWrapper(scopeIndex, wrappers, guard, scope, name);
    if (wrapper == null) {
      if (scope instanceof RubyHash) {
        RubyHash hash = (RubyHash) scope;
        if (hash.get(name) != null) {
          return createWrapper(scopeIndex, wrappers, guard, MAP_METHOD, new Object[]{name});
        }
        RubySymbol rs = RubySymbol.newSymbol(hash.getRuntime(), name);
        if (hash.get(rs) != null) {
          return createWrapper(scopeIndex, wrappers, guard, MAP_METHOD, new Object[]{rs});
        }
      }
      if (scope instanceof RubyObject) {
        return createWrapper(scopeIndex, wrappers, guard, CALL_METHOD, new Object[]{ name });
      }
    }
    return wrapper;
  }
}
