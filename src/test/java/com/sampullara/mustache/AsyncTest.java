package com.sampullara.mustache;

import junit.framework.TestCase;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Test the Jetty asynchronous http client.
 * <p/>
 * User: sam
 * Date: May 7, 2010
 * Time: 1:34:21 PM
 */
public class AsyncTest extends TestCase {
  public void testAsyncHTTP() throws ServletException, IOException, ExecutionException, InterruptedException {
    HttpClient client = new HttpClient();
    client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
    try {
      client.start();
    }
    catch (Exception e) {
      throw new ServletException(e);
    }

    List<CallbackFuture<String>> futures = new ArrayList<CallbackFuture<String>>();
    for (int i = 0; i < 100; i++) {
      final CallbackFuture<String> future = new CallbackFuture<String>();

      // create the exchange object, which lets you define where you want to go
      // and what you want to do once you get a response
      ContentExchange exchange = new ContentExchange() {
        // define the callback method to process the response when you get it back
        protected void onResponseComplete() throws IOException {
          super.onResponseComplete();
          String responseContent = this.getResponseContent();
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
          }
          future.set(responseContent);
        }
      };

      exchange.setMethod("GET");
      exchange.setURL("http://www.javarants.com/test.json");

      // start the exchange
      client.send(exchange);
      futures.add(future);
    }
    for (CallbackFuture<String> future : futures) {
      System.out.println(future.get());
    }
  }
}
