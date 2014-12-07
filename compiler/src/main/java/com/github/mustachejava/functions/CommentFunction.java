package com.github.mustachejava.functions;

import java.util.function.Function;

/**
 * Mustache.java block comment function.
 *
 * Usage code with a class:
 *   public class ... {
 *   	CommentFunction comment = new CommentFunction();
 *   	...
 *   }
 *
 * Usage code with a Map:
 *   HashMap<String, Object> scopes = new HashMap<String, Object>();
 *   scopes.put("comment", new CommentFunction());
 *   ...
 *
 * Usage in template:
 *   ... {{#comment}} Your multiline comment text {{/comment}} ...
 *
 * @author gw0 [http://gw.tnode.com/] <gw.2012@tnode.com>
 */
public class CommentFunction implements Function<String, String> {
	
	/** Ignore contents of comment block. */
	@Override
	public String apply(String input) {
		return "";
	}
}
