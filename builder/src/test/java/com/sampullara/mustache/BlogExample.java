package com.sampullara.mustache;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class BlogExample {
  public static void main(String[] args) throws MustacheException, IOException {
    String template = Files.toString(new File("src/test/resources/template.mustache"), Charsets.UTF_8);
    String content = Files.toString(new File("src/test/resources/template.html"), Charsets.UTF_8);
    Mustache mustache = new MustacheBuilder().parse(template, "template");
    Scope unexecuted = mustache.unexecute(content);
    System.out.println(unexecuted);
  }
}
