package com.sampullara.mustache.http;

import com.sampullara.mustache.CallbackFuture;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Future;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: May 7, 2010
 * Time: 5:04:42 PM
 */
public class XMLHttpRequest extends HttpRequest<Document> {
  private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
  private static DocumentBuilder db;

  static {
    try {
      db = dbf.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new AssertionError("Failed to create document builder: " + e);
    }
  }

  public XMLHttpRequest(String url) throws MalformedURLException {
    super(url);
  }

  @Override
  public Future<Document> execute() throws IOException {
    final CallbackFuture<Document> future = new CallbackFuture<Document>();
    ContentExchange exchange = new ContentExchange() {
      protected void onResponseComplete() throws IOException {
        super.onResponseComplete();
        String responseContent = this.getResponseContent();
        try {
          future.set(db.parse(new InputSource(responseContent)));
        } catch (SAXException e) {
          future.error(e);
        }
      }
    };

    exchange.setMethod("GET");
    exchange.setURL(url.toExternalForm());

    // start the exchange
    client.send(exchange);

    return future;
  }
}
