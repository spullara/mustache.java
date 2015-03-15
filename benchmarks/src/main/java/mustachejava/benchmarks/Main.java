package mustachejava.benchmarks;

import com.github.mustachejava.util.HtmlEscaper;
import com.github.mustachejavabenchmarks.NullWriter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

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


 */
@State(Scope.Benchmark)
public class Main {

  private final NullWriter nw = new NullWriter();

  @Benchmark
  public void benchJustEscapeClean() {
    HtmlEscaper.escape("variable", nw);
  }

  @Benchmark
  public void benchJustEscapeOnce() {
    HtmlEscaper.escape("vari\"ble", nw);
  }

  @Benchmark
  public void benchJustEscapeTwo() {
    HtmlEscaper.escape("va&ri\"ble", nw);
  }
}
