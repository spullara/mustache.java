package com.github.mustachejava;

import org.junit.Test;

import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class AbsolutePartialReferenceTest {

    private static final String TEMPLATE_FILE = "absolute_partials_template.html";

    @Test
    public void should_load_teamplates_with_absolute_references_using_classloader() throws Exception {
        MustacheFactory factory = new DefaultMustacheFactory("templates");
        Mustache maven = factory.compile(TEMPLATE_FILE);
        StringWriter sw = new StringWriter();
        maven.execute(sw, new Object() {
            List<String> messages = Arrays.asList("w00pw00p", "mustache rocks");
        }).close();
        assertEquals("w00pw00p mustache rocks ", sw.toString());
    }

    @Test
    public void should_load_teamplates_with_absolute_references_using_filepath() throws Exception {
        File file = new File("compiler/src/test/resources/templates_filepath");
        File root = new File(file, TEMPLATE_FILE).exists() ? file : new File("src/test/resources/templates_filepath");

        MustacheFactory factory = new DefaultMustacheFactory(root);
        Mustache maven = factory.compile(TEMPLATE_FILE);
        StringWriter sw = new StringWriter();
        maven.execute(sw, new Object() {
            List<String> messages = Arrays.asList("w00pw00p", "mustache rocks");
        }).close();
        assertEquals("w00pw00p mustache rocks ", sw.toString());
    }

}
