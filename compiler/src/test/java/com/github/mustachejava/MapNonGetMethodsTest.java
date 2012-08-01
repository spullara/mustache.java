package com.github.mustachejava;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.github.mustachejava.reflect.SimpleObjectHandler;

import static org.junit.Assert.assertEquals;

public class MapNonGetMethodsTest {

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
    
    StringWriter writer = new StringWriter();
    mustache.execute(writer, model);
    
    assertEquals("", writer.toString());
  }
  
  @Test
  public void testMethodAccessAllowed() {
    Map<String, Object> model = new HashMap<String, Object>();
    
    factory.getObjectHandler().setMapMethodsAccessible(true);
    Reader reader = new StringReader(TEMPLATE);
    Mustache mustache = factory.compile(reader, "template");
    
    StringWriter writer = new StringWriter();
    mustache.execute(writer, model);
    
    assertEquals("true", writer.toString());
  }
  

  @Test
  public void testSimpleHandlerMethodAccessDisallowed() {
    Map<String, Object> model = new HashMap<String, Object>();
    
    factory.setObjectHandler(new SimpleObjectHandler());
    Reader reader = new StringReader(TEMPLATE);
    Mustache mustache = factory.compile(reader, "template");
    
    StringWriter writer = new StringWriter();
    mustache.execute(writer, model);
    
    assertEquals("", writer.toString());
  }
  
  @Test
  public void testSimpleHandlerMethodAccessAllowed() {
    Map<String, Object> model = new HashMap<String, Object>();
    
    factory.setObjectHandler(new SimpleObjectHandler());
    factory.getObjectHandler().setMapMethodsAccessible(true);
    Reader reader = new StringReader(TEMPLATE);
    Mustache mustache = factory.compile(reader, "template");
    
    StringWriter writer = new StringWriter();
    mustache.execute(writer, model);
    
    assertEquals("true", writer.toString());
  }

}
