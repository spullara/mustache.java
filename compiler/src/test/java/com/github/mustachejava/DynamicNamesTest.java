package com.github.mustachejava;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

public class DynamicNamesTest {

    private static final String TEST_TEMPLATES_DIR = "dynamicNames";
    private static final String TEMPLATE_FILE = "monkey.mustache";

    private static File root;

    @BeforeClass
    public static void setUp() throws Exception {
        File file = new File("compiler/src/test/resources/" + TEST_TEMPLATES_DIR);
        root = new File(file, TEMPLATE_FILE).exists() ? file : new File("src/test/resources/" + TEST_TEMPLATES_DIR);
    }

    @Test
    public void dynamic_names_should_work() throws Exception {
        MustacheFactory factory = new DefaultMustacheFactory(root);
        Mustache mustache = factory.compile(TEMPLATE_FILE);
        StringWriter sw = new StringWriter();

        mustache.execute(sw, Map.of("actions", List.of(Map.of("action", "s-action"), Map.of("action", "d-action"))));

        String expected = "monkey see\nmonkey do\n";

        assertEquals(expected, sw.toString());
    }


}
