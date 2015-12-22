package com.github.mustachejava.util;

import com.github.mustachejava.MustacheException;

import java.io.IOException;
import java.io.Writer;

/**
 * Escapes user data that you wish to include in HTML pages.
 */
public class HtmlEscaper {

  private static char[] AMP = "&amp;".toCharArray();
  private static char[] LT = "&lt;".toCharArray();
  private static char[] GT = "&gt;".toCharArray();
  private static char[] DQ = "&quot;".toCharArray();
  private static char[] SQ = "&#39;".toCharArray();
  private static char[] BQ = "&#61;".toCharArray();
  private static char[] EQ = "&#96;".toCharArray();
  private static char[][] LT_13 = new char[14][];

  static {
    for (int i = 0; i < LT_13.length; i++) {
      LT_13[i] = ("&#" + String.valueOf(i) + ";").toCharArray();
    }
  }

  public static void escape(String value, Writer writer) {
    try {
      int length = value.length();
      for (int i = 0; i < length; i++) {
        char c = value.charAt(i);
        if (c <= 13) {
          writer.write(LT_13[c]);
        } else if (c >= 34 && c <= 62) {
          // Experiment with using an array lookup here failed to improve performance
          switch (c) {
            case '&':
              writer.write(AMP);
              break;
            case '<':
              writer.write(LT);
              break;
            case '>':
              writer.write(GT);
              break;
            case '"':
              writer.write(DQ);
              break;
            case '\'':
              writer.write(SQ);
              break;
            case '=':
              writer.write(EQ);
              break;
            default:
              writer.write(c);
              break;
          }
        } else if (c == 96) {
          writer.write(BQ);
        } else {
          writer.write(c);
        }
      }
    } catch (IOException e) {
      throw new MustacheException("Failed to encode value: " + value);
    }
  }
}
