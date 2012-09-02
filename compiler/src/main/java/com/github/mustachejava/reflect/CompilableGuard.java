package com.github.mustachejava.reflect;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * Optimize guards
 */
public interface CompilableGuard extends Guard, Opcodes {
  void addGuard(Label returnFalse, GeneratorAdapter gm, GeneratorAdapter sm, ClassWriter cw, int id, String className);
}
