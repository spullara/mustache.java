package com.github.mustachejava.codegen.guards;

import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.codegen.CompilableGuard;
import com.github.mustachejava.reflect.guards.MapGuard;
import com.github.mustachejava.util.Wrapper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.commons.GeneratorAdapter.EQ;
import static org.objectweb.asm.commons.GeneratorAdapter.NE;

/**
 * Compiled version of map guard.
 */
public class CompilableMapGuard extends MapGuard implements CompilableGuard {

  public CompilableMapGuard(ObjectHandler oh, int scopeIndex, String name, boolean contains, Wrapper[] wrappers) {
    super(oh, scopeIndex, name, contains, wrappers);
  }

  public void addGuard(Label returnFalse, GeneratorAdapter gm, GeneratorAdapter cm, GeneratorAdapter sm,
                       ClassWriter cw, AtomicInteger atomicId, List<Object> cargs, Type thisType) {
    int id = atomicId.incrementAndGet();

    String wrappersFieldName = "wrappers" + id;
    String ohFieldName = "oh" + id;

    // Add the two fields we need
    cw.visitField(ACC_PRIVATE, ohFieldName, "Lcom/github/mustachejava/ObjectHandler;", null, null);
    cw.visitField(ACC_PRIVATE, wrappersFieldName, "[Lcom/github/mustachejava/util/Wrapper;", null, null);

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

    // Unwrap the scope
    gm.loadThis();
    gm.getField(thisType, ohFieldName, OH_TYPE);
    gm.push(scopeIndex);
    gm.loadThis();
    gm.getField(thisType, wrappersFieldName, WRAPPERS_TYPE);
    gm.loadArg(0);
    gm.invokeStatic(ROH_TYPE, ROH_UNWRAP);
    int scopeLocal = gm.newLocal(OBJECT_TYPE);
    gm.storeLocal(scopeLocal);

    // Check to see if it is a map
    gm.loadLocal(scopeLocal);
    gm.instanceOf(MAP_TYPE);
    gm.ifZCmp(EQ, returnFalse);

    // It is a map
    gm.loadLocal(scopeLocal);
    gm.checkCast(MAP_TYPE);
    gm.push(name);
    gm.invokeInterface(MAP_TYPE, MAP_CONTAINSKEY);
    gm.ifZCmp(contains ? EQ : NE, returnFalse);
  }

}
