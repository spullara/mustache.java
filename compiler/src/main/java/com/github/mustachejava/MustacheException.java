package com.github.mustachejava;

/**
 * Generally there is nothing you can do if it fails.
 */
public class MustacheException extends RuntimeException {
  private TemplateContext context;
  
  public MustacheException() {
    super();
  }

  public MustacheException(String message) {
    super(message);
  }

  public MustacheException(String message, Throwable throwable) {
    super(message, throwable);
  }

  public MustacheException(String message, Throwable throwable, TemplateContext context) {
    super(message, throwable);
    this.context = context;
  }

  public MustacheException(Throwable throwable) {
    super(throwable);
  }

  public MustacheException(String message, TemplateContext context) {
    super(message);
    this.context = context;
  }

  public MustacheException(Exception e, TemplateContext context) {
    super(e);
    this.context = context;
  }

  @Override
  public String getMessage() {
    return context == null ? super.getMessage() : super.getMessage() + " @" + context;
  }
  
  public void setContext(TemplateContext context) {
    if (this.context == null)
      this.context = context;
  }

  public TemplateContext getContext() {
    return context;
  }
}
