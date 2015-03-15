package com.github.mustachejava.codegen.guards;

import com.github.mustachejava.codegen.CompilableGuard;
import com.github.mustachejava.reflect.guards.NullGuard;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Compiled null guard.
 */
public class CompilableNullGuard extends NullGuard implements CompilableGuard {

  @Override
  public void addGuard(Label returnFalse, GeneratorAdapter gm, GeneratorAdapter cm, GeneratorAdapter sm, ClassWriter cw, AtomicInteger atomicId, List<Object> cargs, Type thisType) {
    gm.loadArg(0);
    gm.push(0);
    gm.invokeInterface(LIST_TYPE, Method.getMethod("Object get(int)"));
    gm.ifNonNull(returnFalse);
  }
}
