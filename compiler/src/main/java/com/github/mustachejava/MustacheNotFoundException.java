package com.github.mustachejava;

/**
 * Mustache exception that provides the name of the missing mustache.
 */
public class MustacheNotFoundException extends MustacheException {

  private final String name;

  public MustacheNotFoundException(String name) {
    super("Template " + name + " not found");
    this.name = name;
  }

  public MustacheNotFoundException(String name, Throwable throwable) {
    super("Template " + name + " not found", throwable);
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
