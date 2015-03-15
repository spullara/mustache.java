package com.github.mustachejava.codegen.guards;

import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.codegen.CompilableGuard;
import com.github.mustachejava.codegen.GuardCompiler;
import com.github.mustachejava.reflect.Guard;
import com.github.mustachejava.reflect.guards.WrappedGuard;
import com.github.mustachejava.util.Wrapper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.commons.GeneratorAdapter.EQ;

/**
 * Compiled wrapper guard.
 */
public class CompilableWrappedGuard extends WrappedGuard implements CompilableGuard {
  private Guard guard;

  @SuppressWarnings("unchecked")
  public CompilableWrappedGuard(ObjectHandler oh, int index, List<Wrapper> wrappers, List<Guard> wrapperGuard) {
    super(oh, index, wrappers, wrapperGuard);
    guard = GuardCompiler.compile(wrapperGuard);
  }

  @Override
  public void addGuard(Label returnFalse, GeneratorAdapter gm, GeneratorAdapter cm, GeneratorAdapter sm, ClassWriter cw, AtomicInteger atomicId, List<Object> cargs, Type thisType) {
    int id = atomicId.incrementAndGet();

    String wrappersFieldName = "wrappers" + id;
    String ohFieldName = "oh" + id;
    String guardFieldName = "guard" + id;

    // Add the two fields we need
    cw.visitField(ACC_PRIVATE, ohFieldName, "Lcom/github/mustachejava/ObjectHandler;", null, null);
    cw.visitField(ACC_PRIVATE, wrappersFieldName, "[Lcom/github/mustachejava/util/Wrapper;", null, null);
    cw.visitField(ACC_PRIVATE, guardFieldName, "Lcom/github/mustachejava/reflect/Guard;", null, null);

    // Initialize them in the constructor
    int ohArg = cargs.size();
    cargs.add(oh);
    cm.loadThis();
    cm.loadArg(0);
    cm.push(ohArg);
    cm.invokeInterface(LIST_TYPE, Method.getMethod("Object get(int)"));
    cm.checkCast(OH_TYPE);
    cm.putField(thisType, ohFieldName, OH_TYPE);

    int wrappersArg = cargs.size();
    cargs.add(wrappers);
    cm.loadThis();
    cm.loadArg(0);
    cm.push(wrappersArg);
    cm.invokeInterface(LIST_TYPE, Method.getMethod("Object get(int)"));
    cm.checkCast(WRAPPERS_TYPE);
    cm.putField(thisType, wrappersFieldName, WRAPPERS_TYPE);

    int guardArg = cargs.size();
    cargs.add(guard);
    cm.loadThis();
    cm.loadArg(0);
    cm.push(guardArg);
    cm.invokeInterface(LIST_TYPE, Method.getMethod("Object get(int)"));
    cm.checkCast(GUARD_TYPE);
    cm.putField(thisType, guardFieldName, GUARD_TYPE);

    // Unwrap the scope
    gm.loadThis();
    gm.getField(thisType, ohFieldName, OH_TYPE);
    gm.push(index);
    gm.loadThis();
    gm.getField(thisType, wrappersFieldName, WRAPPERS_TYPE);
    gm.loadArg(0);
    gm.invokeStatic(ROH_TYPE, ROH_UNWRAP);
    int scopeLocal = gm.newLocal(OBJECT_TYPE);
    gm.storeLocal(scopeLocal);

    // Create the list
    gm.loadLocal(scopeLocal);
    gm.invokeStatic(Type.getType(ObjectHandler.class), Method.getMethod("java.util.List makeList(Object)"));
    int listLocal = gm.newLocal(Type.getType(ArrayList.class));
    gm.storeLocal(listLocal);

    // Call the apply method on the guard
    gm.loadThis();
    gm.getField(thisType, guardFieldName, GUARD_TYPE);
    gm.loadLocal(listLocal);
    gm.invokeInterface(GUARD_TYPE, GUARD_APPLY);

    // If this is false, return false
    gm.ifZCmp(EQ, returnFalse);
  }
}
