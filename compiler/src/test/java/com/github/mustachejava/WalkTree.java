package com.github.mustachejava;

import com.github.mustachejava.codes.IterableCode;
import com.github.mustachejava.codes.NotIterableCode;
import com.github.mustachejava.codes.ValueCode;
import org.junit.Test;

import java.io.StringReader;

/**
 * Created by sam on 6/5/17.
 */
public class WalkTree {

  @Test
  public void testTree() {
    MustacheFactory mf = new DefaultMustacheFactory();
    Mustache test = mf.compile(new StringReader("Hello {{firstName}} {{lastName}}\n" +
            "Your Education Qualification is\n" +
            "\n" +
            "{{#qualification}}\n" +
            "     - {{ college}}\n" +
            "     - {{ years }}\n" +
            "{{/qualification }}"), "test");
    walk(test, 0);
  }

  private void walk(Code test, int depth) {
    for (Code code : test.getCodes()) {
      if (code instanceof ValueCode) {
        indent(depth);
        System.out.println("- " + code.getName());
      } else if (code instanceof NotIterableCode) {
        System.out.println("^" + code.getName());
        walk(code, depth + 2);
      } else if (code instanceof IterableCode) {
        System.out.println("#" + code.getName());
        walk(code, depth + 2);
      }
    }
  }

  private void indent(int depth) {
    for (int i = 0; i < depth; i++) {
      System.out.print(" ");
    }
  }
}
