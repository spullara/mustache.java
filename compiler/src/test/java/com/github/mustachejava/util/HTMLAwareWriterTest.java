package com.github.mustachejava.util;

import com.github.mustachejavabenchmarks.NullWriter;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static com.github.mustachejava.util.HtmlAwareWriter.Context.*;
import static junit.framework.Assert.assertEquals;

public class HtmlAwareWriterTest {

  private HtmlAwareWriter w;

  private void c(HtmlAwareWriter.Context expected) {
    assertEquals(expected, w.getState());
  }

  @Before
  public void setup() {
    w = new HtmlAwareWriter(new StringWriter());
  }

  @Test
  public void bodyToTag() throws IOException {
    c(BODY);
    w.write("<");
    c(TAG);
  }

  @Test
  public void bodyToTagName() throws IOException {
    w.write("<test");
    c(TAG_NAME);
  }

  @Test
  public void bodyToTagToError() throws IOException {
    w.write("<>");
    c(BODY);
  }

  @Test
  public void bodyToTagToBody() throws IOException {
    w.write("<test>");
    c(BODY);
    w.write("<script>");
    c(SCRIPT);
    w.write("</script><test>");
    c(BODY);
  }

  @Test
  public void bodyToAttributes() throws IOException {
    w.write("<test ");
    c(ATTRIBUTES);
  }

  @Test
  public void bodyToAttrName() throws IOException {
    w.write("<test t");
    c(ATTR_NAME);
  }

  @Test
  public void bodyToSQAttrValue() throws IOException {
    w.write("<test t= '");
    c(SQ_VALUE);
  }

  @Test
  public void startTagWithAttributes() throws IOException {
    w.write("<test");
    c(TAG_NAME);
    w.write(" ");
    c(ATTRIBUTES);
    w.write("t");
    c(ATTR_NAME);
    w.write("=");
    c(ATTR_EQUAL);
    w.write("\"test");
    c(DQ_VALUE);
    w.write("\\");
    c(ESCAPE);
    w.write("\"");
    c(DQ_VALUE);
    w.write("\"");
    c(ATTRIBUTES);
    w.write(">");
    c(BODY);
  }

  @Test
  public void bodyToNQAttrValue() throws IOException {
    w.write("<test t=t");
    c(NQ_VALUE);
  }

  @Test
  public void comment() throws IOException {
    w.write("<!--");
    c(COMMENT);
    w.write("<!-- hello -- this is a test > ");
    c(COMMENT);
    w.write("-->");
    c(BODY);
  }

  @Test
  public void endtags() throws IOException {
    w.write("<test name='value'> body stuff <");
    c(TAG);
    w.write(" /");
    c(END_TAG);
    w.write(" test");
    c(END_TAG_NAME);
    w.write(" ");
    c(AFTER_END_TAG_NAME);
    w.write(">");
    c(BODY);
  }

  @Test
  public void script() throws IOException {
    w.write("<script>window");
    c(SCRIPT);
    w.write(".title='<b>");
    c(SCRIPT_SQ_VALUE);
    w.write("\\");
    c(ESCAPE);
    w.write("'Title\\'</b>");
    w.write("'");
    c(SCRIPT);
    w.write("</scr");
    c(END_TAG_NAME);
    w.write("ipt>");
    c(BODY);
  }

  @Test
  public void pragma() throws IOException {
    w.write("<!DOCTYPE");
    c(PRAGMA);
    w.write(">");
    c(BODY);
  }

  @Test
  public void twitter() throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(ClassLoader.getSystemClassLoader().getResourceAsStream("twitter.html"), "UTF-8"));
    char[] chars = new char[32768];
    int read;
    while ((read = reader.read(chars, 0, chars.length)) != -1) {
      w.write(chars, 0, read);
    }
    c(BODY);
  }

  public static void main(String[] args) throws IOException {
    StringWriter sw = new StringWriter();
    HtmlAwareWriter writer = new HtmlAwareWriter(sw);
    BufferedReader reader = new BufferedReader(new InputStreamReader(ClassLoader.getSystemClassLoader().getResourceAsStream("twitter.html"), "UTF-8"));
    char[] chars = new char[32768];
    int read;
    while ((read = reader.read(chars, 0, chars.length)) != -1) {
      writer.write(chars, 0, read);
    }

    String value = sw.toString();
    int ITERATIONS = 1000;

    for (int j = 0; j < 10; j++) {
      long start = System.currentTimeMillis();
      for (int i = 0; i < ITERATIONS; i++) {
        Writer hawriter = new HtmlAwareWriter(new NullWriter());
        hawriter.write(value);
        hawriter.close();
      }
      long end = System.currentTimeMillis();
      System.out.println(end - start);
      start = end;

      for (int i = 0; i < ITERATIONS; i++) {
        Writer w = new StringWriter();
        w.write(value);
        w.close();
      }

      end = System.currentTimeMillis();
      System.out.println(end - start);
    }
  }
}
