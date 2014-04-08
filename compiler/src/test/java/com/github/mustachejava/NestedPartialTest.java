package com.github.mustachejava;

import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class NestedPartialTest {

  private static final String TEMPLATE_FILE = "nested_partials_template.html";

  private static File root;

  @BeforeClass
  public static void setUp() throws Exception {
    File file = new File("compiler/src/test/resources");
    root = new File(file, TEMPLATE_FILE).exists() ? file : new File("src/test/resources");
  }

  @Test
  public void should_handle_more_than_one_level_of_partial_nesting() throws Exception {
    MustacheFactory factory = new DefaultMustacheFactory(root);
    Mustache maven = factory.compile(TEMPLATE_FILE);
    StringWriter sw = new StringWriter();
    maven.execute(sw, new Object() {
      List<String> messages = Arrays.asList("w00pw00p", "mustache rocks");
    }).close();
    assertEquals("w00pw00p mustache rocks ", sw.toString());
  }

}
