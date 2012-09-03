package com.github.mustachejava.compile;

import com.github.mustachejava.reflect.Guard;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimizable guards
 */
public interface CompilableGuard extends Guard, Opcodes {
  void addGuard(Label returnFalse, GeneratorAdapter gm, GeneratorAdapter sm, ClassWriter cw, AtomicInteger id, String className);
}
