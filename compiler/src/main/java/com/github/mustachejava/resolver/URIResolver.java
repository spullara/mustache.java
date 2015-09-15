package com.github.mustachejava.resolver;

import com.github.mustachejava.MustacheResolver;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;

/**
 * MustacheResolver implementation that resolves
 * mustache resources using URI objects.
 */
public class URIResolver implements MustacheResolver {

  @Override
  public Reader getReader(final String resourceName) {
    try {
      final URI uri = new URI(resourceName);
      final URL url = uri.toURL();
      final InputStream in = url.openStream();
      return new InputStreamReader(in, "UTF-8");
    } catch (final Exception ex) {
      return null;
    }
  }

}
