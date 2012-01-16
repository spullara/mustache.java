package com.github.mustachejava.indy;

import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.Wrapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.util.TraceClassVisitor;
import sun.reflect.Reflection;

import java.io.PrintWriter;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.sql.Ref;
import java.util.UUID;

/**
 * Utility class for Indy development.
 */
public class IndyUtil {
  private static final boolean debug = Boolean.getBoolean("mustache.indy.debug");

  private static final IndyClassLoader indyCL = new IndyClassLoader();
  private static class IndyClassLoader extends ClassLoader {
    public Class<?> defineClass(final String name, final byte[] b) {
      return defineClass(name, b, 0, b.length);
    }
  }

  public static Class<?> defineClass(String name, byte[] b) {
    return indyCL.defineClass(name, b);
  }
  
  public static final Handle BOOTSTRAP_METHOD =
          new Handle(Opcodes.H_INVOKESTATIC, "com/github/mustachejava/indy/IndyUtil", "bootstrap",
                  MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class,
                          MethodType.class, String.class).toMethodDescriptorString());

  public static String getUUID(String pkgName, String name) {
                            String uuid = UUID.randomUUID().toString().replace("-", "_");
                            return pkgName + "." + name + "_" + uuid;
                          }

  public static ClassWriter createBridgeClass(String className) throws Exception {
    className = className.replace(".", "/");

    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    MethodVisitor mv;

    cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, className, null, "java/lang/Object", null);

    cw.visitSource(className + ".java", null);

    {
      mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
      mv.visitCode();
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
      mv.visitInsn(Opcodes.RETURN);
      mv.visitMaxs(2, 2);
      mv.visitEnd();
    }

    return cw;
  }

  public static CallSite bootstrap(MethodHandles.Lookup caller, String method, MethodType type, String name) throws NoSuchMethodException, IllegalAccessException {
    MethodHandle lookupHandle = MethodHandles.lookup().findStatic(IndyUtil.class, "lookup",
            MethodType.methodType(Object.class, MutableCallSite.class, String.class, Object[].class));
    MutableCallSite callSite = new MutableCallSite(
            MethodType.methodType(Object.class, Object[].class));
    lookupHandle = MethodHandles.insertArguments(lookupHandle, 1, name);
    callSite.setTarget(lookupHandle.bindTo(callSite));
    return callSite;
  }

  public static Object returnNull() {
    return null;
  }

  private static ReflectionObjectHandler roh = new ReflectionObjectHandler();

  public static Object lookup(MutableCallSite callSite, String name, Object[] scopes) throws Throwable {
    // Here we do the lookup all the way down to the method
    // and generate code to find the the object at runtime in the scope
    ReflectionWrapper wrapper = (ReflectionWrapper) roh.find(name, scopes);
    if (wrapper == null) return null;
    return wrapper.call(scopes);
  }

}
