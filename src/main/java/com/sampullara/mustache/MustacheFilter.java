package com.sampullara.mustache;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core routing and templating servlet.
 * <p/>
 * User: sam
 * Date: May 5, 2010
 * Time: 12:24:46 PM
 */
public class MustacheFilter implements Filter {

  private static Logger logger = Logger.getLogger(MustacheFilter.class.getName());
  private static boolean debug;

  /**
   * Configured with a mapping file with the following json format:
   * <p/>
   * [
   * {
   * "route":"regex to match against path",
   * "template":"template file",
   * "context":"class name of root scope"
   * }
   * ]
   */

  private JsonNode map;
  private MustacheCompiler mc;
  private File root;

  private static String getValue(JsonNode mappingSpec, String key) throws ServletException {
    JsonNode jsonNode = mappingSpec.get(key);
    if (jsonNode == null || !jsonNode.isTextual()) {
      throw new ServletException("You must set the route to a text value");
    }
    return jsonNode.getTextValue();
  }

  private List<Mapping> mappings = new ArrayList<Mapping>();

  public void init(FilterConfig config) throws ServletException {
    String rootDir = config.getInitParameter("root");
    if (rootDir == null) {
      throw new ServletException("You must set the root init-parameter");
    } else if (!(root = new File(rootDir)).exists()) {
      throw new ServletException(rootDir + " does not exist");
    }
    String mapName = config.getInitParameter("map");
    if (mapName == null) {
      throw new ServletException("map init-parameter resource not set");
    }
    InputStream mapFile = getClass().getResourceAsStream(mapName);
    if (mapFile == null) {
      throw new ServletException("You must set the map init-parameter to an available resource");
    }
    try {
      JsonParser jp = new MappingJsonFactory().createJsonParser(mapFile);
      map = jp.readValueAsTree();
    } catch (IOException e) {
      throw new ServletException("Failed to parse map file: " + mapName);
    }
    mc = new MustacheCompiler(root);
    if (map.isArray()) {
      for (JsonNode mappingSpec : map) {
        Mapping mapping = new Mapping();
        logger.info("Registering: " + mappingSpec);
        mapping.regex = Pattern.compile(getValue(mappingSpec, "route"));
        try {
          mapping.context = Class.forName(getValue(mappingSpec, "context"));
        } catch (ClassNotFoundException e) {
          throw new ServletException("Failed to get class", e);
        }
        try {
          String path = getValue(mappingSpec, "template");
          File templateFile = new File(root, path);
          if (!templateFile.exists()) {
            throw new ServletException("Template is not present: " + templateFile);
          }
          mapping.timestamp = templateFile.lastModified();
          mapping.template = mc.parseFile(path);
        } catch (MustacheException e) {
          throw new ServletException("Failed to compile template", e);
        }
        mappings.add(mapping);
      }
    } else {
      throw new ServletException("Map file must be a json array");
    }
    // This will auto reload templates on modification
    String debug = config.getInitParameter("debug");
    if (debug != null && new Boolean(debug)) {
      this.debug = true;
    }
  }

  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    HttpServletRequest rq = (HttpServletRequest) servletRequest;
    HttpServletResponse rs = (HttpServletResponse) servletResponse;
    for (Mapping mapping : mappings) {
      String uri = rq.getRequestURI();
      Matcher matcher = mapping.regex.matcher(uri);
      if (matcher.matches()) {
        try {
          rs.setContentType("text/html");
          Scope scope = new Scope(mapping.context.newInstance());
          for (int i = 0; i < matcher.groupCount() + 1; i++) {
            scope.put("route_" + i, matcher.group(i));
          }
          scope.put("request", rq);
          scope.put("response", rs);
          if (debug) {
            String path = mapping.template.getPath();
            File templateFile = new File(root, path);
            if (templateFile.exists()) {
              if (templateFile.lastModified() > mapping.timestamp) {
                mapping.template = mc.parseFile(path);
              }
            } else {
              return;
            }
          }
          MustacheWriter mw = new MustacheWriter(rs.getWriter());
          mapping.template.execute(mw, scope);
          mw.flush();
        } catch (Exception e) {
          logger.log(Level.SEVERE, "Failed to execute template", e);
        }
        // Do not continue the chain if we match
        return;
      }
    }
    filterChain.doFilter(servletRequest, servletResponse);
  }

  public void destroy() {
  }

  static class Mapping {
    Pattern regex;
    Mustache template;
    Class context;
    long timestamp;
  }
}
