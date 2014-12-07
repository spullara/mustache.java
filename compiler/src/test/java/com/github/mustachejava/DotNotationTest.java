package com.github.mustachejava;

import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

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
      
  
}
