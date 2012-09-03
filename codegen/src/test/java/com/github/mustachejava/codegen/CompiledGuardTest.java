package com.github.mustachejava.codegen;

import com.github.mustachejava.codegen.guards.CompilableClassGuard;
import com.github.mustachejava.reflect.Guard;
import com.github.mustachejava.reflect.guards.ClassGuard;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static com.github.mustachejava.codegen.GuardCompiler.compile;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Test our guard compilations
 */
public class CompiledGuardTest implements Opcodes {

  @Test
  public void testGuard() {
    ClassGuard stringClassGuard = new ClassGuard(0, "");
    assertTrue("string is ok", stringClassGuard.apply(new Object[]{"test"}));
    assertFalse("integer is not ok", stringClassGuard.apply(new Object[]{1}));
  }

  @Test
  public void testCompiledGuard() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
    String source = "Test.java";
    CompilableClassGuard stringClassGuard = new CompilableClassGuard(0, "");
    List<CompilableGuard> guards = new ArrayList<CompilableGuard>();
    guards.add(stringClassGuard);

    Guard testGuard = compile(source, guards);
    assertTrue("string is ok", testGuard.apply(new Object[]{"test", 1}));
    assertFalse("integer is not ok", testGuard.apply(new Object[]{1, "test"}));
  }

}