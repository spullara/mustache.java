package com.github.mustachejava.reflect;

import com.github.mustachejava.asm.CompilableGuard;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.commons.GeneratorAdapter.LE;
import static org.objectweb.asm.commons.GeneratorAdapter.NE;

/**
 * Ensure that the class of the current scope is that same as when this wrapper was generated.
 * User: spullara
 * Date: 4/13/12
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class ClassGuard implements CompilableGuard {
  private final Class classGuard;
  private final int scopeIndex;

  public ClassGuard(int scopeIndex, Object scope) {
    this.scopeIndex = scopeIndex;
    this.classGuard = scope == null ? null : scope.getClass();
  }

  @Override
  public int hashCode() {
    return classGuard == null ? 0 : classGuard.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    ClassGuard other = (ClassGuard) o;
    return o instanceof ClassGuard && (classGuard == null ? classGuard == other.classGuard : classGuard.equals(other.classGuard));
  }

  @Override
  public boolean apply(Object[] scopes) {
    if (scopes == null || scopes.length <= scopeIndex) return false;
    Object scope = scopes[scopeIndex];
    if (scope != null && classGuard != scope.getClass()) return false;
    if (scope == null && classGuard != null) return false;
    return true;
  }

  @Override
  public void addGuard(Label returnFalse, GeneratorAdapter gm, GeneratorAdapter cm, GeneratorAdapter sm, ClassWriter cw, AtomicInteger atomicId, String className, List<Object> cargs) {
    int id = atomicId.incrementAndGet();

    // Add the field for the class guard
    String fieldName = "classGuard" + id;
    cw.visitField(ACC_PUBLIC | ACC_STATIC, fieldName, "Ljava/lang/Class;", null, null);

    // Initialize the field
    sm.push(classGuard.getName());
    sm.invokeStatic(Type.getType(Class.class), Method.getMethod("Class forName(String)"));
    sm.putStatic(Type.getType(className), fieldName, Type.getType(Class.class));

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
    gm.arrayLoad(Type.getType(Object.class)); // Object[]
    int scopeLocal = gm.newLocal(Type.getType(Object.class));
    gm.storeLocal(scopeLocal);
    int classGuardLocal = gm.newLocal(Type.getType(Class.class));
    gm.getStatic(Type.getType(className), fieldName, Type.getType(Class.class));
    gm.storeLocal(classGuardLocal);

    // Check to see if the scope is null
    gm.loadLocal(scopeLocal);
    Label scopeIsNull = new Label();
    gm.ifNull(scopeIsNull); // after here scope is not null

    // Check to see if the scopes class matches the guard
    gm.loadLocal(scopeLocal);
    gm.invokeVirtual(Type.getType(Object.class), Method.getMethod("Class getClass()")); // scope.getClass()
    gm.loadLocal(classGuardLocal);
    gm.ifCmp(Type.getType(Class.class), NE, returnFalse); // if they are not equal return false

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
