package com.github.mustachejava;

/**
 * Generally there is nothing you can do if it fails.
 */
public class MustacheException extends RuntimeException {
  public MustacheException() {
    super();
  }

  public MustacheException(String s) {
    super(s);
  }

  public MustacheException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public MustacheException(Throwable throwable) {
    super(throwable);
  }
}
