package com.github.mustachejava;

import com.github.mustachejava.reflect.guards.ClassGuard;
import com.github.mustachejava.reflect.guards.DepthGuard;
import com.github.mustachejava.reflect.guards.DotGuard;
import com.github.mustachejava.reflect.guards.NullGuard;
import com.github.mustachejava.util.DecoratedCollection;
import com.github.mustachejava.util.NodeValue;
import org.junit.Test;

import java.util.Arrays;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Checks the JEP 401 value classes (JDK 28 preview): no identity, and == compares state.
 */
public class ValueClassesTest {

  @Test
  public void testNoIdentity() {
    TemplateContext tc = new TemplateContext("{{", "}}", "a.mustache", 1, true);
    assertFalse(Objects.hasIdentity(tc));
    assertFalse(Objects.hasIdentity(new FragmentKey(tc, "x")));
    assertFalse(Objects.hasIdentity(NodeValue.value("x")));
    assertFalse(Objects.hasIdentity(new ClassGuard(0, "s")));
    assertFalse(Objects.hasIdentity(new DepthGuard(1)));
    assertFalse(Objects.hasIdentity(new DotGuard("a.b", 0, "s")));
    assertFalse(Objects.hasIdentity(new NullGuard()));
    Object element = new DecoratedCollection<>(Arrays.asList("a")).iterator().next();
    assertFalse(Objects.hasIdentity(element));
  }

  @Test
  public void testSubstitutability() {
    TemplateContext tc1 = new TemplateContext("{{", "}}", "a.mustache", 1, true);
    TemplateContext tc2 = new TemplateContext("{{", "}}", "a.mustache", 1, true);
    assertTrue(tc1 == tc2);
    assertTrue(new FragmentKey(tc1, "x") == new FragmentKey(tc2, "x"));
    assertTrue(new DepthGuard(3) == new DepthGuard(3));
    assertTrue(new NullGuard() == new NullGuard());
  }

  @Test
  public void testClassGuardIncludesScopeIndex() {
    ClassGuard g0 = new ClassGuard(0, "s");
    ClassGuard g1 = new ClassGuard(1, "s");
    assertNotEquals(g0, g1);
    assertFalse(g0 == g1);
    assertEquals(g0, new ClassGuard(0, "t"));
    assertEquals(g0.hashCode(), new ClassGuard(0, "t").hashCode());
  }
}
