package com.github.mustachejava.codegen;

import com.github.mustachejava.reflect.Guard;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

import java.lang.reflect.AccessibleObject;
import java.util.Arrays;
import java.util.List;

public class CodegenReflectionWrapper extends ReflectionWrapper {
  final Guard compiledGuards;

  public CodegenReflectionWrapper(CodegenObjectHandler codegenObjectHandler, int scopeIndex, Wrapper[] wrappers, List<? extends Guard> guards, AccessibleObject member, Object[] arguments) {
    super(scopeIndex, wrappers, guards.toArray(new Guard[guards.size()]), member, arguments, codegenObjectHandler);
    compiledGuards = GuardCompiler.compile(guards);
  }

  public CodegenReflectionWrapper(ReflectionWrapper rw) {
    super(rw);
    compiledGuards = GuardCompiler.compile(Arrays.asList(rw.getGuards()));
  }

  @Override
  protected void guardCall(List<Object> scopes) throws GuardException {
    if (!compiledGuards.apply(scopes)) {
      throw guardException;
    }
  }
}
