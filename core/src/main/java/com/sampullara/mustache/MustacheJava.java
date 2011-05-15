package com.sampullara.mustache;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: 5/15/11
 * Time: 1:30 PM
 */
public interface MustacheJava {
  Mustache parse(String partial) throws MustacheException;

  Mustache parseFile(String path) throws MustacheException;
}
