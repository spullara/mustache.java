package com.github.mustachejava.codegen.guards;

import com.github.mustachejava.codegen.CompilableGuard;
import com.github.mustachejava.reflect.guards.ClassGuard;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.commons.GeneratorAdapter.LE;
import static org.objectweb.asm.commons.GeneratorAdapter.NE;

/**
 * Compiled form of the class guard.
 */
public class CompilableClassGuard extends ClassGuard implements CompilableGuard {

  public CompilableClassGuard(int scopeIndex, Object scope) {
    super(scopeIndex, scope);
  }

  @Override
  public void addGuard(Label returnFalse, GeneratorAdapter gm, GeneratorAdapter cm, GeneratorAdapter sm, ClassWriter cw, AtomicInteger atomicId, List<Object> cargs, Type thisType) {
    int id = atomicId.incrementAndGet();

    // Add the field for the class guard
    String fieldName = "classGuard" + id;
    cw.visitField(ACC_PUBLIC | ACC_STATIC, fieldName, "Ljava/lang/Class;", null, null);

    // Initialize the field
    sm.push(classGuard.getName());
    sm.invokeStatic(CLASS_TYPE, CLASS_FORNAME);
    sm.putStatic(thisType, fieldName, CLASS_TYPE);

    // Check that the scopes are not null
    gm.loadArg(0); // scopes
    gm.ifNull(returnFalse); // if scopes == null return false

    // Check that we have enough scopes to satisfy
    gm.loadArg(0); // scopes
    gm.arrayLength(); // scopes.length
    gm.push(scopeIndex);
    gm.ifICmp(LE, returnFalse); // scopes.length <= scopeIndex return false

    // Initialize local variables
    gm.loadArg(0); // scopes
    gm.push(scopeIndex);
    gm.arrayLoad(OBJECT_TYPE); // Object[]
    int scopeLocal = gm.newLocal(OBJECT_TYPE);
    gm.storeLocal(scopeLocal);
    int classGuardLocal = gm.newLocal(CLASS_TYPE);
    gm.getStatic(thisType, fieldName, CLASS_TYPE);
    gm.storeLocal(classGuardLocal);

    // Check to see if the scope is null
    gm.loadLocal(scopeLocal);
    Label scopeIsNull = new Label();
    gm.ifNull(scopeIsNull); // after here scope is not null

    // Check to see if the scopes class matches the guard
    gm.loadLocal(scopeLocal);
    gm.invokeVirtual(OBJECT_TYPE, OBJECT_GETCLASS); // scope.getClass()
    gm.loadLocal(classGuardLocal);
    gm.ifCmp(CLASS_TYPE, NE, returnFalse); // if they are not equal return false

    Label next = new Label();
    gm.goTo(next); // next guard

    // Check to see if the class guard itself is null
    gm.visitLabel(scopeIsNull); // after here scope is null
    gm.loadLocal(classGuardLocal);
    gm.ifNonNull(returnFalse); // if there is a class guard, return false

    // Successfully passed the guard
    gm.visitLabel(next); // end of method
  }



}
