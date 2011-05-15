package com.sampullara.mustache;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:13:29 AM
 */
public class MustacheException extends Exception {
  public MustacheException(Exception e) {
    super(e);
  }

  public MustacheException(String msg) {
    super(msg);
  }

  public MustacheException(String s, Exception e) {
    super(s, e);
  }
}
