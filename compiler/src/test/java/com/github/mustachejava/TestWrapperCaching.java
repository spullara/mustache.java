package com.github.mustachejava;

import org.junit.Before;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Test that wrappers are not cached too aggressively,
 * causing false misses or hits.
 */
public class TestWrapperCaching {

  private static final String TEMPLATE = "{{object.data}}";
  private static final String SCOPES_TEMPLATE = "{{#scope2}}{{#scope1}}{{data.data}}{{/scope1}}{{/scope2}}";


  private class TestObject {

    public TestObject() {}
    public TestObject(Object data) {
      this.data = data;
    }

    private Object data;

    public Object getData() {
      return data;
    }

    public void setData(Object data) {
      this.data = data;
    }

    public String toString() {
      return "{data=" + data + "}";
    }
  }

  private Mustache template;
  private Mustache scopesTemplate;

  @Before
  public void setUp() {
    MustacheFactory factory = new DefaultMustacheFactory();
    template = factory.compile(new StringReader(TEMPLATE), "template");
    scopesTemplate = factory.compile(new StringReader(SCOPES_TEMPLATE), "template");
  }

  /**
   * Test that initial misses on dot-notation are not incorrectly cached.
   */
  @Test
  public void testInitialMiss() {
    Map<String, Object> model = new HashMap<>();
    assertEquals("", render(template, model));

    TestObject object = new TestObject();
    object.setData("hit");
    model.put("object", object);
    assertEquals("hit", render(template, model));
  }

  /**
   * Test that initial misses on map dot notation are not incorrectly cached.
   */
  @Test
  public void testMapInitialMiss() {
    Map<String, Object> model = new HashMap<>();
    assertEquals("", render(template, model));

    Map<String, String> object = new HashMap<>();
    object.put("data", "hit");
    model.put("object", object);
    assertEquals("hit", render(template, model));
  }

  @Test
  public void testMultiScopeInitialHit() {
    Map<String, Object> model = new HashMap<>();
    model.put("scope1", "foo"); //scope 1 full miss
    model.put("scope2", new TestObject(new TestObject("hit"))); //scope 2 dot hit

    assertEquals("hit", render(scopesTemplate, model));

    model.put("scope2", new TestObject()); //scope2 dot miss
    assertEquals("", render(scopesTemplate, model));
  }

  @Test
  public void testMultiScopeInitialHit2() {
    Map<String, Object> model = new HashMap<>();
    model.put("scope1", new TestObject(new TestObject("hit"))); //scope 1 hit
    model.put("scope2", "foo"); //scope 2 full miss (shouldn't matter)

    assertEquals("hit", render(scopesTemplate, model));

    model.put("scope1", new TestObject()); //scope1 dot miss
    assertEquals("", render(scopesTemplate, model));
  }

  @Test
  public void testMultiScopeInitialMiss() {
    Map<String, Object> model = new HashMap<>();
    model.put("scope1", new TestObject()); //scope 1 dot miss
    model.put("scope2", "foo"); //scope 2 full miss (shouldn't matter)

    assertEquals("", render(scopesTemplate, model));

    model.put("scope1", new TestObject(new TestObject("hit"))); //scope 1 dot hit
    assertEquals("hit", render(scopesTemplate, model));
  }

  @Test
  public void testMultiScopeInitialMiss2() {
    Map<String, Object> model = new HashMap<>();
    model.put("scope1", "foo"); //scope 1 full miss
    model.put("scope2", new TestObject()); //scope 2 dot miss

    assertEquals("", render(scopesTemplate, model));

    model.put("scope2", new TestObject(new TestObject("hit"))); //scope 2 hit
    assertEquals("hit", render(scopesTemplate, model));
  }



  private String render(Mustache template, Object data) {
    Writer writer = new StringWriter();
    template.execute(writer, data);
    return writer.toString();
  }

}