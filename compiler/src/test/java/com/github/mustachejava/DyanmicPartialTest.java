package com.github.mustachejava;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class DyanmicPartialTest {
	static Map<String, String> obj = new HashMap<String, String>();

	@Test
	public void testAbstractClass() throws IOException {

		obj.put("firstName", "Check");
		obj.put("lastName", "Mate");
		obj.put("blogURL", "is something");
		obj.put("parent", "partial");

		PrintWriter pw = new PrintWriter(System.out);
		MustacheFactory mustachefactory = new DefaultMustacheFactory();
		Mustache mustacheTemplate = mustachefactory.compile("index.html");
		mustacheTemplate.execute(pw, obj).close();
	}
}
