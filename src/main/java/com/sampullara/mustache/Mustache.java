package com.sampullara.mustache;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for Mustaches.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:12:47 AM
 */
public abstract class Mustache {
  protected Logger logger = Logger.getLogger(getClass().getName());

  public abstract void execute(Writer writer, Scope ctx) throws MustacheException;

  protected void write(Writer writer, Scope s, String name, boolean encode) throws MustacheException {
    Object value = getValue(s, name);
    if (value != null) {
      String string = String.valueOf(value);
      if (encode) {
        string = encode(string);
      }
      try {
        writer.write(string);
      } catch (IOException e) {
        throw new MustacheException("Failed to write: " + e);
      }
    }
  }

  private static Iterable emptyIterable = new ArrayList(0);
  private static Iterable singleIterable = Arrays.asList(true);

  protected Iterable iterable(Scope s, String name) {
    Object value = s.get(name);
    if (value != null) {
      if (value instanceof Boolean) {
        if (((Boolean)value)) {
          return singleIterable;
        }
      } else if (value instanceof Iterable) {
        return (Iterable) value;
      } else if (value.getClass().isArray()) {
        return Arrays.asList((Object[])value);
      } else {
        logger.warning(name + " is not a boolean or iterable");
      }
    }
    return emptyIterable;
  }

  private Object getValue(Scope s, String name) {
    try {
      return s.get(name);
    } catch (Exception e) {
      logger.warning("Failed: " + e + " using " + name);
    }
    return null;
  }

  private static Pattern findToEncode = Pattern.compile("&(?!\\w+;)|[\"<>\\\\]");

  protected static String encode(String value) {
    StringBuffer sb = new StringBuffer();
    Matcher matcher = findToEncode.matcher(value);
    while (matcher.find()) {
      char c = matcher.group().charAt(0);
      switch (c) {
        case '&':
          matcher.appendReplacement(sb, "&amp;");
        case '\\':
          matcher.appendReplacement(sb, "\\\\");
        case '"':
          matcher.appendReplacement(sb, "\"");
        case '<':
          matcher.appendReplacement(sb, "&lt;");
        case '>':
          matcher.appendReplacement(sb, "&gt;");
      }
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}
