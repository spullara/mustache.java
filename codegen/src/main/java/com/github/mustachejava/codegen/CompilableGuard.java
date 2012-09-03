package com.github.mustachejava.codegen;

import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.reflect.Guard;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.Wrapper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimizable guards
 */
public interface CompilableGuard extends Guard, Opcodes {
  Type OBJECT_TYPE = Type.getType(Object.class);
  Type CLASS_TYPE = Type.getType(Class.class);
  Type MAP_TYPE = Type.getType(Map.class);
  Type OH_TYPE = Type.getType(ObjectHandler.class);
  Type WRAPPERS_TYPE = Type.getType(Wrapper[].class);
  Type ROH_TYPE = Type.getType(ReflectionObjectHandler.class);
  Type GUARD_TYPE = Type.getType(Guard.class);
  Method CLASS_FORNAME = Method.getMethod("Class forName(String)");
  Method OBJECT_GETCLASS = Method.getMethod("Class getClass()");
  Method ROH_UNWRAP = Method.getMethod("Object unwrap(com.github.mustachejava.ObjectHandler, int, com.github.mustachejava.util.Wrapper[], Object[])");
  Method MAP_CONTAINSKEY = Method.getMethod("boolean containsKey(Object)");
  Method GUARD_APPLY = Method.getMethod("boolean apply(Object[])");

  public abstract void addGuard(Label returnFalse,
                                GeneratorAdapter gm,
                                GeneratorAdapter cm,
                                GeneratorAdapter sm,
                                ClassWriter cw,
                                AtomicInteger atomicId,
                                List<Object> cargs, Type thisType);
}
