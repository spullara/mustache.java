package com.github.mustachejava.reflect;

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
 * Optimize guards
 */
public interface CompilableGuard extends Guard, Opcodes {
  void addGuard(Label returnFalse, GeneratorAdapter gm, GeneratorAdapter sm, ClassWriter cw, AtomicInteger id, String className);

  public static class Compiler {
    private static AtomicInteger id = new AtomicInteger(0);

    public static Guard[] compile(Guard[] guards) {
      List<Guard> finalGuards = new ArrayList<Guard>();
      List<CompilableGuard> compilableGuards = new ArrayList<CompilableGuard>();
      for (Guard guard : guards) {
        if (guard instanceof CompilableGuard) {
          compilableGuards.add((CompilableGuard) guard);
        } else {
          finalGuards.add(guard);
        }
      }
      try {
        Guard compiledGuard = compile("compiledguards", compilableGuards);
        finalGuards.add(0, compiledGuard);
        return finalGuards.toArray(new Guard[finalGuards.size()]);
      } catch (Exception e) {
        e.printStackTrace();
        return guards;
      }
    }

    public static Guard compile(String source, Iterable<CompilableGuard> guards) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
      int classId = id.incrementAndGet();
      String className = "com.github.mustachejava.reflect.CompiledGuards" + classId;
      String internalClassName = className.replace(".", "/");
      cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, internalClassName, null, "java/lang/Object", new String[]{Guard.class.getName().replace(".", "/")});
      cw.visitSource(source, null);

      GeneratorAdapter cm = new GeneratorAdapter(ACC_PUBLIC, getMethod("void <init> ()"), null, null, cw);
      cm.loadThis();
      cm.invokeConstructor(Type.getType(Object.class), getMethod("void <init> ()"));
      cm.returnValue();
      cm.endMethod();

      GeneratorAdapter sm = new GeneratorAdapter(ACC_PUBLIC | ACC_STATIC, getMethod("void <clinit> ()"), null, null, cw);

      {
        GeneratorAdapter gm = new GeneratorAdapter(ACC_PUBLIC, getMethod("boolean apply(Object[])"), null, null, cw);
        Label returnFalse = new Label();
        for (CompilableGuard guard : guards) {
          guard.addGuard(returnFalse, gm, sm, cw, id, internalClassName);
        }

        // Makes it through the guard, success
        gm.push(true);
        gm.returnValue();
        // Jumps to returnFalse, failure
        gm.visitLabel(returnFalse);
        gm.push(false);
        gm.returnValue();
        gm.endMethod();
      }

      sm.returnValue();
      sm.endMethod();

      cw.visitEnd();
      Class<?> aClass = defineClass(className, cw.toByteArray());
      return (Guard) aClass.getConstructor().newInstance();
    }

    private static final DefiningClassLoader cl = new DefiningClassLoader();

    private static class DefiningClassLoader extends ClassLoader {
      public Class<?> defineClass(final String name, final byte[] b) {
        return defineClass(name, b, 0, b.length);
      }
    }

    public static Class<?> defineClass(String name, byte[] b) {
      return cl.defineClass(name, b);
    }

  }
}
