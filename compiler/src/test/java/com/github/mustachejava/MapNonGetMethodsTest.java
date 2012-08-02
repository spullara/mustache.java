package com.github.mustachejava;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.reflect.SimpleObjectHandler;

import static org.junit.Assert.assertEquals;

public class MapNonGetMethodsTest {
  
  /**
   * Extended reflection handler that can access map methods.
   */
  private class MapMethodReflectionHandler extends ReflectionObjectHandler {
    @Override
    protected boolean areMethodsAccessible(Map<?, ?> map) {
      return true;
    }
  }
  
  private class SimpleMapMethodHandler extends SimpleObjectHandler {
    @Override
    protected boolean areMethodsAccessible(Map<?, ?> map) {
      return true;
    }
  }

  private static final String TEMPLATE = "{{empty}}";
  
  private DefaultMustacheFactory factory;

  @Before
  public void setUp() {
    factory = new DefaultMustacheFactory();
  }
  
  @Test
  public void testMethodAccessDisallowed() {
    Map<String, Object> model = new HashMap<String, Object>();
    
    Reader reader = new StringReader(TEMPLATE);
    Mustache mustache = factory.compile(reader, "template");
    
    verifyOutput("", model, mustache);
  }
  
  @Test
  public void testMethodAccessAllowed() {
    Map<String, Object> model = new HashMap<String, Object>();
    
    factory.setObjectHandler(new MapMethodReflectionHandler());
    Reader reader = new StringReader(TEMPLATE);
    Mustache mustache = factory.compile(reader, "template");
    
    verifyOutput("true", model, mustache);
  }
  
  @Test
  public void testWrapperCaching() {
    factory.setObjectHandler(new MapMethodReflectionHandler());
    Reader reader = new StringReader(TEMPLATE);
    Mustache mustache = factory.compile(reader, "template");
    
    Map<String, String> model = new HashMap<String, String>();
    verifyOutput("true", model, mustache);
    
    model.put("empty", "data");
    verifyOutput("data", model, mustache);
  }
  

  @Test
  public void testSimpleHandlerMethodAccessDisallowed() {
    Map<String, Object> model = new HashMap<String, Object>();
    
    factory.setObjectHandler(new SimpleObjectHandler());
    Reader reader = new StringReader(TEMPLATE);
    Mustache mustache = factory.compile(reader, "template");
    
    verifyOutput("", model, mustache);
  }
  
  @Test
  public void testSimpleHandlerMethodAccessAllowed() {
    Map<String, Object> model = new HashMap<String, Object>();
    
    factory.setObjectHandler(new SimpleMapMethodHandler());
    Reader reader = new StringReader(TEMPLATE);
    Mustache mustache = factory.compile(reader, "template");
    
    verifyOutput("true", model, mustache);
  }
  
  private void verifyOutput(String expected, Object model, Mustache mustache) {
    StringWriter writer = new StringWriter();
    mustache.execute(writer, model);
    
    assertEquals(expected, writer.toString());
  }

}
