package com.github.mustachejava.codegen;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.reflect.Guard;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.commons.Method.getMethod;

/**
 * Compiles Compilable Guards.
 */
public class GuardCompiler {
  private static AtomicInteger id = new AtomicInteger(0);

  public static Guard compile(List<? extends Guard> guards) {
    List<CompilableGuard> compilableGuards = new ArrayList<CompilableGuard>();
    for (Guard guard : guards) {
      if (guard instanceof CompilableGuard) {
        compilableGuards.add((CompilableGuard) guard);
      } else {
        throw new MustacheException("Invalid guard for compilation: " + guard);
      }
    }
    try {
      return compile("compiledguards:" + guards.size(), compilableGuards);
    } catch (Exception e) {
      throw new MustacheException("Compilation failure", e);
    }
  }

  private static Guard compile(String source, Iterable<CompilableGuard> guards) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    int classId = id.incrementAndGet();
    String className = "com.github.mustachejava.codegen.CompiledGuards" + classId;
    String internalClassName = className.replace(".", "/");
    cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, internalClassName, null, "java/lang/Object", new String[]{Guard.class.getName().replace(".", "/")});
    cw.visitSource(source, null);

    // Constructor
    GeneratorAdapter cm = new GeneratorAdapter(Opcodes.ACC_PUBLIC, getMethod("void <init> (Object[])"), null, null, cw);
    cm.loadThis();
    cm.invokeConstructor(Type.getType(Object.class), getMethod("void <init> ()"));

    // Static initializer
    GeneratorAdapter sm = new GeneratorAdapter(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, getMethod("void <clinit> ()"), null, null, cw);

    // Method implementation
    GeneratorAdapter gm = new GeneratorAdapter(Opcodes.ACC_PUBLIC, getMethod("boolean apply(Object[])"), null, null, cw);
    Label returnFalse = new Label();

    // Add each guard in the list
    List<Object> cargs = new ArrayList<Object>();
    for (CompilableGuard guard : guards) {
      guard.addGuard(returnFalse, gm, cm, sm, cw, id, cargs, Type.getType(internalClassName));
    }

    // Makes it through the guard, success
    gm.push(true);
    gm.returnValue();
    // Jumps to returnFalse, failure
    gm.visitLabel(returnFalse);
    gm.push(false);
    gm.returnValue();
    gm.endMethod();

    // Close the constructor
    cm.returnValue();
    cm.endMethod();

    // Close the static initializer
    sm.returnValue();
    sm.endMethod();

    cw.visitEnd();
    Class<?> aClass = defineClass(className, cw.toByteArray());
    return (Guard) aClass.getConstructor(Object[].class).newInstance((Object) cargs.toArray(new Object[cargs.size()]));
  }

  private static final DefiningClassLoader cl = new DefiningClassLoader(Thread.currentThread().getContextClassLoader());

  private static class DefiningClassLoader extends ClassLoader {
    public DefiningClassLoader(ClassLoader parent) {
      super(parent);
    }

    public Class<?> defineClass(final String name, final byte[] b) {
      return defineClass(name, b, 0, b.length);
    }
  }

  public static Class<?> defineClass(String name, byte[] b) {
    return cl.defineClass(name, b);
  }

}
