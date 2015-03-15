package mustachejava.benchmarks;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.codegen.CodegenObjectHandler;
import com.github.mustachejava.indy.IndyObjectHandler;
import com.github.mustachejavabenchmarks.NullWriter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

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
Main.benchMustache         thrpt   20  1046459.757 ± 36631.743  ops/s
Main.benchMustacheCodegen  thrpt   20  1066092.882 ± 33464.632  ops/s
Main.benchMustacheIndy     thrpt   20  1087113.076 ± 21833.084  ops/s

 */
@State(Scope.Benchmark)
public class Main {

  private final NullWriter nw = new NullWriter();
  private final Mustache normal = new DefaultMustacheFactory().compile(new StringReader("({{#loop}}({{value}}){{/loop}})"), "test");
  private final Mustache indy;
  private final Mustache codegen;
  private final Object scope = new Object() {
    List loop = new ArrayList();
    {
      for (int i = 0; i < 10; i++) {
        final int finalI = i;
        loop.add(new Object() {
          String value = String.valueOf(finalI);
        });
      }
    }
  };

  {
    DefaultMustacheFactory dmf1 = new DefaultMustacheFactory();
    dmf1.setObjectHandler(new CodegenObjectHandler());
    codegen = dmf1.compile(new StringReader("({{#loop}}({{value}}){{/loop}})"), "test");
    DefaultMustacheFactory dmf2 = new DefaultMustacheFactory();
    dmf2.setObjectHandler(new IndyObjectHandler());
    indy = dmf1.compile(new StringReader("({{#loop}}({{value}}){{/loop}})"), "test");
  }
  
  @Benchmark
  public void benchMustache() throws IOException {
    normal.execute(nw, scope).close();
  }
  
  @Benchmark
  public void benchMustacheCodegen() throws IOException {
    normal.execute(nw, scope).close();
  }

  @Benchmark
  public void benchMustacheIndy() throws IOException {
    normal.execute(nw, scope).close();
  }
//  @Benchmark
//  public void benchTheoreticalLimit() throws IOException {
//    nw.write("variable");
//  }
//  
//  @Benchmark
//  public void benchJustEscapeClean() {
//    HtmlEscaper.escape("variable", nw);
//  }
//
//  @Benchmark
//  public void benchJustEscapeOnce() {
//    HtmlEscaper.escape("vari\"ble", nw);
//  }
//
//  @Benchmark
//  public void benchJustEscapeTwo() {
//    HtmlEscaper.escape("va&ri\"ble", nw);
//  }
  
  public static void main(String[] args) throws IOException {
    Main main = new Main();
    while(true) {
      main.benchMustache();
    }
  }
}
