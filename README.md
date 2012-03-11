Mustache.java
=============

**Mustache.java** is a derivative of [mustache.js](http://mustache.github.com/mustache.5.html).

There is a Google Group for support and questions: <http://groups.google.com/group/mustachejava>

Documentation:

- For the trivial use case see the Tests and mustache.js documentation
- Biggest difference between mustache.js and mustache.java is concurrency

Example:

	{{#items}}
	Name: {{name}}
	Price: {{price}}
	  {{#features}}
	  Feature: {{description}}
	  {{/features}}
	{{/items}}

Might be powered by:

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

And would result in:

	Name: Item 1
	Price: $19.99
	  Feature: New!
	  Feature: Awesome!
	Name: Item 2
	Price: $29.99
	  Feature: Old.
	  Feature: Ugly.

In terms of how this is executed, what you would see if you look under the covers is that at every
`{{#}}` tag the execution of that section is pushed into an execution queue.  
This allows you to trivially
code highly concurrent web pages and get the lowest possible latency without complicated threading
semantics in your code.  For example, if you were to change the Feature class:

    static class Feature {
      Feature(String description) {
        this.description = description;
      }

      String description;

      String desc() throws InterruptedException {
        Thread.sleep(1000);
        return description;
      }
    }

And the HTML to reference desc instead, you would find that the whole page still only takes
approximately 1 second to execute, not 4(!) seconds.  Here is another example using the new `CallbackFuture`
in order to not use a up a thread for each of the sleeps:

      Callable<String> desc() throws InterruptedException {
        return new Callable<String>() {
          @Override
          public String call() throws Exception {
            Thread.sleep(1000);
            return description;
          }
        };
      }

This should enable scheduled tasks, streaming behavior and asynchronous i/o.