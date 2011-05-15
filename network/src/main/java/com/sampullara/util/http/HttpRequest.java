package com.sampullara.util.http;

import org.eclipse.jetty.client.HttpClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Future;

/**
 * Base class for doing an HTTP request
 * <p/>
 * User: sam
 * Date: May 7, 2010
 * Time: 5:12:46 PM
 */
public abstract class HttpRequest<T> {
  protected URL url;
  protected static HttpClient client = new HttpClient();

  static {
    client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
    try {
      client.start();
    } catch (Exception e) {
      throw new AssertionError("Could not start HTTP client");
    }
  }

  public HttpRequest(String url) throws MalformedURLException {
    this.url = new URL(url);
  }

  public abstract Future<T> execute() throws IOException;
}
