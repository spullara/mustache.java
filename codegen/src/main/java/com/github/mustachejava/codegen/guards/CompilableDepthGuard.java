package com.github.mustachejava.codegen.guards;

import com.github.mustachejava.codegen.CompilableGuard;
import com.github.mustachejava.reflect.guards.DepthGuard;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Compiled version of the depth guard.
 */
public class CompilableDepthGuard extends DepthGuard implements CompilableGuard {

  public CompilableDepthGuard(int length) {
    super(length);
  }

  @Override
  public void addGuard(Label returnFalse, GeneratorAdapter gm, GeneratorAdapter cm, GeneratorAdapter sm, ClassWriter cw, AtomicInteger atomicId, List<Object> cargs, Type thisType) {
    // If objects is null return false
    gm.loadArg(0);
    gm.ifNull(returnFalse);

    // If they are not equal return false
    gm.loadArg(0);
    gm.invokeInterface(LIST_TYPE, Method.getMethod("int size()"));
    gm.push(length);
    gm.ifICmp(GeneratorAdapter.NE, returnFalse);
  }

}
