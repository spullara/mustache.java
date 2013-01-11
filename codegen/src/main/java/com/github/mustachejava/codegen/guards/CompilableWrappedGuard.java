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
    cm.arrayLoad(OBJECT_TYPE);
    cm.checkCast(OH_TYPE);
    cm.putField(thisType, ohFieldName, OH_TYPE);

    int wrappersArg = cargs.size();
    cargs.add(wrappers);
    cm.loadThis();
    cm.loadArg(0);
    cm.push(wrappersArg);
    cm.arrayLoad(OBJECT_TYPE);
    cm.checkCast(WRAPPERS_TYPE);
    cm.putField(thisType, wrappersFieldName, WRAPPERS_TYPE);

    int guardArg = cargs.size();
    cargs.add(guard);
    cm.loadThis();
    cm.loadArg(0);
    cm.push(guardArg);
    cm.arrayLoad(OBJECT_TYPE);
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

    // Create the object array
    gm.push(1);
    gm.newArray(OBJECT_TYPE);
    int arrayLocal = gm.newLocal(Type.getType(Object[].class));
    gm.storeLocal(arrayLocal);

    // Put the scope in the array
    gm.loadLocal(arrayLocal);
    gm.push(0);
    gm.loadLocal(scopeLocal);
    gm.arrayStore(OBJECT_TYPE);

    // Call the apply method on the guard
    gm.loadThis();
    gm.getField(thisType, guardFieldName, GUARD_TYPE);
    gm.loadLocal(arrayLocal);
    gm.invokeInterface(GUARD_TYPE, GUARD_APPLY);

    // If this is false, return false
    gm.ifZCmp(EQ, returnFalse);
  }
}
