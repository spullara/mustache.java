package com.github.mustachejava.indy;

import java.lang.reflect.AccessibleObject;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;
import com.sun.org.apache.xml.internal.security.utils.I18n;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import static com.github.mustachejava.indy.IndyUtil.BOOTSTRAP_METHOD;

/**
 * Creates custom classes instead of using reflection for handling objects. Leverages
 * the ReflectionObjectHandler to create the original wrappers and converts them to
 * new versions.
 */
public class IndyObjectHandler extends ReflectionObjectHandler implements Opcodes {

  private static final Method CALL_METHOD = Method.getMethod("Object call(Object[])");
  private static final Type[] GUARD_EXCEPTION = new Type[]{Type.getType(GuardException.class)};

  @Override
  public Wrapper find(String name, Object[] scopes) {
    try {
      String bootstrap = IndyUtil.getUUID("com.github.mustachejava.indy", "Bootstrap");
      Class<?> aClass = IndyUtil.defineClass(bootstrap, createClass(name, bootstrap, "java.lang.Object"));
      return (Wrapper) aClass.newInstance();
    } catch (Exception e) {
      throw new MustacheException(e);
    }
  }

  public static byte[] createClass(String name, String className, String superClassName) throws Exception {
    className = className.replace(".", "/");
    superClassName = superClassName.replace(".", "/");

    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    MethodVisitor mv;

    cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, className, null, superClassName, new String[] { "com/github/mustachejava/util/Wrapper"});

    cw.visitSource(className + ".java", null);

    {
      Method noargConstructor = Method.getMethod("void <init> ()");
      GeneratorAdapter ga = new GeneratorAdapter(ACC_PUBLIC, noargConstructor, null, null, cw);
      ga.loadThis();
      ga.invokeConstructor(Type.getType(Object.class), noargConstructor);
      ga.returnValue();
      ga.endMethod();
    }
    {
      GeneratorAdapter ga = new GeneratorAdapter(ACC_PUBLIC, CALL_METHOD, null, GUARD_EXCEPTION, cw);
      ga.loadArg(0);
      ga.invokeDynamic("call", "([Ljava/lang/Object;)Ljava/lang/Object;", BOOTSTRAP_METHOD, name);
      ga.returnValue();
      ga.endMethod();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }
}
