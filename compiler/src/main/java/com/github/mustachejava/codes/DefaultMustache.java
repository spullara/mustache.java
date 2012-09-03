package com.github.mustachejava.codes;

import com.github.mustachejava.Code;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.reflect.CompilableGuard;
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
 * Default Mustache
 */
public class DefaultMustache extends DefaultCode implements Mustache, Opcodes {
  private static AtomicInteger id = new AtomicInteger(0);
  private static final Method EXECUTE_METHOD = Method.getMethod("java.io.Writer execute(java.io.Writer, Object[])");

  private Code[] codes;
  private boolean inited = false;

  public DefaultMustache(TemplateContext tc, MustacheFactory cf, Code[] codes, String name) {
    super(tc, cf.getObjectHandler(), null, name, null);
    setCodes(codes);
  }

  @Override
  public Code[] getCodes() {
    return codes;
  }

  private CompiledCodes compiledCodes = null;

  public Writer runCodes(Writer writer, Object[] scopes) {
    return compiledCodes == null ? writer : compiledCodes.runCodes(writer, scopes);
  }

  @Override
  public final void setCodes(Code[] newcodes) {
    codes = newcodes;
    if (codes == null) {
      compiledCodes = null;
    } else {
      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
      int classId = id.incrementAndGet();
      String className = "com.github.mustachejava.codes.RunCodes" + classId;
      String internalClassName = className.replace(".", "/");
      cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, internalClassName, null, "java/lang/Object", new String[]{CompiledCodes.class.getName().replace(".", "/")});
      cw.visitSource("runCodes", null);

      GeneratorAdapter cm = new GeneratorAdapter(ACC_PUBLIC, getMethod("void <init> (com.github.mustachejava.Code[])"), null, null, cw);
      cm.loadThis();
      cm.invokeConstructor(Type.getType(Object.class), getMethod("void <init> ()"));
      {
        GeneratorAdapter gm = new GeneratorAdapter(ACC_PUBLIC, getMethod("java.io.Writer runCodes(java.io.Writer, Object[])"), null, null, cw);
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
          cw.visitField(ACC_PRIVATE | ACC_FINAL, fieldName, fieldType.getDescriptor(), null, null);

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
      Class<?> aClass = CompilableGuard.Compiler.defineClass(className, cw.toByteArray());
      try {
        compiledCodes = (CompiledCodes) aClass.getConstructor(Code[].class).newInstance(new Object[] { codes });
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void identity(Writer writer) {
    // No self output at the top level
    runIdentity(writer);
  }

  @Override
  public synchronized void init() {
    if (!inited) {
      inited = true;
      super.init();
    }
  }
}
