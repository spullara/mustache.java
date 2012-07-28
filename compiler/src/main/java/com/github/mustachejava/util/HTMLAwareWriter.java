package com.github.mustachejava.util;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

import static com.github.mustachejava.util.HTMLAwareWriter.Context.*;

/**
 * Manages a state machine that knows the context in an HTML document it is writing.
 */
public class HTMLAwareWriter extends Writer {

  private static Logger l = Logger.getLogger("HTMLAwareWriter");

  // Delegate
  private Writer writer;

  // Current state of the document
  private Context state;
  private Context quoteState;
  private Context bodyState;

  // Buffer for tracking things
  private StringBuilder buffer = new StringBuilder();

  public Context getState() {
    return state;
  }

  public enum Context {
    PRAGMA,
    COMMENT,
    TAG,
    TAG_NAME,
    END_TAG,
    END_TAG_NAME,
    AFTER_END_TAG_NAME,
    ATTRIBUTES,
    ATTR_EQUAL,
    ATTR_NAME,
    NQ_VALUE,
    ESCAPE,
    SQ_VALUE,
    DQ_VALUE,
    BODY,
    SCRIPT,
    SCRIPT_SQ_VALUE,
    SCRIPT_DQ_VALUE,
    SCRIPT_CHECK,
  }

  public HTMLAwareWriter(Writer writer) {
    this(writer, BODY);
  }

  public HTMLAwareWriter(Writer writer, Context state) {
    this.state = state;
    this.writer = writer;
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    int end = off + len;
    for (int i = off; i < end; i++) {
      char c = cbuf[i];
      nextState(c);
      if (state == TAG_NAME || state == PRAGMA || state == COMMENT) {
        buffer.append(c);
      }
    }
    writer.write(cbuf, off, len);
  }

  public void s(Context c) {
    state = c;
    if (buffer.length() > 0) {
      buffer = new StringBuilder();
    }
  }

  private void nextState(char c) {
    if (state == Context.ATTRIBUTES) {
      attr(c);
    } else if (state == Context.BODY) {
      body(c);
    } else if (state == Context.TAG) {
      tag(c);
    } else if (state == Context.ATTR_NAME) {
      attrName(c);
    } else if (state == Context.ATTR_EQUAL) {
      attrEqual(c);
    } else if (state == Context.DQ_VALUE) {
      dqValue(c);
    } else if (state == Context.SCRIPT_DQ_VALUE) {
      scriptDqValue(c);
    } else if (state == Context.TAG_NAME) {
      tagName(c);
    } else if (state == Context.END_TAG) {
      endTag(c);
    } else if (state == Context.END_TAG_NAME) {
      endTagName(c);
    } else if (state == Context.ESCAPE) {
      escape();
    } else if (state == Context.SCRIPT) {
      script(c);
    } else if (state == Context.SCRIPT_SQ_VALUE) {
      scriptSqValue(c);
    } else if (state == Context.SCRIPT_CHECK) {
      scriptCheck(c);
    } else if (state == Context.AFTER_END_TAG_NAME) {
      afterEndTagName(c);
    } else if (state == Context.SQ_VALUE) {
      sqValue(c);
    } else if (state == Context.NQ_VALUE) {
      nqValue(c);
    } else if (state == Context.PRAGMA) {
      pragma(c);
    } else if (state == Context.COMMENT) {
      comment(c);
    }
  }

  private void scriptCheck(char c) {
    if (c == '/') {
      bodyState = BODY;
      s(END_TAG);
    } else if (ws(c)) {
    } else {
      s(SCRIPT);
    }
  }

  private void pragma(char c) {
    if (c == '-') {
      if (buffer.toString().equals("!-")) {
        s(COMMENT);
      }
    } else if (c == '>') {
      s(bodyState);
    }
  }

  private void scriptSqValue(char c) {
    if (c == '\\') {
      quoteState = state;
      s(ESCAPE);
    } else if (c == '\'') {
      s(SCRIPT);
    }
  }

  private void scriptDqValue(char c) {
    if (c == '\\') {
      quoteState = state;
      s(ESCAPE);
    } else if (c == '"') {
      s(SCRIPT);
    }
  }

  private void script(char c) {
    if (c == '"') {
      s(SCRIPT_DQ_VALUE);
    } else if (c == '\'') {
      s(SCRIPT_SQ_VALUE);
    } else if (c == '<') {
      s(SCRIPT_CHECK);
    }
  }

  private void afterEndTagName(char c) {
    if (ws(c)) {
    } else if (c == '>') {
      s(bodyState);
    } else error(c);
  }

  private void endTagName(char c) {
    if (namePart(c)) {
    } else if (c == '>') {
      s(bodyState);
    } else if (ws(c)) {
      s(AFTER_END_TAG_NAME);
    } else error(c);
  }

  private void endTag(char c) {
    if (nameStart(c)) {
      s(END_TAG_NAME);
    } else if (Character.isWhitespace(c)) {
    } else if (c == '>') {
      s(bodyState);
    } else error(c);
  }

  private boolean nameStart(char c) {
    return Character.isJavaIdentifierStart(c);
  }

  private void comment(char c) {
    if (c == '>') {
      int length = buffer.length();
      if (buffer.substring(length - 2, length).equals("--")) {
        s(bodyState);
      }
    }
  }

  private void nqValue(char c) {
    if (ws(c)) {
      s(ATTRIBUTES);
    } else if (c == '<') {
      error(c);
    } else if (c == '>') {
      s(bodyState);
    }
  }

  private void escape() {
    s(quoteState);
  }

  private void sqValue(char c) {
    if (c == '\\') {
      s(ESCAPE);
      quoteState = SQ_VALUE;
    } else if (c == '\'') {
      s(ATTRIBUTES);
    } else if (c == '<' || c == '>') {
      error(c);
    }
  }

  private void dqValue(char c) {
    if (c == '\\') {
      s(ESCAPE);
      quoteState = DQ_VALUE;
    } else if (c == '\"') {
      s(ATTRIBUTES);
    } else if (c == '<' || c == '>') {
      error(c);
    }
  }

  private void attrEqual(char c) {
    if (c == '"') {
      s(DQ_VALUE);
    } else if (c == '\'') {
      s(SQ_VALUE);
    } else if (ws(c)) {
    } else {
      s(NQ_VALUE);
    }
  }

  private boolean ws(char c) {
    return Character.isWhitespace(c);
  }

  private void attrName(char c) {
    if (namePart(c)) {
    } else if (c == '=') {
      s(ATTR_EQUAL);
    } else if (ws(c)) {
    } else if (c == '>') {
      s(bodyState);
    } else error(c);
  }

  private void attr(char c) {
    if (nameStart(c)) {
      s(ATTR_NAME);
    } else if (c == '>') {
      s(bodyState);
    } else if (c == '/') {
      s(AFTER_END_TAG_NAME);
    } else if (ws(c)) {
    } else error(c);
  }

  private void tagName(char c) {
    if (namePart(c)) {
    } else if (ws(c)) {
      setBodyTag(buffer.toString());
      s(ATTRIBUTES);
    } else if (c == '>') {
      setBodyTag(buffer.toString());
      s(bodyState);
    }
  }

  private boolean namePart(char c) {
    return Character.isJavaIdentifierPart(c) || c == '-';
  }

  private void setBodyTag(String tag) {
    if (tag.equalsIgnoreCase("script")) {
      bodyState = SCRIPT;
    } else bodyState = BODY;
  }

  private void tag(char c) {
    if (nameStart(c)) {
      s(TAG_NAME);
    } else if (c == '/') {
      s(END_TAG);
    } else if (c == '!') {
      s(PRAGMA);
    } else if (ws(c)) {
    } else error(c);
  }

  private void error(char c) {
    buffer.append(c);
    l.warning("Invalid: " + buffer + " (" + state + ")");
    s(bodyState);
  }

  private void body(char c) {
    if (c == '<') {
      bodyState = state;
      s(TAG);
    }
  }

  @Override
  public void flush() throws IOException {
    writer.flush();
  }

  @Override
  public void close() throws IOException {
    flush();
    writer.close();
  }
}

