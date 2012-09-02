package com.github.mustachejava.reflect;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.InvocationTargetException;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.objectweb.asm.commons.Method.getMethod;

/**
 * Test our guard compilations
 */
public class CompiledGuard implements Opcodes {

  @Test
  public void testGuard() {
    ClassGuard stringClassGuard = new ClassGuard(0, "");
    assertTrue("string is ok", stringClassGuard.apply(new Object[]{"test"}));
    assertFalse("integer is not ok", stringClassGuard.apply(new Object[]{1}));
  }

  @Test
  public void testCompiledGuard() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
    String className = "Test";
    String source = "Test.java";
    ClassGuard stringClassGuard = new ClassGuard(0, "");

    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", new String[]{Guard.class.getName().replace(".", "/")});
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
      stringClassGuard.addGuard(returnFalse, gm, sm, cw, 0, className);
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
    Guard testGuard = (Guard) aClass.getConstructor().newInstance();
    assertTrue("string is ok", testGuard.apply(new Object[]{"test", 1}));
    assertFalse("integer is not ok", testGuard.apply(new Object[]{1, "test"}));
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
