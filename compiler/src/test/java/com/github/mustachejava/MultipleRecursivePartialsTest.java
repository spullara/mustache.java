package com.github.mustachejava;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class MultipleRecursivePartialsTest {

  private static final String TEMPLATE_FILE = "multiple_recursive_partials.html";

  private static File root;
  
  @SuppressWarnings("unused")
  private static class Model {
    Type type;
    List<Model> items;
      
    Model(Type type, List<Model> items) {
      this.type = type;
      this.items = items;
    }
      
    Model(Type type) {
      this.type = type;
    }
      
    Type getType() { return type; }
    List<Model> getItems() { return items; }
  }
  
  @SuppressWarnings("unused")
  private enum Type {
    FOO, BAR;
    boolean isFoo () { return this == FOO; }
    boolean isBar() { return this == BAR; }
  }

  @BeforeClass
  public static void setUp() throws Exception {
    File file = new File("compiler/src/test/resources");
    root = new File(file, TEMPLATE_FILE).exists() ? file : new File("src/test/resources");
  }

  @Test
  public void shouldHandleTemplateWithMultipleRecursivePartials() throws Exception {
    MustacheFactory factory = new DefaultMustacheFactory(root);
    Mustache template = factory.compile(TEMPLATE_FILE);
    StringWriter sw = new StringWriter();
    Model model = new Model(Type.FOO, Arrays.asList(new Model(Type.BAR), new Model(Type.FOO)));
    template.execute(sw, model);
    assertEquals("I'm a foo!\nIn foo: I'm a bar!\n\nIn foo: I'm a foo!\n\n\n", sw.toString());
  }

}
