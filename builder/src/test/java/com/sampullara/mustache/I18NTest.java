package com.sampullara.mustache;

import com.sampullara.util.FutureWriter;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;

/**
 * Author: Hugo Zhu
 * Date:   2010-10-9 14:44:12
 */
public class I18NTest  extends CompilerTest {
      private File root;
      protected void setUp() throws Exception {
        super.setUp();
        root = new File("src/test/resources");
     }
     public void testChinese() throws MustacheException, IOException, ExecutionException, InterruptedException {
        MustacheCompiler c = new MustacheCompiler(root);
//         c.setDebug();
        Mustache m = c.parseFile("i18n.html");
        c.setOutputDirectory("target/classes");
        StringWriter sw = new StringWriter();
        FutureWriter writer = new FutureWriter(sw);
        m.execute(writer, new Scope(new Object() {
          String name = "Chris";
          int value = 10000;

          int taxed_value() {
            return (int) (this.value - (this.value * 0.4));
          }

          boolean in_ca = true;
        }));
        writer.flush();
        assertEquals(getContents(root, "i18n.txt"), sw.toString());
      }
}
