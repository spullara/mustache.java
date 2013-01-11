package com.github.mustachejava.codegen.guards;

import com.github.mustachejava.codegen.CompilableGuard;
import com.github.mustachejava.reflect.guards.DotGuard;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Compiled dot guard.
 */
public class CompilableDotGuard extends DotGuard implements CompilableGuard {

  public CompilableDotGuard(String lookup, int scopeIndex, Object classGuard) {
    super(lookup, scopeIndex, classGuard);
  }

  public void addGuard(Label returnFalse, GeneratorAdapter gm, GeneratorAdapter cm, GeneratorAdapter sm, ClassWriter cw, AtomicInteger atomicId, List<Object> cargs, Type thisType) {
    // do nothing and it is assumed true
  }
}
