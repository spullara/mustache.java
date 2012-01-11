package com.github.mustachejava.indy;

import com.github.mustachejava.ObjectHandler;
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

import java.io.PrintWriter;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
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
          new Handle(Opcodes.H_INVOKESTATIC, "com/sampullara/mustache/indy/IndyUtil", "bootstrap",
                  MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class,
                          MethodType.class).toMethodDescriptorString());

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

  public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
    MethodHandle lookupHandle = MethodHandles.lookup().findStatic(IndyUtil.class, "lookup",
            MethodType.methodType(Object.class, MutableCallSite.class, String.class, Object[].class));
    MutableCallSite callSite = new MutableCallSite(
            MethodType.methodType(Object.class, String.class, Object[].class));
    callSite.setTarget(lookupHandle.bindTo(callSite));
    return callSite;
  }

  public static Object returnNull() {
    return null;
  }

  public static Object lookup(MutableCallSite callSite, String name, Object[] scope) throws Throwable {
    // Here we do the lookup all the way down to the method
    // and generate code to find the the object at runtime in the scope
    Object[] originalScope = scope;

    // Find the target field or method
    AccessibleObject ao = null;
    Object parent = originalScope.getParent();
    if (parent != null) {
      ObjectHandler objectHandler = scope.getObjectHandler();
      ao = objectHandler.getMember(name, name.getClass());
    }
    if (ao == null) {
      while (ao == null && (scope = scope.getParentScope()) != null) {
        parent = scope.getParent();
        if (parent != null) {
          ObjectHandler objectHandler = scope.getObjectHandler();
          ao = objectHandler.getMember(name, parent.getClass());
        }
      }
    }
    if (ao == null || ao == DefaultObjectHandler.NOTHING) {
      MethodHandle returnNull = MethodHandles.constant(Object.class, null);
      MethodHandle newTarget = MethodHandles.dropArguments(returnNull, 0, String.class, Object[].class);
      callSite.setTarget(newTarget);
      return null;
    }

    // Second pass to generate the class to access the field / method
    int line = 1;
    String pkgName = parent.getClass().getPackage().getName();
    String simpleName = parent.getClass().getName();
    simpleName = simpleName.substring(simpleName.lastIndexOf(".") + 1);
    String className = getUUID(pkgName, simpleName + "$" + name);
    ClassWriter classWriter = createBridgeClass(className);
    GeneratorAdapter ga = new GeneratorAdapter(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
            Method.getMethod("Object getObject(com.sampullara.mustache.Scope)"), null, null,
            classWriter);
    ao = null;
    scope = originalScope;
    parent = scope.getParent();
    if (parent != null) {
      ObjectHandler objectHandler = scope.getObjectHandler();
      ao = objectHandler.getMember(name, name.getClass());
    }
    if (ao == null) {
      ga.loadArg(0);
      while (ao == null && (scope = scope.getParentScope()) != null) {
        parent = scope.getParent();
        if (parent != null) {
          ObjectHandler objectHandler = scope.getObjectHandler();
          ao = objectHandler.getMember(name, parent.getClass());
          if (ao != null) {
            Label label = new Label();
            ga.visitLabel(label);
            ga.visitLineNumber(line++, label);
            ga.invokeVirtual(Type.getType(Scope.class), Method.getMethod("com.sampullara.mustache.Scope getParentScope()"));
            Label label2 = new Label();
            ga.visitLabel(label2);
            ga.visitLineNumber(line++, label2);
            ga.invokeVirtual(Type.getType(Scope.class), Method.getMethod("Object getParent()"));
          }
        } else {
          Label label = new Label();
          ga.visitLabel(label);
          ga.visitLineNumber(line++, label);
          ga.invokeVirtual(Type.getType(Scope.class), Method.getMethod("com.sampullara.mustache.Scope getParentScope()"));
        }
      }
    } else {
      ga.loadArg(0);
      Label label = new Label();
      ga.visitLabel(label);
      ga.visitLineNumber(line++, label);
      ga.invokeVirtual(Type.getType(Object.class), Method.getMethod("Object getParent()"));
    }
    if (ao == null) {
      throw new AssertionError();
    }

    // We have the object on the stack, now we need to call the method or field
    Type parentType = Type.getType(parent.getClass());
    ga.checkCast(parentType);
    if (ao instanceof Field) {
      Label label = new Label();
      ga.visitLabel(label);
      ga.visitLineNumber(line++, label);
      Field field = (Field) ao;
      ga.getField(parentType, field.getName(), Type.getType(((Field) ao).getType()));
    } else {
      java.lang.reflect.Method method = (java.lang.reflect.Method) ao;
      if (method.getParameterTypes().length == 0) {
        Label label = new Label();
        ga.visitLabel(label);
        ga.visitLineNumber(line++, label);
        ga.invokeVirtual(parentType, Method.getMethod(method));
      } else {
        ga.loadArg(0);
        Label label = new Label();
        ga.visitLabel(label);
        ga.visitLineNumber(line++, label);
        ga.invokeVirtual(parentType, Method.getMethod(method));
      }
    }
    Label label = new Label();
    ga.visitLabel(label);
    ga.visitLineNumber(line, label);
    ga.returnValue();
    ga.endMethod();
    classWriter.visitEnd();
    byte[] b = classWriter.toByteArray();
    if (debug) {
      PrintWriter printWriter = new PrintWriter(System.out, true);
      ClassVisitor cv = new TraceClassVisitor(printWriter);
      new ClassReader(b).accept(cv, 0);
      printWriter.flush();
    }
    Class<?> bridgeClass = indyCL.defineClass(className, b);

    MethodHandle targetMethod = MethodHandles.lookup().findStatic(bridgeClass,
            "getObject", MethodType.methodType(Object.class, Scope.class));
    Object o = targetMethod.invokeWithArguments(originalScope);
    targetMethod = MethodHandles.dropArguments(targetMethod, 0, String.class);
    callSite.setTarget(targetMethod);
    return o;
  }

}
