package com.sampullara.mustache.code;

import com.sampullara.mustache.Code;

/**
 * Created by IntelliJ IDEA.
 * User: spullara
 * Date: 9/16/11
 * Time: 11:56 AM
 * To change this template use File | Settings | File Templates.
 */
public interface WriteCode extends Code {
  void append(String s);
}
