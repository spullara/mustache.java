package com.github.mustachejava.asm;

import com.github.mustachejava.Code;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.io.Writer;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.commons.Method.getMethod;

/**
 * Compile a list of codes to execute down to a single method.
 */
public class CodeCompiler {
  private static AtomicInteger id = new AtomicInteger(0);
  private static final Method EXECUTE_METHOD = Method.getMethod("java.io.Writer execute(java.io.Writer, Object[])");

  public static CompiledCodes compile(Code[] codes, Code[] newcodes) {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    int classId = id.incrementAndGet();
    String className = "com.github.mustachejava.codes.RunCodes" + classId;
    String internalClassName = className.replace(".", "/");
    cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, internalClassName, null, "java/lang/Object", new String[]{CompiledCodes.class.getName().replace(".", "/")});
    cw.visitSource("runCodes", null);

    GeneratorAdapter cm = new GeneratorAdapter(Opcodes.ACC_PUBLIC, getMethod("void <init> (com.github.mustachejava.Code[])"), null, null, cw);
    cm.loadThis();
    cm.invokeConstructor(Type.getType(Object.class), getMethod("void <init> ()"));
    {
      GeneratorAdapter gm = new GeneratorAdapter(Opcodes.ACC_PUBLIC, getMethod("java.io.Writer runCodes(java.io.Writer, Object[])"), null, null, cw);
      int writerLocal = gm.newLocal(Type.getType(Writer.class));
      // Put the writer in our local
      gm.loadArg(0);
      gm.storeLocal(writerLocal);
      int fieldNum = 0;
      for (Code newcode : newcodes) {
        Class<? extends Code> codeClass = newcode.getClass();
        Class fieldClass = codeClass;
        while(fieldClass.isAnonymousClass() || fieldClass.isLocalClass() || (fieldClass.getModifiers() & Modifier.PUBLIC) == 0) {
          if (codeClass.getSuperclass() != Object.class && codeClass.getSuperclass().isAssignableFrom(Code.class)) {
            fieldClass = codeClass.getSuperclass();
          } else {
            fieldClass = Code.class;
          }
        }
        Type fieldType = Type.getType(fieldClass);

        // add a field for each one to the class
        String fieldName = "code" + fieldNum;
        cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, fieldName, fieldType.getDescriptor(), null, null);

        // set the fields to the passed in values in the constructor
        cm.loadThis();
        cm.loadArg(0);
        cm.push(fieldNum);
        cm.arrayLoad(Type.getType(Code.class));
        cm.checkCast(fieldType);
        cm.putField(Type.getType(internalClassName), fieldName, fieldType);

        // writer, scopes)
        gm.loadThis();
        gm.getField(Type.getType(internalClassName), fieldName, fieldType);
        gm.loadLocal(writerLocal);
        gm.loadArg(1);
        // code.execute(
        if (fieldClass.isInterface()) {
          gm.invokeInterface(fieldType, EXECUTE_METHOD);
        } else {
          gm.invokeVirtual(fieldType, EXECUTE_METHOD);
        }
        // writer =
        gm.storeLocal(writerLocal);

        fieldNum++;
      }
      cm.returnValue();
      cm.endMethod();

      // Load writer and return it
      gm.loadLocal(writerLocal);
      gm.returnValue();
      gm.endMethod();
    }

    cw.visitEnd();
    Class<?> aClass = GuardCompiler.defineClass(className, cw.toByteArray());
    try {
      return (CompiledCodes) aClass.getConstructor(Code[].class).newInstance(new Object[] {codes});
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
