package com.github.mustachejava.asm;

import com.github.mustachejava.reflect.Guard;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimizable guards
 */
public interface CompilableGuard extends Guard, Opcodes {
  void addGuard(Label returnFalse,
                GeneratorAdapter gm,
                GeneratorAdapter cm,
                GeneratorAdapter sm,
                ClassWriter cw,
                AtomicInteger atomicId,
                String className,
                List<Object> cargs);
}
