package mustachejava.benchmarks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheResolver;
import com.github.mustachejavabenchmarks.NullWriter;
import com.sun.management.ThreadMXBean;
import org.openjdk.jmh.annotations.*;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;

/**
 * Created by sam on 3/19/16.
 * <p>
 * Initial test 5b9fce4
 * Benchmark                                  Mode  Cnt       Score       Error  Units
 * TweetBench.testCompilation                thrpt   20    8070.287 ±   567.640  ops/s
 * TweetBench.testExecution                  thrpt   20  389133.230 ± 15624.712  ops/s
 * TweetBench.testExecutionWithStringWriter  thrpt   20  157934.510 ± 41675.991  ops/s
 * TweetBench.testJsonExecution              thrpt   20  239396.118 ± 11455.727  ops/s
 * <p>
 * Added recursion and escaping optimization e8df5360
 * Benchmark                  Mode  Cnt       Score       Error  Units
 * TweetBench.testExecution  thrpt   20  412346.447 ± 19794.264  ops/s
 * <p>
 * TweetBench.testCompilation                thrpt   20       7529.923 ±      617.342  ops/s
 * TweetBench.testExecution                  thrpt   20     407067.266 ±    12712.684  ops/s
 * TweetBench.testExecutionWithStringWriter  thrpt   20     177134.276 ±     6049.166  ops/s
 * TweetBench.testJsonExecution              thrpt   20     236385.894 ±    15152.458  ops/s
 * <p>
 * Java 1.8.0_144
 * Benchmark                    Mode  Cnt           Score          Error  Units
 * Main.benchJustEscapeClean   thrpt   20   554814932.404 ± 10199017.578  ops/s
 * Main.benchJustEscapeTwo     thrpt   20   171512683.535 ±  4149779.926  ops/s
 * Main.benchMustache          thrpt   20      983399.501 ±    38625.627  ops/s
 * TweetBench.testCompilation  thrpt   20       57576.565 ±     1109.601  ops/s
 * TweetBench.testExecution    thrpt   20      461662.966 ±    43016.740  ops/s
 * TweetBench.testTimeline     thrpt   20       24707.624 ±     1411.883  ops/s
 * <p>
 * Java 9+181
 * Benchmark                    Mode  Cnt           Score           Error  Units
 * Main.benchJustEscapeClean   thrpt   20  1703973020.899 ± 126794076.169  ops/s
 * Main.benchJustEscapeTwo     thrpt   20  1173719723.660 ±  40555137.903  ops/s
 * Main.benchMustache          thrpt   20      982541.378 ±     29570.703  ops/s
 * TweetBench.testCompilation  thrpt   20       44001.507 ±      1793.522  ops/s
 * TweetBench.testExecution    thrpt   20      444469.477 ±     12699.894  ops/s
 * TweetBench.testTimeline     thrpt   20       21863.356 ±       352.252  ops/s
 */
@SuppressWarnings("unused")
@State(Scope.Benchmark)
public class TweetBench {

  private Mustache tweetMustache = new DefaultMustacheFactory().compile("tweet.mustache");
  private Mustache timelineMustache = new DefaultMustacheFactory().compile("timeline.mustache");
  private Tweet tweet = new Tweet();
  private NullWriter nullWriter = new NullWriter();
  private List<Object> tweetScope = new ArrayList<>(singletonList(tweet));
  private List<Object> timelineScope = new ArrayList<>();

  {
    List<Tweet> tweetList = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      tweetList.add(new Tweet());
    }
    timelineScope.add(new Object() {
      List<Tweet> tweets = tweetList;
    });
  }

  private Map<String, String> cache = new HashMap<>();

  {
    cache.put("tweet.mustache", readResource("tweet.mustache"));
    cache.put("entities.mustache", readResource("entities.mustache"));
  }

  private MustacheResolver cached = resourceName -> new StringReader(cache.get(resourceName));

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
      new ArrayList<>(singletonList(new JsonMap(jf.createParser(json).readValueAsTree())));
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


  private static ThreadMXBean threadMXBean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

  public static void main(String[] args) throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache m = dmf.compile("tweet.mustache");
    StringWriter sw1 = new StringWriter();
    m.execute(sw1, new Tweet()).close();
    System.out.println(sw1);

    InputStream json = TweetBench.class.getClassLoader().getResourceAsStream("tweet.json");
    MappingJsonFactory jf = new MappingJsonFactory();
    JsonNode jsonNode = jf.createParser(json).readValueAsTree();
    StringWriter sw2 = new StringWriter();
    m.execute(sw2, new JsonMap(jsonNode)).close();
    System.out.println(sw2);

    System.out.println(sw1.toString().equals(sw2.toString()));


    TweetBench tb = new TweetBench();
    int i = 0;
    long startTime = System.nanoTime();
    long threadId = Thread.currentThread().getId();
    long startMemory = threadMXBean.getThreadAllocatedBytes(threadId);
    int n = 0;
    while (true) {
      if (++i == 100000) {
        long endTime = System.nanoTime();
        long diffTime = endTime - startTime;
        long endMemory = threadMXBean.getThreadAllocatedBytes(threadId);
        long diffMemory = endMemory - startMemory;
        System.out.println(diffTime / i + " ns/iteration, " + diffMemory / i + " bytes/iteration, " + 1.0e9 / diffTime * i + " per second");
        startTime = endTime;
        startMemory = endMemory;
        i = 0;
        if (++n == 5) break;
      }
      tb.testTimeline();
    }
  }

  private static class JsonMap extends HashMap {
    private final JsonNode test;

    JsonMap(JsonNode test) {
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
