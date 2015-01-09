package com.sampullara.mustache;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.TemplateFunction;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Run a local server and merge .js and .html files using mustache.
 *
 * User: sam Date: Jun 15, 2010 Time: 4:25:31 PM
 */
public class Handlebar {

  @Argument(alias = "p")
  private static Integer port = 8000;

  @Argument(alias = "d", required = true)
  private static String dir;

  @Argument(alias = "m")
  private static String mocks;

  private static File rootDir;

  private static final FileTypeMap FILE_TYPE_MAP;

  private static final JsonFactory JSON_FACTORY = new MappingJsonFactory();

  static {
    FILE_TYPE_MAP = loadFileTypeMapFromContextSupportModule();
  }

  private static FileTypeMap loadFileTypeMapFromContextSupportModule() {
    // see if we can find the extended mime.types from the context-support module
    InputStream is = ClassLoader.getSystemResourceAsStream("com/sampullara/mustache/mimes.txt");
    if (null != is) {
      return new MimetypesFileTypeMap(is);
    }
    return FileTypeMap.getDefaultFileTypeMap();
  }

  public static Object toObject(final JsonNode node) {
    if (node.isArray()) {
      return new ArrayList() {
        {
          for (JsonNode jsonNodes : node) {
            add(toObject(jsonNodes));
          }
        }
      };
    } else if (node.isObject()) {
      return new HashMap() {
        {
          for (Iterator<Map.Entry<String, JsonNode>> i = node.getFields(); i.hasNext();) {
            Map.Entry<String, JsonNode> next = i.next();
            Object o = toObject(next.getValue());
            put(next.getKey(), o);
          }
        }
      };
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

    rootDir = new File(dir);
    if (null == mocks) mocks = dir;

    Handler handler = new AbstractHandler() {
      public void handle(String s, Request r, HttpServletRequest req, HttpServletResponse res)
          throws IOException, ServletException {
        try {
          String pathInfo = req.getPathInfo();
          if (pathInfo.endsWith("/")) pathInfo += "index.html";

          // obtain mime type
          String mimeType = FILE_TYPE_MAP.getContentType(pathInfo);
          System.out.println(String.format("%s: %s", mimeType, pathInfo));

          // create a handle to the resource
          File staticres = new File(rootDir, pathInfo.substring(1));

          res.setContentType(mimeType == null ? "text/html" : mimeType);
          res.setCharacterEncoding("utf-8");
          if (mimeType == null || mimeType.equals("text/html")) {

            // Handle like a template
            String filename = pathInfo.substring(1);

            // check if file exists
            if (!staticres.exists()) {
              res.setStatus(HttpServletResponse.SC_NOT_FOUND);
              processTemplate(req, res, "404.html");
            } else {
              res.setStatus(HttpServletResponse.SC_OK);
              processTemplate(req, res, filename);
            }

            r.setHandled(true);
          } else {
            if (!staticres.exists()) {
              res.setStatus(HttpServletResponse.SC_NOT_FOUND);
              return;
            }

            // Handle like a file
            res.setStatus(HttpServletResponse.SC_OK);
            ServletOutputStream out = res.getOutputStream();
            Files.copy(staticres.toPath(), out);
            out.close();
            r.setHandled(true);
          }
        } catch (Exception e) {
          // params
          Map<String, String> params = new HashMap<String, String>();
          StringWriter out = new StringWriter();
          PrintWriter pw = new PrintWriter(out);
          e.printStackTrace(pw);
          pw.close();
          params.put("stacktrace", out.toString());

          // render template
          res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          processTemplate(req, res, "500.html", params);

          r.setHandled(true);
        }
      }

      private void processTemplate(HttpServletRequest req, HttpServletResponse res,
          String filename, Object... scopes) throws UnsupportedEncodingException,
          FileNotFoundException, IOException, JsonParseException, JsonProcessingException {

        if (!new File(rootDir, filename).exists()) {
          System.out.println("template not found, skipping: " + filename);
          return;
        }

        MustacheFactory mc = new DefaultMustacheFactory(rootDir);
        Mustache mustache = mc.compile(filename);

        String base = filename.substring(0, filename.lastIndexOf("."));
        File file = new File(mocks, base + ".json");
        Map parameters = new HashMap<Object, Object>(req.getParameterMap()) {
          @Override
          public Object get(Object o) {
            Object result = super.get(o); // To change body of overridden methods use File |
                                          // Settings | File Templates.
            if (result instanceof String[]) {
              String[] strings = (String[]) result;
              if (strings.length == 1) {
                return strings[0];
              }
            }
            return result;
          }
        };

        List<Object> scs = new ArrayList<Object>();
        if (null != scopes) scs.addAll(Arrays.<Object>asList(scopes));
        scs.add(parameters);

        scs.add(new Object() {
          Function slots = new TemplateFunction() {
            @Override
            public String apply(String input) {
              return "{{>" + input.trim() + "}}";
            }
          };
        });

        if (file.exists()) {
          BufferedReader br =
              new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
          JsonParser parser = JSON_FACTORY.createJsonParser(br);
          JsonNode json = parser.readValueAsTree();
          br.close();
          scs.add(0, toObject(json));
        }
        mustache.execute(res.getWriter(), scs.toArray());
      }

      private String simpleEscape(String string) {
        return string.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
      }
    };

    Server server = new Server(port);
    server.setHandler(handler);
    server.start();
  }
}
