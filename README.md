Mustache.java
=============

**Mustache.java** is a derivative of [mustache.js](http://mustache.github.com/mustache.5.html).

There is a Google Group for support and questions: <http://groups.google.com/group/mustachejava>

Request for contributions:

- Real world benchmarks that matter - currently benchmarking based on Twitter templates
- Documentation
- Bug reports / fixes
- API feedback
- Optimizations

Documentation:

- [Mustache.js manual](http://mustache.github.com/mustache.5.html)
- Passes all of the `mustache` [specification tests](https://github.com/mustache/spec) modulo whitespace differences
- Biggest difference between mustache.js and mustache.java is optional concurrent evaluation
- Data is provided by objects in an array of scopes and are accessed via non-private fields, methods or maps
- Any `Iterable` can be used for list-like behaviors
- Returning a `Callable` allows for concurrent evaluation if an `ExecutorService` is configured
- Template inheritance is supported by this implementation, see <https://github.com/mustache/spec/issues/38> (eg. `{{<super}}{{$content}}...{{/content}}{{/super}}`)
- Additional functions/lambdas (eg. `{{#func1}}...{{/func1}}`) are implemented using `Function` from Google Guava
- Use `TemplateFunction` if you want mustache.java to reparse the results of your function/lambda
- Both default and manually configured classpath based and file system based template roots are supported
- An invokedynamic version is in development but currently is no faster than the default reflection based system
- The `handlebar` server will render templates + json data for quick mockups of templates by designers
- Completely pluggable system for overriding almost all the behavior in the compilation and rendering process
- You can pull out sample data from live systems using the `CapturingMustacheVisitor` for mocks and tests

Performance:

- See the `com.github.mustachejavabenchmarks` package in the `compiler` module
- Compiles 4000+ timeline.html templates per second per core
- Renders 3000+ of 50 tweet timelines per second per core on 2011 Macbook Pro / MacPro hardware
- New codegen module generates code for guards and mustaches

Maven dependency information (ie. for most common cases you will just need the `compiler` module):

```xml
<dependency>
  <groupId>com.github.spullara.mustache.java</groupId>
  <artifactId>compiler</artifactId>
  <version>0.8.6</version>
</dependency>
```

Example template file:

	{{#items}}
	Name: {{name}}
	Price: {{price}}
	  {{#features}}
	  Feature: {{description}}
	  {{/features}}
	{{/items}}

Might be powered by some backing code:

```java
public class Context {
  List<Item> items() {
    return Arrays.asList(
      new Item("Item 1", "$19.99", Arrays.asList(new Feature("New!"), new Feature("Awesome!"))),
      new Item("Item 2", "$29.99", Arrays.asList(new Feature("Old."), new Feature("Ugly.")))
    );
  }

  static class Item {
    Item(String name, String price, List<Feature> features) {
      this.name = name;
      this.price = price;
      this.features = features;
    }
    String name, price;
    List<Feature> features;
  }

  static class Feature {
    Feature(String description) {
       this.description = description;
    }
    String description;
  }
}
```

And would result in:

	Name: Item 1
	Price: $19.99
	  Feature: New!
	  Feature: Awesome!
	Name: Item 2
	Price: $29.99
	  Feature: Old.
	  Feature: Ugly.

Evaluation of the template proceeds serially. For instance, if you have blocking code within one of your callbacks
you the system will pause while executing them:

```java
static class Feature {
  Feature(String description) {
    this.description = description;
  }

  String description() throws InterruptedException {
    Thread.sleep(1000);
    return description;
  }
}
```

If you change description to return a `Callable` instead it will automatically be executed in a separate
thread if you have provided an `ExecutorService` when you created your `MustacheFactory`.

```java
Callable<String> description() throws InterruptedException {
  return new Callable<String>() {

    @Override
    public String call() throws Exception {
      Thread.sleep(1000);
      return description;
    }
  };
}
```

This enables scheduled tasks, streaming behavior and asynchronous i/o. Check out the `example` module in order
to see a complete end-to-end example:

```java
package mustachejava;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

public class Example {

  List<Item> items() {
    return Arrays.asList(
      new Item("Item 1", "$19.99", Arrays.asList(new Feature("New!"), new Feature("Awesome!"))),
      new Item("Item 2", "$29.99", Arrays.asList(new Feature("Old."), new Feature("Ugly.")))
    );
  }

  static class Item {
    Item(String name, String price, List<Feature> features) {
      this.name = name;
      this.price = price;
      this.features = features;
    }

    String name, price;
    List<Feature> features;
  }

  static class Feature {
    Feature(String description) {
      this.description = description;
    }

    String description;
  }

  public static void main(String[] args) throws IOException {
    MustacheFactory mf = new DefaultMustacheFactory();
    Mustache mustache = mf.compile("template.mustache");
    mustache.execute(new PrintWriter(System.out), new Example()).flush();
  }
}
```

An alternative approach for providing variables would be to use a Map object, like:

```java
  public static void main(String[] args) throws IOException {
    HashMap<String, Object> scopes = new HashMap<String, Object>();
    scopes.put("name", "Mustache");
    scopes.put("feature", new Feature("Perfect!"));

    Writer writer = new OutputStreamWriter(System.out);
    MustacheFactory mf = new DefaultMustacheFactory();
    Mustache mustache = mf.compile(new StringReader("{{name}}, {{feature.description}}!"), "example");
    mustache.execute(writer, scopes);
    writer.flush();
  }
```
