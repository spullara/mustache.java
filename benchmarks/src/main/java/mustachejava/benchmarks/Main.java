package mustachejava.benchmarks;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.util.HtmlEscaper;
import com.github.mustachejavabenchmarks.NullWriter;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/*

Benchmark                   Mode  Cnt         Score         Error  Units
Main.benchJustEscapeClean  thrpt   20  51860651.412 ± 3395632.169  ops/s
Main.benchJustEscapeOnce   thrpt   20  44812190.360 ± 1502518.508  ops/s
Main.benchJustEscapeTwo    thrpt   20  35545561.198 ±  821159.854  ops/s

After removing !escapeEscaped support and moving to single appends. Reduction in GC as well.
Benchmark                   Mode  Cnt         Score         Error  Units
Main.benchJustEscapeClean  thrpt   20  72200826.995 ± 5209673.647  ops/s
Main.benchJustEscapeOnce   thrpt   20  70819655.505 ± 2323357.730  ops/s
Main.benchJustEscapeTwo    thrpt   20  52694326.532 ± 1906826.170  ops/s

Switching entirely to writing char arrays for entities:
Benchmark                   Mode  Cnt          Score          Error  Units
Main.benchJustEscapeClean  thrpt   20  104535652.053 ± 14023384.161  ops/s
Main.benchJustEscapeOnce   thrpt   20  107039586.096 ±  6607556.854  ops/s
Main.benchJustEscapeTwo    thrpt   20   98931791.187 ±  4212931.813  ops/s

Bypass switch statement when we can:
Benchmark                   Mode  Cnt          Score          Error  Units
Main.benchJustEscapeClean  thrpt   20  196904626.848 ± 19804749.810  ops/s
Main.benchJustEscapeOnce   thrpt   20  102768645.605 ±  3764598.754  ops/s
Main.benchJustEscapeTwo    thrpt   20   81450001.026 ±  2270993.769  ops/s

Benchmark            Mode  Cnt       Score       Error  Units
Main.benchMustache  thrpt   20  763256.970 ± 59474.781  ops/s

Converstion of array scope to list scope:
Benchmark            Mode  Cnt       Score       Error  Units
Main.benchMustache  thrpt   20  913015.783 ± 40586.647  ops/s

Basically I can delete codegen/indy:
Main.benchMustache         thrpt   20  982355.601 ± 30641.685  ops/s
Main.benchMustacheCodegen  thrpt   20  987436.507 ± 54305.222  ops/s
Main.benchMustacheIndy     thrpt   20  818468.833 ± 71852.617  ops/s
Main.benchMustacheSimple   thrpt   20  683899.043 ± 43079.288  ops/s

Now purely an array lookup
Benchmark                      Mode  Cnt          Score          Error  Units
Main.benchJustEscapeClean     thrpt   20  408133726.531 ± 41452124.506  ops/s
Main.benchJustEscapeOldClean  thrpt   20  136543848.645 ±  4981651.587  ops/s
Main.benchJustEscapeOnce      thrpt   20  216812988.639 ± 16074660.164  ops/s
Main.benchJustEscapeTwo       thrpt   20  101607565.357 ±  5789918.319  ops/s

Main.benchJustEscapeClean                 thrpt   20  397296922.707 ± 77462659.664  ops/s
Main.benchJustEscapeOnce                  thrpt   20  216287927.848 ± 13641785.965  ops/s
Main.benchJustEscapeTwo                   thrpt   20  106926157.896 ±  5172497.616  ops/s
Main.benchMustache                        thrpt   20     897792.899 ±    44805.792  ops/s
Main.benchMustacheSimple                  thrpt   20     588578.957 ±    61712.824  ops/s

 */
@State(Scope.Benchmark)
public class Main {

  private final NullWriter nw = new NullWriter();
  private final Mustache normal = new DefaultMustacheFactory().compile(new StringReader("({{#loop}}({{value}}){{/loop}})"), "test");

  private final List<Object> scope = new ArrayList<>(Collections.singletonList(new Object() {
    List loop = new ArrayList();

    {
      for (int i = 0; i < 10; i++) {
        final int finalI = i;
        loop.add(new Object() {
          String value = String.valueOf(finalI);
        });
      }
    }
  }));

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void benchMustache() throws IOException {
    normal.execute(nw, scope).close();
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void benchJustEscapeClean() {
    HtmlEscaper.escape("variable", nw);
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void benchJustEscapeTwo() {
    HtmlEscaper.escape("va&ri\"ble", nw);
  }

  public static void main(String[] args) throws IOException {
    Main main = new Main();
    while (true) {
      main.benchMustache();
    }
  }
}
