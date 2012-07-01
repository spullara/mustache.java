package com.github.mustachejava.util;

import com.github.mustachejava.MustacheException;

import java.io.IOException;
import java.io.Writer;

/**
 * Escapes user data that you wish to include in HTML pages.
 */
public class HtmlEscaper {
  public static void escape(String value, Writer writer) {
    try {
      int position = 0;
      int length = value.length();
      for (int i = 0; i < length; i++) {
        char c = value.charAt(i);
        switch (c) {
          case '&':
            // If we match an entity or char ref then keep it
            // as is in the text. Otherwise, replace it.
            if (matchesEntityRef(i + 1, length, value)) {
              // If we are at the beginning we can just keep going
              if (position != 0) {
                position = append(value, writer, position, i, "&");
              }
            } else {
              position = append(value, writer, position, i, "&amp;");
            }
            break;
          case '<':
            position = append(value, writer, position, i, "&lt;");
            break;
          case '>':
            position = append(value, writer, position, i, "&gt;");
            break;
          case '"':
            position = append(value, writer, position, i, "&quot;");
            break;
          case '\'':
            position = append(value, writer, position, i, "&#39;");
            break;
          case '/':
            position = append(value, writer, position, i, "&#x2F;");
            break;
          case '\n':
            position = append(value, writer, position, i, "&#10;");
            break;
        }
      }
      writer.append(value, position, length);
    } catch (IOException e) {
      throw new MustacheException("Failed to encode value: " + value);
    }
  }

  private static int append(String value, Writer writer, int position, int i, String replace) throws IOException {
    // Append the clean text
    writer.append(value, position, i);
    // Append the encoded value
    writer.append(replace);
    // and advance the position past it
    return i + 1;
  }

  // Matches all HTML named and character entity references
  private static boolean matchesEntityRef(int position, int length, String value) {
    for (int i = position; i < length; i++) {
      char c = value.charAt(i);
      switch (c) {
        case ';':
          // End of the entity
          return i != position;
        case '#':
          // Switch to char reference
          return i == position && matchesCharRef(i + 1, length, value);
        default:
          // Letters can be at the start
          if (c >= 'a' && c <= 'z') continue;
          if (c >= 'A' && c <= 'Z') continue;
          if (i != position) {
            // Can only appear in the middle
            if (c >= '0' && c <= '9') continue;
          }
          return false;
      }
    }
    // Didn't find ending ;
    return false;
  }

  private static boolean matchesCharRef(int position, int length, String value) {
    for (int i = position; i < length; i++) {
      char c = value.charAt(i);
      if (c == ';') {
        return i != position;
      } else if ((c >= '0' && c <= '9') || c == 'x' || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
      } else return false;
    }
    return false;
  }
}
