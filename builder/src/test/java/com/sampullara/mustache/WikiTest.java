package com.sampullara.mustache;

import org.junit.Test;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;

/**
 * Make sure this compiles
 * <p/>
 * User: sam
 * Date: 12/7/11
 * Time: 11:37 PM
 */
public class WikiTest {

  @Test
  public void test() throws IOException, MustacheException {
    HelloWorld.main(null);
  }
}

class HelloWorld {
  String hello = "Hello";
  String world() {return "world";}

  public static void main(String[] args) throws MustacheException, IOException {
    Writer writer = new OutputStreamWriter(System.out);
    new MustacheBuilder().build(new StringReader("{{hello}}, {{world}}!"), "helloworld").execute(writer, new Scope(new HelloWorld()));
    writer.flush();
  }
}

