package com.github.mustachejava;

/**
 * Generally there is nothing you can do if it fails.
 */
public class MustacheException extends RuntimeException {
  private TemplateContext context;
  
  public MustacheException() {
    super();
  }

  public MustacheException(String s) {
    super(s);
  }

  public MustacheException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public MustacheException(String s, Throwable throwable, TemplateContext context) {
    super(s, throwable);
    this.context = context;
  }

  public MustacheException(Throwable throwable) {
    super(throwable);
  }
  
  @Override
  public String getMessage() {
    return context == null ? super.getMessage() : super.getMessage() + " @" + context;
  }
  
  public void setContext(TemplateContext context) {
    if (this.context == null)
      this.context = context;
  }
  
}
