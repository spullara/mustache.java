package com.github.mustachejava.util;

import junit.framework.TestCase;

import java.io.StringWriter;

import static com.github.mustachejava.util.HtmlEscaper.escape;

public class HtmlEscaperTest extends TestCase {
  public void testEscape() throws Exception {
    {
      StringWriter sw = new StringWriter();
      escape("Hello, world!", sw);
      assertEquals("Hello, world!", sw.toString());
    }
    {
      StringWriter sw = new StringWriter();
      escape("Hello & world!", sw);
      assertEquals("Hello &amp; world!", sw.toString());
    }
    {
      StringWriter sw = new StringWriter();
      escape("Hello &amp; world!", sw);
      assertEquals("Hello &amp;amp; world!", sw.toString());
    }
    {
      StringWriter sw = new StringWriter();
      escape("Hello &amp world!", sw);
      assertEquals("Hello &amp;amp world!", sw.toString());
    }
    {
      StringWriter sw = new StringWriter();
      escape("\"Hello\" &amp world!", sw);
      assertEquals("&quot;Hello&quot; &amp;amp world!", sw.toString());
    }
    {
      StringWriter sw = new StringWriter();
      escape("\"Hello\" &amp world!&#10;", sw);
      assertEquals("&quot;Hello&quot; &amp;amp world!&amp;#10;", sw.toString());
    }
    {
      StringWriter sw = new StringWriter();
      escape("\"Hello\" &amp <world>!\n", sw);
      assertEquals("&quot;Hello&quot; &amp;amp &lt;world&gt;!&#10;", sw.toString());
    }
    {
      StringWriter sw = new StringWriter();
      escape("\"Hello\" &amp world!\n&sam", sw);
      assertEquals("&quot;Hello&quot; &amp;amp world!&#10;&amp;sam", sw.toString());
    }
    {
      StringWriter sw = new StringWriter();
      escape("\"Hello\" &amp 'world'!\n&sam", sw);
      assertEquals("&quot;Hello&quot; &amp;amp &#39;world&#39;!&#10;&amp;sam", sw.toString());
    }
    {
      StringWriter sw = new StringWriter();
      escape("\"Hello\" &amp 'world'!\n&sam", sw);
      assertEquals("&quot;Hello&quot; &amp;amp &#39;world&#39;!&#10;&amp;sam", sw.toString());
    }
    {
      StringWriter sw = new StringWriter();
      escape("\"Hello\" &amp&#zz 'world'!\n&sam", sw);
      assertEquals("&quot;Hello&quot; &amp;amp&amp;#zz &#39;world&#39;!&#10;&amp;sam", sw.toString());
    }
    {
      StringWriter sw = new StringWriter();
      escape("\"Hello\" &amp&#zz 'world'!\n&sam&#", sw);
      assertEquals("&quot;Hello&quot; &amp;amp&amp;#zz &#39;world&#39;!&#10;&amp;sam&amp;#", sw.toString());
    }
    {
      StringWriter sw = new StringWriter();
      escape("\"Hello\" =` 'world'!", sw);
      assertEquals("&quot;Hello&quot; &#61;&#96; &#39;world&#39;!", sw.toString());
    }
  }
}
