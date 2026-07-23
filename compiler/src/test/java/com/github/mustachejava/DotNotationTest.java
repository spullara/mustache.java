package com.github.mustachejava;

import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class DotNotationTest {

  private static final String EARLY_MISS_TEMPLATE = "{{container1.container2.target}}";
  private static final String LAST_ELEMENT_MISS_TEMPLATE = "{{container1.nothing}}";
  
  private static final class ModelObject {
      @SuppressWarnings("unused")
      public Object getContainer2() {
          return null;
      }
  }
  
  private MustacheFactory factory;
  private Map<String, Object> mapModel;
  private Map<String, Object> objectModel;
  
  @Before
  public void setUp() {
      factory = new DefaultMustacheFactory();
      
      mapModel = new HashMap<>();
      Map<String, Object> container1 = new HashMap<>();
      mapModel.put("container1", container1);
      
      objectModel = new HashMap<>();
      objectModel.put("container1", new ModelObject());
  }
  

  @Test
  public void testIncompleteMapPath() {
      testMiss(mapModel, EARLY_MISS_TEMPLATE);
  }
  
  @Test
  public void testAlmostCompleteMapPath() {
      testMiss(mapModel, LAST_ELEMENT_MISS_TEMPLATE);
  }
  
  @Test
  public void testIncompleteObjectPath() {
      testMiss(objectModel, EARLY_MISS_TEMPLATE);
  }

  @Test
  public void testAlmostCompleteObjectPath() {
      testMiss(objectModel, LAST_ELEMENT_MISS_TEMPLATE);
  }

  @Test
  public void testDottedSectionPushesIntermediateScope() throws Exception {
    Map<String, Object> bar = map();
    Map<String, Object> foo = map("bar", bar, "baz", "from foo");
    Map<String, Object> model = map("foo", foo);

    String dotted = render(compile("{{#foo.bar}}{{baz}}{{/foo.bar}}"), model);
    String nested = render(compile("{{#foo}}{{#bar}}{{baz}}{{/bar}}{{/foo}}"), model);

    assertEquals("from foo", dotted);
    assertEquals(nested, dotted);
  }

  @Test
  public void testDottedSectionPrefersFinalScope() throws Exception {
    Map<String, Object> bar = map("baz", "from bar");
    Map<String, Object> foo = map("bar", bar, "baz", "from foo");

    assertEquals("from bar", render(compile("{{#foo.bar}}{{baz}}{{/foo.bar}}"), map("foo", foo)));
  }

  @Test
  public void testThreePartDottedSectionPushesAllScopes() throws Exception {
    Map<String, Object> c = map("fromC", "c");
    Map<String, Object> b = map("c", c, "fromB", "b");
    Map<String, Object> a = map("b", b, "fromA", "a");

    assertEquals("a/b/c", render(compile(
            "{{#a.b.c}}{{fromA}}/{{fromB}}/{{fromC}}{{/a.b.c}}"), map("a", a)));
  }

  @Test
  public void testDottedSectionPreservesContextPrecedence() throws Exception {
    Map<String, Object> a = map("b", map());
    Map<String, Object> model = map("a", a, "c", true);

    assertEquals("", render(compile("{{#a.b.c}}ERROR{{/a.b.c}}"), model));
  }

  @Test
  public void testBrokenDottedSectionChainRendersNothing() throws Exception {
    assertEquals("", render(compile("{{#a.b.c}}ERROR{{/a.b.c}}"), map("a", map())));
  }

  @Test
  public void testLiteralDottedKeyDoesNotPushIntermediateScope() throws Exception {
    Mustache mustache = compile("{{#foo.bar}}{{baz}}{{/foo.bar}}");
    Map<String, Object> literalOnly = map("foo.bar", map("value", true), "baz", "root");
    Map<String, Object> collision = map(
            "foo.bar", map("value", true),
            "foo", map("baz", "wrong intermediate"),
            "baz", "root");

    assertEquals("root", render(mustache, literalOnly));
    assertEquals("root", render(mustache, collision));
  }

  @Test
  public void testInvertedDottedSectionBehaviorIsUnchanged() throws Exception {
    Mustache mustache = compile("{{^foo.bar}}missing{{/foo.bar}}");

    assertEquals("", render(mustache, map("foo", map("bar", true))));
    assertEquals("missing", render(mustache, map("foo", map())));
    assertEquals("", render(mustache, map("foo.bar", true)));
  }

  @Test
  public void testCallableIntermediateMatchesNestedSections() throws Exception {
    Callable<Object> foo = () -> map(
            "bar", (Function<String, String>) text -> "baz");
    Map<String, Object> model = map("foo", foo);

    String dotted = render(compile("{{#foo.bar}}quux{{/foo.bar}}"), model);
    String nested = render(compile("{{#foo}}{{#bar}}quux{{/bar}}{{/foo}}"), model);

    assertEquals("baz", dotted);
    assertEquals(nested, dotted);
  }

  @Test
  public void testCallableIntermediateInvocationCount() throws Exception {
    AtomicInteger dottedInvocations = new AtomicInteger();
    Callable<Object> dottedFoo = () -> {
      dottedInvocations.incrementAndGet();
      return map("bar", true);
    };
    AtomicInteger nestedInvocations = new AtomicInteger();
    Callable<Object> nestedFoo = () -> {
      nestedInvocations.incrementAndGet();
      return map("bar", true);
    };

    assertEquals("dotted", render(compile("{{#foo.bar}}dotted{{/foo.bar}}"), map("foo", dottedFoo)));
    assertEquals("nested", render(compile("{{#foo}}{{#bar}}nested{{/bar}}{{/foo}}"), map("foo", nestedFoo)));
    assertEquals("Dotted lookup and resolution each invoke the intermediate once", 2, dottedInvocations.get());
    assertEquals(1, nestedInvocations.get());
  }

  @Test
  public void testNullCallableBreaksDottedSectionChain() throws Exception {
    Callable<Object> foo = () -> null;

    assertEquals("", render(compile("{{#foo.bar}}ERROR{{/foo.bar}}"), map("foo", foo)));
  }

  @Test
  public void testCallableInMiddleOfThreePartName() throws Exception {
    Callable<Object> b = () -> map("c", map("fromC", "c"), "fromB", "b");
    Map<String, Object> a = map("b", b, "fromA", "a");

    assertEquals("a/b/c", render(compile(
            "{{#a.b.c}}{{fromA}}/{{fromB}}/{{fromC}}{{/a.b.c}}"), map("a", a)));
  }

  @Test
  public void testAlternatingDottedSectionShapesReuseCachedWrappers() throws Exception {
    Mustache mustache = compile("{{#foo.bar}}{{baz}}{{/foo.bar}}");
    Map<String, Object> literal = map(
            "foo.bar", map("value", true),
            "foo", map("baz", "wrong intermediate"),
            "baz", "literal");
    Map<String, Object> nested = map(
            "foo", map("bar", map(), "baz", "nested"),
            "baz", "wrong root");
    Callable<Object> foo = () -> map("bar", map(), "baz", "callable");
    Map<String, Object> callable = map("foo", foo, "baz", "wrong root");

    for (int i = 0; i < 2; i++) {
      assertEquals("literal", render(mustache, literal));
      assertEquals("nested", render(mustache, nested));
      assertEquals("callable", render(mustache, callable));
    }
  }
  
  private void testMiss(Object model, String template) {
      Mustache mustache = compile(template);
      StringWriter writer = new StringWriter();
      mustache.execute(writer, model);
      
      assertEquals("", writer.toString());
  }

  private Mustache compile(String template) {
      Reader reader = new StringReader(template);
      return factory.compile(reader, "template");
  }

  private String render(Mustache mustache, Object model) throws Exception {
    StringWriter writer = new StringWriter();
    mustache.execute(writer, model).close();
    return writer.toString();
  }

  private static Map<String, Object> map(Object... entries) {
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i < entries.length; i += 2) {
      map.put((String) entries[i], entries[i + 1]);
    }
    return map;
  }
      
  
}
