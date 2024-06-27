package com.github.mustachejava;

import org.junit.Test;

import java.io.StringWriter;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DynamicNamesTest {

    @Test
    public void testIssue305() {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache m = mf.compile("pd-test.mustache");

        StringWriter writer = new StringWriter();

        Map<String, String> scope = new java.util.HashMap<>();
        scope.put("name", "dn");
        m.execute(writer, scope);

        String result = writer.toString();

        assertEquals("Do dynamic names work?\n" +
                "\n" +
                "Answer: Mustache Do!", result);
    }
}
