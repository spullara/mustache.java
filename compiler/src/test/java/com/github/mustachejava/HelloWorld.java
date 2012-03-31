package com.github.mustachejava;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;

public class HelloWorld {
  //  String hello = "Hello";
  String world() {return "world";}

  public static void main(String[] args) throws MustacheException, IOException {
    Writer writer = new OutputStreamWriter(System.out);
    MustacheFactory mf = new DefaultMustacheFactory();
    Mustache mustache = mf.compile(new StringReader("{{hello}}, {{world}}!"), "helloworld");
    mustache.execute(writer, new HelloWorld());
    writer.flush();
  }
}