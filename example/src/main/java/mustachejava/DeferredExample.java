package mustachejava;

import com.github.mustachejava.DeferringMustacheFactory;
import com.github.mustachejava.Mustache;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Executors;

/**
 * This show how to use the deferred mustache factory.
 *
 * User: sam
 * Date: 7/7/12
 * Time: 12:32 PM
 */
public class DeferredExample {

  Object deferredpartial = DeferringMustacheFactory.DEFERRED;
  Object deferred = new DeferringMustacheFactory.DeferredCallable();
  boolean wait1second() throws InterruptedException {
    Thread.sleep(1000);
    return true;
  }

  public static void main(String[] args) throws IOException {
    DeferringMustacheFactory mf = new DeferringMustacheFactory();
    mf.setExecutorService(Executors.newCachedThreadPool());
    Mustache m = mf.compile("deferring.mustache");
    PrintWriter pw = new PrintWriter(System.out);
    m.execute(pw, new DeferredExample()).close();
  }
}
