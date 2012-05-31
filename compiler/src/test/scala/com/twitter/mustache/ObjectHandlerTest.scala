package com.twitter.mustache

import com.github.mustachejava.DefaultMustacheFactory
import com.twitter.util.FuturePool
import java.io.{StringWriter, StringReader}
import java.util.concurrent.Executors
import org.junit.{Assert, Test}

class ObjectHandlerTest {
  @Test
  def testHandler() {
    val pool = Executors.newCachedThreadPool()
    val futurePool = FuturePool(pool)
    val mf = new DefaultMustacheFactory()
    mf.setObjectHandler(new TwitterObjectHandler)
    mf.setExecutorService(pool)
    val m = mf.compile(
      new StringReader("{{#list}}{{optionalHello}}, {{futureWorld}}!" +
              "{{#test}}?{{/test}}{{^test}}!{{/test}}{{#num}}?{{/num}}{{^num}}!{{/num}}" +
              "{{#map}}{{value}}{{/map}}\n{{/list}}"),
      "helloworld"
    )
    val sw = new StringWriter
    val writer = m.execute(sw, new {
      val list = Seq(new {
        lazy val optionalHello = Some("Hello")
        val futureWorld = futurePool {
          "world"
        }
        val test = true
        val num = 0
      }, new {
        val optionalHello = Some("Goodbye")
        val futureWorld = futurePool {
          "thanks for all the fish"
        }
        lazy val test = false
        val map = Map(("value", "test"))
        val num = 1
      })
    })
    // You must use close if you use concurrent latched writers
    writer.close()
    Assert.assertEquals("Hello, world!?!\nGoodbye, thanks for all the fish!!?test\n", sw.toString)
  }
}
