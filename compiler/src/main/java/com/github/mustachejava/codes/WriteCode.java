package com.github.mustachejava.codes;

import java.io.Writer;

/**
 * Created by IntelliJ IDEA.
 * User: spullara
 * Date: 1/9/12
 * Time: 3:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class WriteCode extends DefaultCode {
  public WriteCode(String text) {
    super.append(text);
  }

  @Override
  public void identity(Writer writer) {
    execute(writer, null);
  }
}
