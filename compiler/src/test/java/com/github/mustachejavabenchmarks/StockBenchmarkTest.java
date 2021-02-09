package com.github.mustachejavabenchmarks;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

public class StockBenchmarkTest {

  private com.github.mustachejava.Mustache template;

  protected Map<String, Object> getContext() {
    Map<String, Object> context = new HashMap<>();
    context.put("items", Stock.dummyItems());
    return context;
  }

  public void setup() {
    DefaultMustacheFactory mustacheFactory = new DefaultMustacheFactory() {
      @Override
      public void encode(String value, Writer writer) {
        // Disable HTML escaping
        try {
          writer.write(value);
        } catch (IOException e) {
          throw new MustacheException(e);
        }
      }
    };
    template = mustacheFactory.compile("stocks.mustache.html");
  }

  @Test
  @Ignore
  public void testMustacheBenchmark() {
    StockBenchmarkTest mustache = new StockBenchmarkTest();
    mustache.setup();

    long start = System.currentTimeMillis();
    for (int i = 0; i < 500_000; i++) {
      mustache.benchmark();
      start = progress(start, i);
    }
  }

  private long progress(long start, int i) {
    if (i != 0 && i % 20000 == 0) {
      long diff = System.currentTimeMillis() - start;
      System.out.println(20000 * 1000 / diff);
      start = System.currentTimeMillis();
    }
    return start;
  }

  @SuppressWarnings("unchecked")
  public String benchmark() {

    Map<String, Object> data = getContext();
    data.put("items", new StockCollection((Collection<Stock>) data.get("items")));

    Writer writer = new StringWriter(60000);
    template.execute(writer, new Object[] { data });
    return writer.toString();
  }

  /**
   * This is a modified copy of
   * {@link com.github.mustachejava.util.DecoratedCollection} - we need the
   * first element at index 1.
   */
  private class StockCollection extends AbstractCollection<StockView> {

    private final Collection<Stock> c;

    public StockCollection(Collection<Stock> c) {
      this.c = c;
    }

    @Override
    public Iterator<StockView> iterator() {
      final Iterator<Stock> iterator = c.iterator();
      return new Iterator<StockView>() {

        int index = 1;

        @Override
        public boolean hasNext() {
          return iterator.hasNext();
        }

        @Override
        public StockView next() {
          Stock next = iterator.next();
          int current = index++;
          return new StockView(current, current == 1, !iterator.hasNext(), next);
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public int size() {
      return c.size();
    }
  }

  static class StockView {

    private final int index;

    private final boolean first;

    private final boolean last;

    private final Stock value;

    private final String negativeClass;

    private final String rowClass;

    public StockView(int index, boolean first, boolean last, Stock value) {
      this.index = index;
      this.first = first;
      this.last = last;
      this.value = value;
      this.negativeClass = value.getChange() > 0 ? "" : "class=\"minus\"";
      this.rowClass = index % 2 == 0 ? "even" : "odd";
    }

    public String getNegativeClass() {
      return negativeClass;
    }

    public String getRowClass() {
      return rowClass;
    }

    public int getIndex() {
      return index;
    }

    public boolean isFirst() {
      return first;
    }

    public boolean isLast() {
      return last;
    }

    public Stock getValue() {
      return value;
    }
  }

}
