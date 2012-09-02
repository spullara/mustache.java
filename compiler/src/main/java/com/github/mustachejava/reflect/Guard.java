package com.github.mustachejava.reflect;

import com.google.common.base.Predicate;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * Optimize guards
 */
public interface Guard extends Predicate<Object[]>, Opcodes {
  void addGuard(Label returnFalse, GeneratorAdapter gm, GeneratorAdapter cm, ClassWriter cw, int id, String className);
}
