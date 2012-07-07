package com.github.mustachejava;

public interface PragmaHandler {
  Code handle(String pragma, String args);
}
