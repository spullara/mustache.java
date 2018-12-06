package com.github.mustachejava.util;

import com.github.mustachejava.MustacheException;

import java.io.IOException;
import java.io.Writer;

/**
 * Escapes user data that you wish to include in HTML pages.
 */
public class HtmlEscaper {

  private static char[][] ESC = new char[97][];

  static {
    char[] AMP = "&amp;".toCharArray();
    char[] LT = "&lt;".toCharArray();
    char[] GT = "&gt;".toCharArray();
    char[] DQ = "&quot;".toCharArray();
    char[] SQ = "&#39;".toCharArray();
    char[] BQ = "&#96;".toCharArray();
    char[] EQ = "&#61;".toCharArray();
    for (int c = 0; c < ESC.length; c++) {
      if (c <= 13) {
        ESC[c] = ("&#" + c + ";").toCharArray();
      } else {
        switch (c) {
          case '&':
            ESC[c] = AMP;
            break;
          case '<':
            ESC[c] = LT;
            break;
          case '>':
            ESC[c] = GT;
            break;
          case '"':
            ESC[c] = DQ;
            break;
          case '\'':
            ESC[c] = SQ;
            break;
          case '=':
            ESC[c] = EQ;
            break;
          case '`':
            ESC[c] = BQ;
            break;
          default:
            ESC[c] = null;
            break;
        }
      }
    }
  }

  public static void escape(String value, Writer writer) {
    try {
      char[] chars = value.toCharArray();
      int length = chars.length;
      int start = 0;
      for (int i = 0; i < length; i++) {
        char c = chars[i];
        char[] escaped;
        // We only possibly escape chars in the range 0-96
        if (c <= 96 && (escaped = ESC[c]) != null) {
          // Write from the last replacement to before this one
          if (i > start) writer.write(chars, start, i - start);
          // Write the replacement
          writer.write(escaped);
          // Move the pointer to the position after replacement
          start = i + 1;
        }
      }
      writer.write(chars, start, length - start);
    } catch (IOException e) {
      throw new MustacheException("Failed to encode value: " + value, e);
    }
  }

}
