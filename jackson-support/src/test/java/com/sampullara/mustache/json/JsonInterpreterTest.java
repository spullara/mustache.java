package com.sampullara.mustache.json;

import com.google.common.collect.ImmutableMap;
import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheBuilder;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;
import junit.framework.TestCase;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Tests for the compiler.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:23:54 AM
 */
public class JsonInterpreterTest extends TestCase {
  private File root;

  public Object toObject(final JsonNode node) {
    if (node.isArray()) {
      return new ArrayList() {{
        for (JsonNode jsonNodes : node) {
          add(toObject(jsonNodes));
        }
      }};
    } else if (node.isObject()) {
      return new HashMap() {{
        for (Iterator<Map.Entry<String,JsonNode>> i = node.getFields(); i.hasNext(); ) {
          Map.Entry<String, JsonNode> next = i.next();
          Object o = toObject(next.getValue());
          put(next.getKey(), o);
        }
      }};
    } else if (node.isNull()) {
      return null;
    } else {
      return node.asText();
    }
  }

  public void testHogan() throws MustacheException, IOException {
    FutureWriter.setParallel(null);
    MustacheBuilder mb = new MustacheBuilder(root);
    Mustache parse = mb.parseFile("timeline.mustache");
    long start = System.currentTimeMillis();
    mb.parseFile("timeline.mustache");
    System.out.println(System.currentTimeMillis() - start);
    start = System.currentTimeMillis();
    MappingJsonFactory jf = new MappingJsonFactory();
    final Map node = (Map) toObject(jf.createJsonParser(
            "{\"in_reply_to_status_id\":null,\"possibly_sensitive\":false,\"in_reply_to_user_id_str\":null,\"contributors\":null,\"truncated\":false,\"id_str\":\"114176016611684353\",\"user\":{\"statuses_count\":5327,\"profile_use_background_image\":false,\"time_zone\":\"Pacific Time (US & Canada)\",\"protected\":false,\"default_profile\":false,\"notifications\":false,\"profile_text_color\":\"333333\",\"name\":\"Richard Henry\",\"expanded_url\":null,\"default_profile_image\":false,\"following\":true,\"verified\":false,\"geo_enabled\":true,\"profile_background_image_url\":\"http:\\/\\/a0.twimg.com\\/images\\/themes\\/theme1\\/bg.png\",\"favourites_count\":98,\"id_str\":\"31393\",\"utc_offset\":-28800,\"profile_link_color\":\"0084B4\",\"profile_image_url\":\"http:\\/\\/a3.twimg.com\\/profile_images\\/1192563998\\/Photo_on_2010-02-22_at_23.32_normal.jpeg\",\"description\":\"Husband to @chelsea. Designer at @twitter. English expat living in California.\",\"is_translator\":false,\"profile_background_image_url_https\":\"https:\\/\\/si0.twimg.com\\/images\\/themes\\/theme1\\/bg.png\",\"location\":\"San Francisco, CA\",\"follow_request_sent\":false,\"friends_count\":184,\"profile_background_color\":\"404040\",\"profile_background_tile\":false,\"url\":null,\"display_url\":null,\"profile_sidebar_fill_color\":\"DDEEF6\",\"followers_count\":2895,\"profile_image_url_https\":\"https:\\/\\/si0.twimg.com\\/profile_images\\/1192563998\\/Photo_on_2010-02-22_at_23.32_normal.jpeg\",\"entities\":{\"urls\":[],\"hashtags\":[],\"user_mentions\":[{\"name\":\"Chelsea Henry\",\"id_str\":\"16447200\",\"indices\":[11,19],\"id\":16447200,\"screen_name\":\"chelsea\"},{\"name\":\"Twitter\",\"id_str\":\"783214\",\"indices\":[33,41],\"id\":783214,\"screen_name\":\"twitter\"}]},\"lang\":\"en\",\"show_all_inline_media\":true,\"listed_count\":144,\"contributors_enabled\":false,\"profile_sidebar_border_color\":\"C0DEED\",\"id\":31393,\"created_at\":\"Wed Nov 29 22:40:31 +0000 2006\",\"screen_name\":\"richardhenry\"},\"retweet_count\":0,\"in_reply_to_user_id\":null,\"favorited\":false,\"geo\":null,\"in_reply_to_screen_name\":null,\"entities\":{\"urls\":[],\"hashtags\":[],\"media\":[{\"type\":\"photo\",\"expanded_url\":\"http:\\/\\/twitter.com\\/richardhenry\\/status\\/114176016611684353\\/photo\\/1\",\"id_str\":\"114176016615878656\",\"indices\":[22,42],\"url\":\"http:\\/\\/t.co\\/wJxLOTOR\",\"media_url\":\"http:\\/\\/p.twimg.com\\/AZWie3BCMAAcQJj.jpg\",\"display_url\":\"pic.twitter.com\\/wJxLOTOR\",\"id\":114176016615878656,\"media_url_https\":\"https:\\/\\/p.twimg.com\\/AZWie3BCMAAcQJj.jpg\",\"sizes\":{\"small\":{\"h\":254,\"w\":340,\"resize\":\"fit\"},\"large\":{\"h\":765,\"w\":1024,\"resize\":\"fit\"},\"thumb\":{\"h\":150,\"w\":150,\"resize\":\"crop\"},\"medium\":{\"h\":448,\"w\":600,\"resize\":\"fit\"}}}],\"user_mentions\":[]},\"coordinates\":null,\"source\":\"\\u003Ca href=\\\"http:\\/\\/twitter.com\\\" rel=\\\"nofollow\\\"\\u003ETwitter for  iPhone\\u003C\\/a\\u003E\",\"place\":null,\"retweeted\":false,\"id\":114176016611684353,\"in_reply_to_status_id_str\":null,\"annotations\":null,\"text\":\"LOOK AT WHAT ARRIVED. http:\\/\\/t.co\\/wJxLOTOR\",\"created_at\":\"Thu Sep 15 03:17:38 +0000 2011\"}").readValueAsTree());
    System.out.println(node);
    Object parent = new Object() {
      int uid = 0;
      List tweets = new ArrayList() {{
        for (int i = 0; i < 50; i++) {
          add(node);
        }
      }};
    };
    StringWriter writer = new StringWriter();
    parse.execute(writer, parent);

    start = System.currentTimeMillis();
    for (int i = 0; i < 500; i++) {
      parse.execute(new StringWriter(), parent);
    }
    System.out.println((System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    for (int i = 0; i < 500; i++) {
      parse.execute(new StringWriter(), parent);
    }
    System.out.println((System.currentTimeMillis() - start));
  }

  public void testSimpleWithJson() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    JsonFactory jf = new MappingJsonFactory();
    JsonNode jsonNode = jf.createJsonParser(
            "{\"name\":\"Chris\", \"value\":10000, \"taxed_value\":6000,\"in_ca\":true}").readValueAsTree();
    m.execute(sw, jsonNode);
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testSimpleWithJsonAndWriter() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    JsonFactory jf = new MappingJsonFactory();
    JsonNode jsonNode = jf.createJsonParser(
            "{\"name\":\"Chris\", \"value\":10000, \"taxed_value\":6000,\"in_ca\":true}").readValueAsTree();
    m.execute(writer, jsonNode);
    writer.flush();
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testJson() throws IOException, MustacheException {
    String content = getContents(root, "template_partial.js");
    content = content.substring(content.indexOf("=") + 1);
    JsonParser jp = new MappingJsonFactory().createJsonParser(content);
    JsonNode jsonNode = jp.readValueAsTree();
    Scope scope = new Scope(jsonNode);
    MustacheBuilder c = init();
    Mustache m = c.parseFile("template_partial.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, scope);
    writer.flush();
    assertEquals(getContents(root, "template_partial.txt"), sw.toString());

  }

//  public void testJSONHttpRequest() throws MustacheException, IOException {
//    MustacheBuilder c = init();
//    Mustache m = c.parseFile("simple2.html");
//    StringWriter sw = new StringWriter();
//    FutureWriter writer = new FutureWriter(sw);
//    m.execute(writer, new Scope(new Object() {
//      Future<JsonNode> data() throws IOException {
//        JSONHttpRequest jhr = new JSONHttpRequest("http://www.javarants.com/simple.json");
//        return jhr.execute();
//      }
//    }));
//    writer.flush();
//    assertEquals(getContents(root, "simple.txt"), sw.toString());
//  }

  public void testXSS() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("xss.html");
    final StringWriter json = new StringWriter();
    ImmutableMap<String, Object> of = ImmutableMap.<String, Object>of("foo", "bar", "\"baz\"", 42);
    MappingJsonFactory jf = new MappingJsonFactory();
    JsonGenerator jg = jf.createJsonGenerator(json);
    jg.writeObject(of);
    jg.flush();
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      String message = "I <3 Ponies!";
      String object = json.toString();
    }));
    writer.flush();
    assertEquals(getContents(root, "xss.txt"), sw.toString());
  }

  private MustacheBuilder init() {
    Scope.setDefaultObjectHandler(new JsonObjectHandler());
    return new MustacheBuilder(root);
  }

  protected String getContents(File root, String file) throws IOException {
    BufferedReader br = new BufferedReader(
            new InputStreamReader(new FileInputStream(new File(root, file)), "UTF-8"));
    StringWriter capture = new StringWriter();
    char[] buffer = new char[8192];
    int read;
    while ((read = br.read(buffer)) != -1) {
      capture.write(buffer, 0, read);
    }
    return capture.toString();
  }

  protected void setUp() throws Exception {
    super.setUp();
    File file = new File("src/test/resources");
    root = new File(file, "simple.html").exists() ? file : new File("../src/test/resources");
  }

}
