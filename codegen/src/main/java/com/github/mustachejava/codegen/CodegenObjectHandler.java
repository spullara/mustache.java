package com.github.mustachejava.codegen;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.codegen.guards.*;
import com.github.mustachejava.reflect.*;
import com.github.mustachejava.reflect.guards.*;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

import java.lang.reflect.AccessibleObject;
import java.util.List;

/**
 * Generates code when it can for higher performance. Make sure you have enough
 * PermGen to run your application. Each variable will generate at least one guard
 * class and each mustache section or partial will as well.
 */
public class CodegenObjectHandler extends ReflectionObjectHandler {
  @Override
  protected ClassGuard createClassGuard(int i, Object scope) {
    return new CompilableClassGuard(i, scope);
  }

  @Override
  protected DepthGuard createDepthGuard(int length) {
    return new CompilableDepthGuard(length);
  }

  @Override
  protected DotGuard createDotGuard(int i, Object scope, String lookup) {
    return new CompilableDotGuard(lookup, i, scope);
  }

  @Override
  protected MapGuard createMapGuard(int scopeIndex, Wrapper[] wrappers, String name, boolean contains) {
    return new CompilableMapGuard(this, scopeIndex, name, contains, wrappers);
  }

  @Override
  protected NullGuard createNullGuard() {
    return new CompilableNullGuard();
  }

  @Override
  protected WrappedGuard createWrappedGuard(int i, List<Wrapper> wrappers, List<Guard> wrapperGuard) {
    return new CompilableWrappedGuard(this, i, wrappers, wrapperGuard);
  }

  @Override
  protected MissingWrapper createMissingWrapper(List<Guard> guards) {
    final Guard compiledGuards = compile(guards);
    return new MissingWrapper(guards.toArray(new Guard[guards.size()])) {
      @Override
      protected void guardCall(Object[] scopes) throws GuardException {
        if (!compiledGuards.apply(scopes)) {
          throw guardException;
        }
      }
    };
  }

  @Override
  protected Wrapper createWrapper(int scopeIndex, Wrapper[] wrappers, List<? extends Guard> guards, AccessibleObject member, Object[] arguments) {
    final Guard compiledGuards = compile(guards);
    return new ReflectionWrapper(scopeIndex, wrappers, guards.toArray(new Guard[guards.size()]), member, arguments, this) {
      @Override
      protected void guardCall(Object[] scopes) throws GuardException {
        if (!compiledGuards.apply(scopes)) {
          throw guardException;
        }
      }
    };
  }

  private Guard compile(List<? extends Guard> guard) {
    Guard[] compiled = GuardCompiler.compile(guard.toArray(new Guard[guard.size()]));
    if (compiled.length != 1) throw new MustacheException("Failed to compile all guards: " + guard);
    return compiled[0];
  }
}
