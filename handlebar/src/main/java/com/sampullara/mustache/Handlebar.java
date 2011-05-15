package com.sampullara.mustache;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import com.sampullara.util.FutureWriter;
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
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
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

  public static void main(String[] args) throws Exception {
    try {
      Args.parse(Handlebar.class, args);
    } catch (IllegalArgumentException e) {
      Args.usage(Handlebar.class);
      System.exit(1);
    }
    final MustacheBuilder mc = new MustacheBuilder(new File("."));
    final JsonFactory jf = new MappingJsonFactory();
    Handler handler = new AbstractHandler() {
      public void handle(String s, Request r, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String pathInfo = req.getPathInfo();
        String extension = pathInfo.substring(pathInfo.lastIndexOf(".") + 1);
        String base = pathInfo.substring(0, pathInfo.lastIndexOf("."));
        String mimeType = mimeTypes.get(extension);
        res.setContentType(mimeType == null ? "text/html" : mimeType);
        res.setCharacterEncoding("utf-8");
        if (mimeType == null || mimeType.equals("text/html")) {
          // Handle like a template
          String filename = pathInfo.endsWith("/") ? pathInfo + "index.html" : pathInfo.substring(1);
          try {
            Mustache mustache = mc.parseFile(filename);
            FutureWriter fw = new FutureWriter(res.getWriter());
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
              mustache.execute(fw, new Scope(json, new Scope(parameters)));
            } else {
              mustache.execute(fw, new Scope(parameters));
            }
            fw.flush();
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
