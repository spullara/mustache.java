package com.github.mustachejava.codes;

import java.io.Writer;

/**
 * Compiled code.
 */
public interface CompiledCodes {
  Writer runCodes(Writer writer, Object[] scopes);
}
