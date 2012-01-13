package com.github.mustachejava.indy;

import java.lang.reflect.AccessibleObject;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.Wrapper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

/**
 * Creates custom classes instead of using reflection for handling objects. Leverages
 * the ReflectionObjectHandler to create the original wrappers and converts them to
 * new versions.
 */
public class IndyObjectHandler extends ReflectionObjectHandler implements Opcodes {

  @Override
  public Wrapper find(String name, Object[] scopes) {
    ReflectionWrapper wrapper = (ReflectionWrapper) find(name, scopes);
    try {
      // Create a new wrapper per wrapper that is implemented with Indy
    } catch (Exception e) {
      throw new MustacheException("Failed to generate class", e);
    }
    return wrapper;
  }

  @Override
  public Object coerce(Object object) {
    return super.coerce(object);
  }

  @Override
  protected Wrapper createWrapper(int scopeIndex, Wrapper[] wrappers, Class[] guard, AccessibleObject member, Object[] arguments) {
    return super.createWrapper(scopeIndex, wrappers, guard, member, arguments);
  }

  public static byte[] createClass(String className, String superClassName) throws Exception {
    className = className.replace(".", "/");
    superClassName = superClassName.replace(".", "/");

    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    MethodVisitor mv;

    cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, className,
            null, superClassName, null);

    cw.visitSource(className + ".java", null);

    {
      mv = cw.visitMethod(ACC_PUBLIC, "<init>",
              "(Lcom/sampullara/mustache/Mustache;Ljava/lang/String;ZI)V", null, null);
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitVarInsn(ILOAD, 3);
      mv.visitVarInsn(ILOAD, 4);
      mv.visitMethodInsn(INVOKESPECIAL, superClassName, "<init>",
              "(Lcom/sampullara/mustache/Mustache;Ljava/lang/String;ZI)V");
      mv.visitInsn(RETURN);
      mv.visitMaxs(5, 5);
      mv.visitEnd();
    }
    {
      GeneratorAdapter ga = new GeneratorAdapter(ACC_PROTECTED,
              Method.getMethod("Object getValue(com.sampullara.mustache.Scope)"), null, null, cw);
      ga.loadThis();
      ga.getField(Type.getType(className), "name", Type.getType(String.class));
      ga.loadArg(0);
      ga.invokeDynamic("getValue",
              "(Ljava/lang/String;Lcom/sampullara/mustache/Scope;)Ljava/lang/Object;",
              IndyUtil.BOOTSTRAP_METHOD);
      ga.returnValue();
      ga.endMethod();
    }
    cw.visitEnd();

    return cw.toByteArray();
  }
}
