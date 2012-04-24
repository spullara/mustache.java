package com.github.mustachejava;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExplicitMapNullTest {

    private static final String TEMPLATE = "{{nullData}}";
    
    private Mustache mustache;
    
    @Before
    public void setUp() {
        MustacheFactory factory = new DefaultMustacheFactory();
        Reader reader = new StringReader(TEMPLATE);
        mustache = factory.compile(reader, "template");
    }
    

    @Test
    public void textExplicitNullMapValue() {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("nullData", null);
        
        StringWriter writer = new StringWriter();
        mustache.execute(writer, model);
        
        assertEquals("", writer.toString());
    }
    
    
    
}
