package mustachejava.benchmarks;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejavabenchmarks.NullWriter;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

 */
@State(Scope.Benchmark)
public class TweetBench {

  DefaultMustacheFactory dmf = new DefaultMustacheFactory();
  Mustache m = dmf.compile("tweet.mustache");
  Tweet tweet = new Tweet();
  NullWriter nullWriter = new NullWriter();
  List<Object> tweetScope = new ArrayList<>(singletonList(tweet));
  List<Object> jsonScope;

  public TweetBench() {
    try {
      MappingJsonFactory jf = new MappingJsonFactory();
      InputStream json = TweetBench.class.getClassLoader().getResourceAsStream("tweet.json");
      jsonScope = new ArrayList<>(singletonList(new JsonMap(jf.createJsonParser(json).readValueAsTree())));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void testCompilation() {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache m = dmf.compile("tweet.mustache");
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void testExecution() throws IOException {
    m.execute(nullWriter, tweetScope).close();
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void testExecutionWithStringWriter() throws IOException {
    StringWriter writer = new StringWriter();
    m.execute(writer, tweetScope).close();
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void testJsonExecution() throws IOException {
    m.execute(nullWriter, jsonScope).close();
  }

  public static void main(String[] args) throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache m = dmf.compile("tweet.mustache");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Tweet()).close();
    System.out.println(sw);

    InputStream json = TweetBench.class.getClassLoader().getResourceAsStream("tweet.json");
    MappingJsonFactory jf = new MappingJsonFactory();
    JsonNode jsonNode = jf.createJsonParser(json).readValueAsTree();
    sw = new StringWriter();
    m.execute(sw, new JsonMap(jsonNode)).close();
    System.out.println(sw);

    TweetBench tb = new TweetBench();
    while (true) {
      tb.testExecution();
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
        return value.getBooleanValue();
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
