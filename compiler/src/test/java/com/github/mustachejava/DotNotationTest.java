package com.github.mustachejava;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class DotNotationTest {

  private static final String TEMPLATE = "{{container1.container2.target}}";
  
  private static final class ModelObject {
      public Object getContainer2() {
          return null;
      }
  }
  
  private Mustache mustache;
  
  @Before
  public void setUp() {
      MustacheFactory factory = new DefaultMustacheFactory();
      Reader reader = new StringReader(TEMPLATE);
      mustache = factory.compile(reader, "template");
  }
  

  @Test
  public void testIncompleteMapPath() {
      Map<String, Object> model = new HashMap<String, Object>();
      Map<String, Object> container1 = new HashMap<String, Object>();
      model.put("container1", container1);
      StringWriter writer = new StringWriter();
      mustache.execute(writer, model);
      
      assertEquals("", writer.toString());
  }

  @Test
  public void testIncompleteObjectPath() {
      Map<String, Object> model = new HashMap<String, Object>();
      Object container1 = new ModelObject();
      model.put("container1", container1);
      StringWriter writer = new StringWriter();
      mustache.execute(writer, model);
      
      assertEquals("", writer.toString());
  }

}
