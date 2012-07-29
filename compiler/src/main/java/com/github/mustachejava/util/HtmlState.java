package com.github.mustachejava.util;

import java.util.logging.Logger;

import static com.github.mustachejava.util.HtmlState.HTML.*;

/**
 * Stores the HTML state while parsing.
 */
public class HtmlState implements State<HtmlState.HTML> {

  private static Logger l = Logger.getLogger("HTMLAwareWriter");

  // Current state of the document
  private HTML state;
  private HTML quoteState;
  private HTML bodyState;

  // Ringbuffer for comments and script tags
  private final RingBuffer ringBuffer = new RingBuffer(6);

  // Ask the writer for the current state
  public HTML getState() {
    return state;
  }

  public HtmlState() {
    this(BODY);
  }

  public HtmlState(HTML state) {
    this.state = state;
  }

  public enum HTML {
    ATTRIBUTES,
    BODY,
    TAG,
    ATTR_NAME,
    ATTR_EQUAL,
    DQ_VALUE,
    SCRIPT_DQ_VALUE,
    TAG_NAME,
    END_TAG,
    END_TAG_NAME,
    ESCAPE,
    SCRIPT,
    SCRIPT_SQ_VALUE,
    SCRIPT_CHECK,
    AFTER_END_TAG_NAME,
    SQ_VALUE,
    NQ_VALUE,
    PRAGMA,
    COMMENT,
  }

  public void nextState(char[] cbuf, int off, int len) {
    int end = off + len;
    for (int i = off; i < end; i++) {
      char c = cbuf[i];
      switch (state) {
        case ATTRIBUTES:
          attr(c);
          break;
        case BODY:
          body(c);
          break;
        case TAG:
          tag(c);
          break;
        case ATTR_NAME:
          attrName(c);
          break;
        case ATTR_EQUAL:
          attrEqual(c);
          break;
        case DQ_VALUE:
          dqValue(c);
          break;
        case SCRIPT_DQ_VALUE:
          scriptDqValue(c);
          break;
        case TAG_NAME:
          tagName(c);
          break;
        case END_TAG:
          endTag(c);
          break;
        case END_TAG_NAME:
          endTagName(c);
          break;
        case ESCAPE:
          escape();
          break;
        case SCRIPT:
          script(c);
          break;
        case SCRIPT_SQ_VALUE:
          scriptSqValue(c);
          break;
        case SCRIPT_CHECK:
          scriptCheck(c);
          break;
        case AFTER_END_TAG_NAME:
          afterEndTagName(c);
          break;
        case SQ_VALUE:
          sqValue(c);
          break;
        case NQ_VALUE:
          nqValue(c);
          break;
        case PRAGMA:
          pragma(c);
          break;
        case COMMENT:
          comment(c);
          break;
      }
      if (state == TAG_NAME || state == PRAGMA || state == COMMENT) {
        ringBuffer.append(c);
      }
    }
  }

  private void scriptCheck(char c) {
    if (c == '/') {
      bodyState = BODY;
      state = END_TAG;
    } else if (ws(c)) {
    } else {
      state = SCRIPT;
    }
  }

  private void pragma(char c) {
    if (c == '-') {
      if (ringBuffer.compare("!-", false)) {
        state = COMMENT;
        ringBuffer.clear();
      }
    } else if (c == '>') {
      state = bodyState;
    }
  }

  private void scriptSqValue(char c) {
    if (c == '\\') {
      quoteState = state;
      state = ESCAPE;
    } else if (c == '\'') {
      state = SCRIPT;
    }
  }

  private void scriptDqValue(char c) {
    if (c == '\\') {
      quoteState = state;
      state = ESCAPE;
    } else if (c == '"') {
      state = SCRIPT;
    }
  }

  private void script(char c) {
    if (c == '"') {
      state = SCRIPT_DQ_VALUE;
    } else if (c == '\'') {
      state = SCRIPT_SQ_VALUE;
    } else if (c == '<') {
      state = SCRIPT_CHECK;
    }
  }

  private void afterEndTagName(char c) {
    if (ws(c)) {
    } else if (c == '>') {
      state = bodyState;
    } else error(c);
  }

  private void endTagName(char c) {
    if (namePart(c)) {
    } else if (c == '>') {
      state = bodyState;
    } else if (ws(c)) {
      state = AFTER_END_TAG_NAME;
    } else error(c);
  }

  private void endTag(char c) {
    if (nameStart(c)) {
      state = END_TAG_NAME;
    } else if (Character.isWhitespace(c)) {
    } else if (c == '>') {
      state = bodyState;
    } else error(c);
  }

  private boolean nameStart(char c) {
    return Character.isJavaIdentifierStart(c);
  }

  private void comment(char c) {
    if (c == '>') {
      if (ringBuffer.compare("--", false)) {
        state = bodyState;
      }
    }
  }

  private void nqValue(char c) {
    if (ws(c)) {
      state = ATTRIBUTES;
    } else if (c == '<') {
      error(c);
    } else if (c == '>') {
      state = bodyState;
    }
  }

  private void escape() {
    state = quoteState;
  }

  private void sqValue(char c) {
    if (c == '\\') {
      state = ESCAPE;
      quoteState = SQ_VALUE;
    } else if (c == '\'') {
      state = ATTRIBUTES;
    } else if (c == '<' || c == '>') {
      error(c);
    }
  }

  private void dqValue(char c) {
    if (c == '\\') {
      state = ESCAPE;
      quoteState = DQ_VALUE;
    } else if (c == '\"') {
      state = ATTRIBUTES;
    } else if (c == '<' || c == '>') {
      error(c);
    }
  }

  private void attrEqual(char c) {
    if (c == '"') {
      state = DQ_VALUE;
    } else if (c == '\'') {
      state = SQ_VALUE;
    } else if (ws(c)) {
    } else {
      state = NQ_VALUE;
    }
  }

  private boolean ws(char c) {
    return Character.isWhitespace(c);
  }

  private void attrName(char c) {
    if (namePart(c)) {
    } else if (c == '=') {
      state = ATTR_EQUAL;
    } else if (ws(c)) {
    } else if (c == '>') {
      state = bodyState;
    } else error(c);
  }

  private void attr(char c) {
    if (nameStart(c)) {
      state = ATTR_NAME;
    } else if (c == '>') {
      state = bodyState;
    } else if (c == '/') {
      state = AFTER_END_TAG_NAME;
    } else if (ws(c)) {
    } else error(c);
  }

  private void tagName(char c) {
    if (namePart(c)) {
    } else if (ws(c)) {
      setBodyTag();
      state = ATTRIBUTES;
    } else if (c == '>') {
      setBodyTag();
      state = bodyState;
    }
  }

  private boolean namePart(char c) {
    return Character.isJavaIdentifierPart(c) || c == '-';
  }

  private void setBodyTag() {
    if (ringBuffer.compare("script", true)) {
      bodyState = SCRIPT;
    } else bodyState = BODY;
  }

  private void tag(char c) {
    if (nameStart(c)) {
      state = TAG_NAME;
      ringBuffer.clear();
    } else if (c == '/') {
      state = END_TAG;
    } else if (c == '!') {
      state = PRAGMA;
      ringBuffer.clear();
    } else if (ws(c)) {
    } else error(c);
  }

  private void error(char c) {
    l.warning("Invalid: " + new StringBuilder().append(c) + " (" + state + ")");
    state = bodyState;
  }

  private void body(char c) {
    if (c == '<') {
      bodyState = state;
      state = TAG;
    }
  }

}
