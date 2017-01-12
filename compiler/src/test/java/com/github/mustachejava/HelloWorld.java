package com.github.mustachejava;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.mustachejava.codes.*;

public class HelloWorld {
  //  String hello = "Hello";
  String world() {return "world";}

  public static void main(String[] args) throws MustacheException, IOException {
	  DefaultMustacheFactory dmf = new DefaultMustacheFactory() {
	      @Override
	      public MustacheVisitor createMustacheVisitor() {
	        return new DefaultMustacheVisitor(this) {
	          @Override
	          public void value(TemplateContext tc, String variable, boolean encoded) {
	            list.add(new ValueCode(tc, df, variable, encoded) {
	              @Override
	              public Writer execute(Writer writer, List<Object> scopes) {
	                try {
	                  final Object object = get(scopes);
	                  if (object == null) {
	                    identity(writer);
	                  }
	                  return super.execute(writer, scopes);
	                } catch (Exception e) {
	                  throw new MustacheException("Failed to get value for " + name, e, tc);
	                }
	              }
	            });
	          }
	        };
	      }
	    };
	   // Mustache test = dmf.compile(new StringReader("{{name}} -<a src=\"{{test.other}}\"> xyz {{email}}"), "test");
	    Mustache test = dmf.compile(new StringReader("{{name}} -\"{{test.other}}\" xyz {{email}}"), "test");
	    //Mustache test = dmf.compile(new StringReader("{{name}} - {{email}}"), "test");
	    StringWriter sw = new StringWriter();
	    Map<Object, Object> map = new HashMap<Object, Object>();
	    map.put("name", "Sam Pullara");
	    test.execute(sw, map).close();
	    System.out.print(sw.toString());
  }
 
}