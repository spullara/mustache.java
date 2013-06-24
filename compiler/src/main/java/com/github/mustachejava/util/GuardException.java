package com.github.mustachejava.util;

/**
 * If the wrapper has a different calling signature, tell the
 * caller to refind it.
 */
public class GuardException extends RuntimeException {
  public GuardException() {
  }

  public GuardException(String message) {
    super(message);
  }
}
