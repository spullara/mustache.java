package com.github.mustachejava.indy;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.codegen.CodegenReflectionWrapper;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.GuardException;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Creates wrappers using ASM and Invokedynamic.
 */
public abstract class IndyWrapper extends CodegenReflectionWrapper implements Opcodes {

  public static final Handle BOOTSTRAP_METHOD =
          new Handle(Opcodes.H_INVOKESTATIC, "com/github/mustachejava/indy/IndyWrapper",
                  "bootstrap",
                  MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class,
                          MethodType.class).toMethodDescriptorString());

  private static final String METHOD_SIGNATURE =
          "(Lcom/github/mustachejava/indy/IndyWrapper;Ljava/lang/Object;)Ljava/lang/Object;";

  /**
   * This bootstrap method simply points to the lookup method so we can see what is on the stack at runtime
   * since we need the parameters in order to make a decision.
   *
   * @param caller the factory for creating method handles
   * @param name   the name of the method to call
   * @param type   the type of the method
   * @return the callsite to use, initially the lookup site that then finds the real site
   * @throws NoSuchMethodException
   * @throws IllegalAccessException
   */
  public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
    MutableCallSite callSite = new MutableCallSite(MethodType.methodType(Object.class, IndyWrapper.class, Object.class));
    MethodHandle lookup = caller.findStatic(IndyWrapper.class, "lookup",
            MethodType.methodType(Object.class, MutableCallSite.class, IndyWrapper.class,
                    Object.class));
    lookup = MethodHandles.insertArguments(lookup, 0, callSite);
    callSite.setTarget(lookup);
    return callSite;
  }

  protected IndyWrapper(ReflectionWrapper rw) {
    super(rw);
  }

  public abstract Object call(Object[] scopes) throws GuardException; // {

  public static IndyWrapper create(CodegenReflectionWrapper rw) {
    return create(rw, true);
  }

  /**
   * We replace a codegenerated reflection based wrapper with one that uses
   * invoke dynamic.
   *
   * @param rw    the original code generated wrapper
   * @param guard whether to guard the wrapper
   * @return a invokedyanmic based wrapper
   */
  public static IndyWrapper create(CodegenReflectionWrapper rw, boolean guard) {
    String name;
    Method method = rw.getMethod();
    if (method == null) {
      Field field = rw.getField();
      name = "W_" + field.getDeclaringClass().getSimpleName() + "_" + field.getName();
    } else {
      name = "W_" + method.getDeclaringClass().getSimpleName() + "_" + method.getName();
    }
    // Creates a very unique class name for the generated code
    String className = encodeClassName("com.github.mustachejava.indy", name);
    try {
      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

      // Extends the IndyWrapper class
      cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, className.replace(".", "/"), null, "com/github/mustachejava/indy/IndyWrapper", null);

      cw.visitSource(className + ".java", null);

      {
        // Call the super constructor that takes a ReflectionWrapper 
        org.objectweb.asm.commons.Method constructor = org.objectweb.asm.commons.Method.getMethod(
                "void <init> (com.github.mustachejava.reflect.ReflectionWrapper)");
        GeneratorAdapter ga = new GeneratorAdapter(ACC_PUBLIC, constructor, null, null, cw);
        ga.loadThis();
        ga.loadArg(0);
        ga.invokeConstructor(Type.getType(IndyWrapper.class), constructor);
        ga.returnValue();
        ga.endMethod();
      }
      {
        // Write the call method
        GeneratorAdapter ga = new GeneratorAdapter(ACC_PUBLIC,
                org.objectweb.asm.commons.Method.getMethod("Object call(Object[])"), null,
                new Type[]{Type.getType(GuardException.class)}, cw);
        if (guard) {
          // If we enable guards we first call the guard method
          ga.visitVarInsn(ALOAD, 0);
          ga.visitVarInsn(ALOAD, 1);
          ga.invokeVirtual(Type.getType(IndyWrapper.class),
                  org.objectweb.asm.commons.Method.getMethod("void guardCall(Object[])"));
        }
        // Unwrap the scope
        ga.visitVarInsn(ALOAD, 0);
        ga.visitVarInsn(ALOAD, 1);
        ga.invokeVirtual(Type.getType(IndyWrapper.class),
                org.objectweb.asm.commons.Method.getMethod("Object unwrap(Object[])"));
        ga.visitVarInsn(ASTORE, 2);
        ga.visitVarInsn(ALOAD, 2);
        // If the unwrapped scope is null, return null
        Label l0 = new Label();
        ga.ifNonNull(l0);
        ga.visitInsn(ACONST_NULL);
        ga.returnValue();
        // If not null, we bootstrap the invokedynamic process
        ga.visitLabel(l0);
        ga.visitVarInsn(ALOAD, 0);
        ga.visitVarInsn(ALOAD, 2);
        ga.invokeDynamic("bootstrap", METHOD_SIGNATURE, BOOTSTRAP_METHOD);
        ga.returnValue();
        ga.endMethod();
      }
      cw.visitEnd();
      Class<?> aClass = defineClass(className, cw.toByteArray());
      return (IndyWrapper) aClass.getConstructor(ReflectionWrapper.class).newInstance(rw);
    } catch (Exception e) {
      throw new MustacheException(e);
    }
  }

  public static String encodeClassName(String pkgName, String name) {
    String uuid = UUID.randomUUID().toString().replace("-", "_");
    return pkgName + "." + name + "_" + uuid;
  }

  private static final IndyClassLoader indyCL = new IndyClassLoader(Thread.currentThread().getContextClassLoader());

  private static class IndyClassLoader extends ClassLoader {

    public IndyClassLoader(ClassLoader parent) {
      super(parent);
    }

    public Class<?> defineClass(final String name, final byte[] b) {
      return defineClass(name, b, 0, b.length);
    }
  }

  public static Class<?> defineClass(String name, byte[] b) {
    return indyCL.defineClass(name, b);
  }

  /**
   * This bootstrap method does the actual work of tracking down the CallSite at runtime.
   *
   * @param callSite the callsite that called lookup, this is redirected to the actual field or method
   * @param iw       the wrapper around the real call site
   * @param scope    the current object scope
   * @return the result of executing the method against the scope
   * @throws IllegalAccessException
   * @throws InvocationTargetException
   */
  public static Object lookup(MutableCallSite callSite, IndyWrapper iw, Object scope) throws IllegalAccessException, InvocationTargetException {
    Method method = iw.getMethod();
    if (method == null) {
      Field field = iw.getField();
      MethodHandle unreflect = MethodHandles.lookup().unreflectGetter(field);
      unreflect = MethodHandles.dropArguments(unreflect, 0, IndyWrapper.class);
      setCallsite(callSite, unreflect);
      return field.get(scope);
    } else {
      MethodHandle unreflect = MethodHandles.lookup().unreflect(method);
      unreflect = MethodHandles.dropArguments(unreflect, 0, IndyWrapper.class);
      if (method.getParameterTypes().length != 0) {
        for (int i = 0; i < iw.getArguments().length; i++) {
          unreflect = MethodHandles.insertArguments(unreflect, i + 2, iw.getArguments()[i]);
        }
      }
      setCallsite(callSite, unreflect);
      return method.invoke(scope, iw.getArguments());
    }
  }

  private static void setCallsite(MutableCallSite callSite, MethodHandle unreflect) {
    // change where the callsite points - make sure that all the arguments of the
    // unreflected method match the arguments of our callsite.
    callSite.setTarget(MethodHandles.explicitCastArguments(unreflect,
            MethodType.methodType(Object.class, IndyWrapper.class, Object.class)));
  }
}
