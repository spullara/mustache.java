package com.github.mustachejava;

import com.google.common.base.Optional;

public interface PragmaHandler {
  Code handle(String pragma, String args);
}
