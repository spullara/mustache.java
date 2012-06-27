package com.github.mustachejava;

import org.junit.Before;
import org.junit.Test;

import javax.management.modelmbean.ModelMBean;
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

    public static final String TEMPLATE = "{{object.data}}";

    private class TestObject {

        private Object data;

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }

    private Mustache template;

    @Before
    public void setUp() {
        MustacheFactory factory = new DefaultMustacheFactory();
        template = factory.compile(new StringReader(TEMPLATE), "template");
    }

    /**
     * Test that initial misses on dot-notation are not incorrectly cached.
     */
    @Test
    public void testInitialMiss() {
        Map<String, Object> model = new HashMap<String, Object>();
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
        Map<String, Object> model = new HashMap<String, Object>();
        assertEquals("", render(template, model));

        Map<String, String> object = new HashMap<String, String>();
        object.put("data", "hit");
        model.put("object", object);
        assertEquals("hit", render(template, model));
    }

    public String render(Mustache template, Object data) {
        Writer writer = new StringWriter();
        template.execute(writer, data);
        return writer.toString();
    }

    public String render(Mustache template, Object[] scopes) {
        Writer writer = new StringWriter();
        template.execute(writer, scopes);
        return writer.toString();
    }


}
