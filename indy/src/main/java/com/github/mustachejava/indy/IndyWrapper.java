package com.github.mustachejava.indy;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.GuardException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

/**
 * Creates wrappers using ASM and Invokedynamic.
 */
public abstract class IndyWrapper extends ReflectionWrapper implements Opcodes {

  public static final Handle BOOTSTRAP_METHOD =
          new Handle(Opcodes.H_INVOKESTATIC, "com/github/mustachejava/indy/IndyWrapper",
                  "bootstrap",
                  MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class,
                          MethodType.class).toMethodDescriptorString());

  private static final String METHOD_SIGNATURE =
          "(Lcom/github/mustachejava/indy/IndyWrapper;Ljava/lang/Object;)Ljava/lang/Object;";

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

  @Override
  public Object call(Object[] scopes) throws GuardException {
    guardCall(scopes);
    Object scope = unwrap(scopes);
    if (scope == null) return null;
    return indy(scope);
  }

  public abstract Object indy(Object scope);

  public static IndyWrapper create(ReflectionWrapper rw) {
    String className = encodeClassName("com.github.mustachejava.indy", "Wrapper");
    try {
      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

      cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, className.replace(".", "/"), null, "com/github/mustachejava/indy/IndyWrapper", null);

      cw.visitSource(className + ".java", null);

      {
        org.objectweb.asm.commons.Method constructor = org.objectweb.asm.commons.Method.getMethod(
                "void <init> (com.github.mustachejava.reflect.ReflectionWrapper)");
        GeneratorAdapter ga = new GeneratorAdapter(ACC_PUBLIC, constructor, null, null, cw);
        ga.loadThis();
        ga.loadArg(0);
        ga.invokeConstructor(Type.getType(ReflectionWrapper.class), constructor);
        ga.returnValue();
        ga.endMethod();
      }
      {
        GeneratorAdapter ga = new GeneratorAdapter(ACC_PROTECTED,
                org.objectweb.asm.commons.Method.getMethod("Object indy(Object)"), null, null, cw);
        ga.loadThis();
        ga.loadArg(0);
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
  private static final IndyClassLoader indyCL = new IndyClassLoader();
  private static class IndyClassLoader extends ClassLoader {
    public Class<?> defineClass(final String name, final byte[] b) {
      return defineClass(name, b, 0, b.length);
    }
  }

  public static Class<?> defineClass(String name, byte[] b) {
    return indyCL.defineClass(name, b);
  }

  public static Object lookup(MutableCallSite callSite, IndyWrapper iw, Object scope) throws IllegalAccessException, InvocationTargetException {
    Method method = iw.getMethod();
    if (method == null) {
      Field field = iw.getField();
      MethodHandle unreflect = MethodHandles.lookup().unreflectGetter(field);
      unreflect = MethodHandles.dropArguments(unreflect, 0, IndyWrapper.class);
      unreflect = MethodHandles.explicitCastArguments(unreflect,
              MethodType.methodType(Object.class, IndyWrapper.class, Object.class));
      callSite.setTarget(unreflect);
      return field.get(scope);
    } else {
      MethodHandle unreflect = MethodHandles.lookup().unreflect(method);
      unreflect = MethodHandles.dropArguments(unreflect, 0, IndyWrapper.class);
      if (method.getParameterTypes().length != 0) {
        for (int i = 0; i < iw.getArguments().length; i++) {
          unreflect = MethodHandles.insertArguments(unreflect, i + 2, iw.getArguments()[i]);
        }
      }
      unreflect = MethodHandles.explicitCastArguments(unreflect,
              MethodType.methodType(Object.class, IndyWrapper.class, Object.class));
      callSite.setTarget(unreflect);
      return method.invoke(scope, iw.getArguments());
    }
  }
}
