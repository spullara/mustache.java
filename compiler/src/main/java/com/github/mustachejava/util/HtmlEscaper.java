package com.github.mustachejava.util;

import com.github.mustachejava.MustacheException;

import java.io.IOException;
import java.io.Writer;

/**
 * Escapes user data that you wish to include in HTML pages.
 */
public class HtmlEscaper {

  private static char[][] LT_96 = new char[97][];

  static {
    char[] AMP = "&amp;".toCharArray();
    char[] LT = "&lt;".toCharArray();
    char[] GT = "&gt;".toCharArray();
    char[] DQ = "&quot;".toCharArray();
    char[] SQ = "&#39;".toCharArray();
    char[] BQ = "&#96;".toCharArray();
    char[] EQ = "&#61;".toCharArray();
    for (int c = 0; c < LT_96.length; c++) {
      if (c <= 13) {
        LT_96[c] = ("&#" + String.valueOf(c) + ";").toCharArray();
      } else {
        switch (c) {
          case '&':
            LT_96[c] = AMP;
            break;
          case '<':
            LT_96[c] = LT;
            break;
          case '>':
            LT_96[c] = GT;
            break;
          case '"':
            LT_96[c] = DQ;
            break;
          case '\'':
            LT_96[c] = SQ;
            break;
          case '=':
            LT_96[c] = EQ;
            break;
          case '`':
            LT_96[c] = BQ;
            break;
          default:
            LT_96[c] = new char[]{(char) c};
            break;
        }
      }
    }
  }

  public static void escape(String value, Writer writer) {
    try {
      int length = value.length();
      for (int i = 0; i < length; i++) {
        char c = value.charAt(i);
        if (c <= 96) {
          writer.write(LT_96[c]);
        } else {
          writer.write(c);
        }
      }
    } catch (IOException e) {
      throw new MustacheException("Failed to encode value: " + value, e);
    }
  }
}
