package com.github.mustachejava.impl;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.github.mustachejava.Code;
import com.github.mustachejava.MustacheException;

/**
 * Simplest possible code implementaion with some default shared behavior
 */
public class DefaultCode implements Code {
  private StringBuilder sb = new StringBuilder();
  private String appended;
  private Code[] codes;

  public DefaultCode() {}
  
  public DefaultCode(Code[] codes) {
    this.codes = codes;
  }

  /**
   * Default append behavior
   * @param writer
   * @param scopes
   */
  @Override
  public void execute(Writer writer, List<Object> scopes) {
    if (codes != null) {
      int length = codes.length;
      for (int i = 0; i < length; i++) {
        codes[i].execute(writer, scopes);
      }
    }
    if (appended != null) {
      try {
        writer.write(appended);
      } catch (IOException e) {
        throw new MustacheException(e);
      }
    }
  }

  @Override
  public void append(String text) {
    sb.append(text);
    appended = sb.toString();
  }
}
