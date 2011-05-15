package com.sampullara.util.http;

import com.google.common.util.concurrent.SettableFuture;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.eclipse.jetty.client.ContentExchange;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.Future;

/**
 * Retrieve JSON data from a data source without blocking a thread.
 * <p/>
 * User: sam
 * Date: May 7, 2010
 * Time: 5:14:45 PM
 */
public class JSONHttpRequest extends HttpRequest<JsonNode> {
  private static MappingJsonFactory jf = new MappingJsonFactory();

  public JSONHttpRequest(String url) throws MalformedURLException {
    super(url);
  }

  @Override
  public Future<JsonNode> execute() throws IOException {
    final SettableFuture<JsonNode> future = SettableFuture.create();
    ContentExchange exchange = new ContentExchange() {
      protected void onResponseComplete() throws IOException {
        try {
          super.onResponseComplete();
          String responseContent = this.getResponseContent();
          if (responseContent == null) {
            future.set(null);
          } else {
            try {
              JsonParser jp = jf.createJsonParser(responseContent);
              future.set(jp.readValueAsTree());
            } catch (Exception e) {
              future.setException(new IOException("Could not access: " + getURI() + ": " + responseContent.substring(0, Math.min(responseContent.length(), 1000)), e));
            }
          }
        } catch (Throwable th) {
          th.printStackTrace();
          if (!future.isDone()) {
            future.setException(th);
          }
          if (th instanceof IOException) {
            throw (IOException) th;
          }
          if (th instanceof RuntimeException) {
            throw (RuntimeException) th;
          }
          if (th instanceof Error) {
            throw (Error) th;
          }
        }
      }
    };

    exchange.setRequestHeader("Accept", "application/json, text/javascript");
    exchange.setMethod("GET");
    exchange.setURL(url.toString());

    // start the exchange
    client.send(exchange);

    return future;
  }
}
