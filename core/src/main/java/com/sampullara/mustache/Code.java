package com.sampullara.mustache;

import com.sampullara.util.FutureWriter;

/**
 * Codes for mustache.
 * <p/>
 * User: sam
 * Date: 5/15/11
 * Time: 1:26 PM
 */
public interface Code {
  void execute(FutureWriter fw, Scope scope) throws MustacheException;
}

