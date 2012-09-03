package com.github.mustachejava.codegen;

import com.github.mustachejava.codegen.guards.*;
import com.github.mustachejava.reflect.Guard;
import com.github.mustachejava.reflect.MissingWrapper;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.reflect.guards.*;
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
    return new CodegenMissingWrapper(guards);
  }

  @Override
  protected Wrapper createWrapper(int scopeIndex, Wrapper[] wrappers, List<? extends Guard> guards, AccessibleObject member, Object[] arguments) {
    return new CodegenReflectionWrapper(this, scopeIndex, wrappers, guards, member, arguments);
  }

}
