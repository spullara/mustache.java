package com.github.mustachejava.codegen;

import com.github.mustachejava.reflect.Guard;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

import java.lang.reflect.AccessibleObject;
import java.util.List;

public class CompiledReflectionWrapper extends ReflectionWrapper {
  final Guard compiledGuards;

  public CompiledReflectionWrapper(CodegenObjectHandler codegenObjectHandler, int scopeIndex, Wrapper[] wrappers, List<? extends Guard> guards, AccessibleObject member, Object[] arguments) {
    super(scopeIndex, wrappers, guards.toArray(new Guard[guards.size()]), member, arguments, codegenObjectHandler);
    compiledGuards = GuardCompiler.compile(guards);
  }

  @Override
  protected void guardCall(Object[] scopes) throws GuardException {
    if (!compiledGuards.apply(scopes)) {
      throw guardException;
    }
  }
}
