package com.github.mustachejava;

/**
 * Generally there is nothing you can do if it fails.
 */
public class MustacheException extends RuntimeException {
  public MustacheException() {
    super();
  }

  public MustacheException(String message) {
    super(message);
  }

  public MustacheException(String message, Throwable throwable) {
    super(message, throwable);
  }

  public MustacheException(Throwable throwable) {
    super(throwable);
  }
}
