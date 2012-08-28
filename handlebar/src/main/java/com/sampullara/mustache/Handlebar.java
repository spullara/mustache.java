package com.sampullara.mustache;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Run a local server and merge .js and .html files using mustache.
 * <p/>
 * User: sam
 * Date: Jun 15, 2010
 * Time: 4:25:31 PM
 */
public class Handlebar {

  @Argument(alias = "p")
  private static Integer port = 8000;

  @Argument(alias = "m", required = true)
  private static String mocks;

  private static Map<String, String> mimeTypes = new HashMap<String, String>();
  static {
    mimeTypes.put("html", "text/html");
    mimeTypes.put("png" , "image/png");
    mimeTypes.put("gif" , "image/gif");
    mimeTypes.put("jpg" , "image/jpg");
    mimeTypes.put("js"  , "text/javascript");
    mimeTypes.put("css" , "text/css");
  }

  public static Object toObject(final JsonNode node) {
    if (node.isArray()) {
      return new ArrayList() {{
        for (JsonNode jsonNodes : node) {
          add(toObject(jsonNodes));
        }
      }};
    } else if (node.isObject()) {
      return new HashMap() {{
        for (Iterator<Map.Entry<String, JsonNode>> i = node.getFields(); i.hasNext(); ) {
          Map.Entry<String, JsonNode> next = i.next();
          Object o = toObject(next.getValue());
          put(next.getKey(), o);
        }
      }};
    } else if (node.isBoolean()) {
      return node.getBooleanValue();
    } else if (node.isNull()) {
      return null;
    } else {
      return node.asText();
    }
  }


  public static void main(String[] args) throws Exception {
    try {
      Args.parse(Handlebar.class, args);
    } catch (IllegalArgumentException e) {
      Args.usage(Handlebar.class);
      System.exit(1);
    }
    final JsonFactory jf = new MappingJsonFactory();
    Handler handler = new AbstractHandler() {
      public void handle(String s, Request r, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String pathInfo = req.getPathInfo();

        if (pathInfo.endsWith("/")) pathInfo += "index.html";
        
        String extension = pathInfo.substring(pathInfo.lastIndexOf(".") + 1);
        String base = pathInfo.substring(0, pathInfo.lastIndexOf("."));
        String mimeType = mimeTypes.get(extension);
        res.setContentType(mimeType == null ? "text/html" : mimeType);
        res.setCharacterEncoding("utf-8");
        if (mimeType == null || mimeType.equals("text/html")) {
          // Handle like a template
          String filename = pathInfo.substring(1);
          try {
            MustacheFactory mc = new DefaultMustacheFactory(new File("."));
            Mustache mustache = mc.compile(filename);
            File file = new File(mocks, base + ".json");
            res.setStatus(HttpServletResponse.SC_OK);
            Map parameters = new HashMap<Object, Object>(req.getParameterMap()) {
              @Override
              public Object get(Object o) {
                Object result = super.get(o);    //To change body of overridden methods use File | Settings | File Templates.
                if (result instanceof String[]) {
                  String[] strings = (String[]) result;
                  if (strings.length == 1) {
                    return strings[0];
                  }
                }
                return result;
              }
            };

            if (file.exists()) {
              BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
              JsonParser parser = jf.createJsonParser(br);
              JsonNode json = parser.readValueAsTree();
              br.close();
              mustache.execute(res.getWriter(), new Object[] { toObject(json), parameters });
            } else {
              mustache.execute(res.getWriter(), parameters);
            }
            r.setHandled(true);
          } catch (MustacheException e) {
            e.printStackTrace(res.getWriter());
            r.setHandled(true);
            res.setStatus(500);
          }
        } else {
          // Handle like a file
          res.setStatus(HttpServletResponse.SC_OK);
          OutputStream os = res.getOutputStream();
          byte[] bytes = new byte[8192];
          BufferedInputStream bis = new BufferedInputStream(new FileInputStream(pathInfo.substring(1)));
          int read;
          while((read = bis.read(bytes)) != -1) {
            os.write(bytes, 0, read);
          }
          os.close();
        }
      }
    };

    Server server = new Server(port);
    server.setHandler(handler);
    server.start();
  }
}
