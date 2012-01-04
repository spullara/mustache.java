package com.sampullara.mustache.code;

import com.sampullara.mustache.Code;
import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.MustacheTrace;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sampullara.mustache.Mustache.truncate;

/**
 * Writes a raw value with or without mustache encoding.
 * <p/>
 * User: sam
 * Date: 11/27/11
 * Time: 10:40 AM
 */
public class WriteValueCode implements Code {
  private final Mustache m;
  private final String name;
  private final boolean encoded;
  private final int line;

  public WriteValueCode(Mustache m, String name, boolean encoded, int line) {
    this.m = m;
    this.name = name;
    this.encoded = encoded;
    this.line = line;
  }

  @Override
  public void execute(FutureWriter fw, Scope scope) throws MustacheException {
    MustacheTrace.Event event = null;
    if (Mustache.trace) {
      Object parent = scope.getParent();
      String traceName = parent == null ? scope.getClass().getName() : parent.getClass().getName();
      event = MustacheTrace.addEvent("get: " + name, traceName);
    }
    Object value = m.getValue(scope, name);
    if (Mustache.trace) {
      event.end();
    }
    if (value != null) {
      if (value instanceof Future) {
        try {
          fw.enqueue((Future) value);
          return;
        } catch (Exception e) {
          throw new MustacheException("Failed to evaluate future value: " + name, e);
        }
      }
      if (value instanceof FutureWriter) {
        final Object finalValue = value;
        try {
          fw.enqueue(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
              return finalValue;
            }
          });
        } catch (IOException e) {
          throw new MustacheException("Failed to enqueue future writer", e);
        }
      } else {
        String string = String.valueOf(value);
        if (encoded) {
          string = m.encode(string);
        }
        try {
          fw.write(string);
        } catch (IOException e) {
          throw new MustacheException("Failed to write: " + e);
        }
      }
    }
  }

  @Override
  public int getLine() {
    return line;
  }

  @Override
  public Scope unexecute(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
    String value = unexecuteValueCode(current, text, position, next);
    if (value != null) {
      if (value.equals("") && next != null && next.length > 0) {
        /*
        This doesn't work either
        Trying to handle {{#include}}{{name}}{{/include}} case and
        {{name}}{{other}} case cleanly when one is empty
        */
        try {
          // Let's see if the next matches iff WriteCode
          if (next[0] instanceof WriteCode) {
            Code[] truncate = truncate(next, 1, null);
            if (next[0].unexecute(current, text, position, truncate) != null) {
              return null;
            }
          }
        } catch (MustacheException me) {
          // Fall through
        }
        // Didn't match next, put in empty string
      }
      BuilderCodeFactory.put(current, name, value);
      return current;
    }
    return null;
  }

  public String unexecuteValueCode(Scope current, String text, AtomicInteger position, Code[] next) throws MustacheException {
    AtomicInteger probePosition = new AtomicInteger(position.get());
    Code[] truncate = truncate(next, 1, null);
    Scope result = null;
    int lastposition = position.get();
    while (next.length > 0 && probePosition.get() < text.length()) {
      lastposition = probePosition.get();
      result = next[0].unexecute(current, text, probePosition, truncate);
      if (result == null) {
        probePosition.incrementAndGet();
      } else {
        break;
      }
    }
    if (result != null) {
      String value = text.substring(position.get(), lastposition);
      if (encoded) {
        value = decode(value);
      }
      position.set(lastposition);
      return value;
    }
    return null;
  }

  @Override
  public void identity(FutureWriter fw) throws MustacheException {
    try {
      if (!encoded) fw.append("{");
      fw.append("{{").append(name).append("}}");
      if (!encoded) fw.append("}");
    } catch (IOException e) {
      throw new MustacheException(e);
    }
  }

  public String decode(String value) {
    return value.replaceAll("&quot;", "\"").replaceAll("&lt;", "<").replaceAll("&gt;", ">")
            .replaceAll("&#10;", "\n").replaceAll("\\\\", "\\").replaceAll("&amp;", "&");
  }
}