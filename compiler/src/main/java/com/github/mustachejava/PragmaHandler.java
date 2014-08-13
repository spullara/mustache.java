package com.github.mustachejava;

public interface PragmaHandler {
  Code handle(TemplateContext tc, String pragma, String args);
}
