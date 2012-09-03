package com.github.mustachejava.reflect;

import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.asm.CompilableGuard;
import com.github.mustachejava.util.Wrapper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.mustachejava.reflect.ReflectionObjectHandler.unwrap;

/**
 * Guards whether or not a name was present in the map.
 */
public class MapGuard implements CompilableGuard {
  private final ObjectHandler oh;
  private final int scopeIndex;
  private final String name;
  private final boolean contains;
  private final Wrapper[] wrappers;

  public MapGuard(ObjectHandler oh, int scopeIndex, String name, boolean contains, Wrapper[] wrappers) {
    this.oh = oh;
    this.scopeIndex = scopeIndex;
    this.name = name;
    this.contains = contains;
    this.wrappers = wrappers;
  }

  @Override
  public boolean apply(Object[] objects) {
    Object scope = unwrap(oh, scopeIndex, wrappers, objects);
    if (scope instanceof Map) {
      Map map = (Map) scope;
      if (contains) {
        return map.containsKey(name);
      } else {
        return !map.containsKey(name);
      }
    }
    return false;
  }

  @Override
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
    gm.ifZCmp(GeneratorAdapter.EQ, returnFalse);

    // It is a map
    gm.loadLocal(scopeLocal);
    gm.checkCast(MAP_TYPE);
    gm.push(name);
    if (contains) {
      gm.invokeInterface(MAP_TYPE, MAP_CONTAINSKEY);
      gm.ifZCmp(GeneratorAdapter.EQ, returnFalse);
    } else {
      gm.invokeInterface(MAP_TYPE, MAP_CONTAINSKEY);
      gm.ifZCmp(GeneratorAdapter.NE, returnFalse);
    }
  }
}
