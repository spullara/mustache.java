package com.github.mustachejava.indy;

import com.github.mustachejava.codegen.CodegenObjectHandler;
import com.github.mustachejava.codegen.CodegenReflectionWrapper;
import com.github.mustachejava.util.Wrapper;

/**
 * Finds the Codegen created wrappers and then wraps them with invokedynamic calls.
 */
public class IndyObjectHandler extends CodegenObjectHandler {

  @Override
  public Wrapper find(String name, Object[] scopes) {
    Wrapper wrapper = super.find(name, scopes);
    if (wrapper instanceof CodegenReflectionWrapper) {
      CodegenReflectionWrapper rw = (CodegenReflectionWrapper) wrapper;
      return IndyWrapper.create(rw);
    } else {
      return wrapper;
    }
  }
}
