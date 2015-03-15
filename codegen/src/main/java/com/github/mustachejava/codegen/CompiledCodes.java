package com.github.mustachejava.codegen;

import java.io.Writer;
import java.util.List;

/**
 * Compiled code.
 */
public interface CompiledCodes {
  Writer runCodes(Writer writer, List<Object> scopes);
}
