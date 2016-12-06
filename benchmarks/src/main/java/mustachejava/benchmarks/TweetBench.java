package mustachejava.benchmarks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheResolver;
import com.github.mustachejavabenchmarks.NullWriter;
import org.openjdk.jmh.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;

/**
 * Created by sam on 3/19/16.

 Initial test 5b9fce4
 Benchmark                                  Mode  Cnt       Score       Error  Units
 TweetBench.testCompilation                thrpt   20    8070.287 ±   567.640  ops/s
 TweetBench.testExecution                  thrpt   20  389133.230 ± 15624.712  ops/s
 TweetBench.testExecutionWithStringWriter  thrpt   20  157934.510 ± 41675.991  ops/s
 TweetBench.testJsonExecution              thrpt   20  239396.118 ± 11455.727  ops/s

 Added recursion and escaping optimization e8df5360
 Benchmark                  Mode  Cnt       Score       Error  Units
 TweetBench.testExecution  thrpt   20  412346.447 ± 19794.264  ops/s

 TweetBench.testCompilation                thrpt   20       7529.923 ±      617.342  ops/s
 TweetBench.testExecution                  thrpt   20     407067.266 ±    12712.684  ops/s
 TweetBench.testExecutionWithStringWriter  thrpt   20     177134.276 ±     6049.166  ops/s
 TweetBench.testJsonExecution              thrpt   20     236385.894 ±    15152.458  ops/s

 */
@State(Scope.Benchmark)
public class TweetBench {

  Mustache tweetMustache = new DefaultMustacheFactory().compile("tweet.mustache");
  Mustache timelineMustache = new DefaultMustacheFactory().compile("timeline.mustache");
  Tweet tweet = new Tweet();
  NullWriter nullWriter = new NullWriter();
  List<Object> tweetScope = new ArrayList<>(singletonList(tweet));
  List<Object> timelineScope = new ArrayList<>();
  {
    List<Tweet> tweetList = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      tweetList.add(new Tweet());
    }
    timelineScope.add(new Object() {
      List<Tweet> tweets = tweetList;
    });
  }
  List<Object> jsonScope;
  Map<String, String> cache = new HashMap<>();
  {
    cache.put("tweet.mustache", readResource("tweet.mustache"));
    cache.put("entities.mustache", readResource("entities.mustache"));
  }
  MustacheResolver cached = resourceName -> new StringReader(cache.get(resourceName));

  private String readResource(String name) {
    StringWriter sw = new StringWriter();
    InputStreamReader reader = new InputStreamReader(ClassLoader.getSystemResourceAsStream(name));
    char[] chars = new char[1024];
    int read;
    try {
      while ((read = reader.read(chars)) != -1) {
        sw.write(chars, 0, read);
      }
      sw.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return sw.toString();
  }

  public TweetBench() {
    try {
      MappingJsonFactory jf = new MappingJsonFactory();
      InputStream json = TweetBench.class.getClassLoader().getResourceAsStream("tweet.json");
      jsonScope = new ArrayList<>(singletonList(new JsonMap(jf.createParser(json).readValueAsTree())));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void testCompilation() {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory(cached);
    Mustache m = dmf.compile("tweet.mustache");
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void testExecution() throws IOException {
    tweetMustache.execute(nullWriter, tweetScope).close();
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void testTimeline() throws IOException {
    timelineMustache.execute(nullWriter, timelineScope).close();
  }

  public static void main(String[] args) throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache m = dmf.compile("tweet.mustache");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Tweet()).close();
    System.out.println(sw);

    InputStream json = TweetBench.class.getClassLoader().getResourceAsStream("tweet.json");
    MappingJsonFactory jf = new MappingJsonFactory();
    JsonNode jsonNode = jf.createParser(json).readValueAsTree();
    sw = new StringWriter();
    m.execute(sw, new JsonMap(jsonNode)).close();
    System.out.println(sw);

    TweetBench tb = new TweetBench();
    while (true) {
      tb.testCompilation();
    }
  }

  private static class JsonMap extends HashMap {
    private final JsonNode test;

    public JsonMap(JsonNode test) {
      this.test = test;
    }

    @Override
    public Object get(Object key) {
      JsonNode value = test.get(key.toString());
      return convert(value);
    }

    @Override
    public boolean containsKey(Object key) {
      return test.has(key.toString());
    }

    private Object convert(final JsonNode value) {
      if (value == null || value.isNull()) return null;
      if (value.isBoolean()) {
        return value.booleanValue();
      } else if (value.isValueNode()) {
        return value.asText();
      } else if (value.isArray()) {
        return (Iterable) () -> new Iterator() {
          private Iterator<JsonNode> iterator = value.iterator();

          @Override
          public boolean hasNext() {
            return iterator.hasNext();
          }

          @Override
          public Object next() {
            return convert(iterator.next());
          }

          @Override
          public void remove() {
          }
        };
      } else {
        return new JsonMap(value);
      }
    }
  }

}
