package com.twitter.mustache

import com.github.mustachejava.DefaultMustacheFactory
import com.twitter.util.{Future, FuturePool}
import java.io.{StringWriter, StringReader}
import java.util.concurrent.{Callable, Executors}
import org.junit.{Assert, Test}

class ObjectHandlerTest {

  @Test
  def testMap() {
    val mf = new DefaultMustacheFactory()
    mf.setObjectHandler(new ScalaObjectHandler)
    val m = mf.compile(
      new StringReader("{{#map}}{{test}}{{test2}}{{/map}}"),
      "helloworld"
    )
    val sw = new StringWriter
    val w = m.execute(sw, Map( "map" -> Map( "test" -> "fred" ) ) ).close()
    Assert.assertEquals("fred", sw.toString())
  }

  @Test
  def testScalaHandler() {
    val pool = Executors.newCachedThreadPool()
    val mf = new DefaultMustacheFactory()
    mf.setObjectHandler(new ScalaObjectHandler)
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
        val futureWorld = new Callable[String] {
          def call(): String = "world"
        }
        val test = true
        val num = 0
      }, new {
        val optionalHello = Some("Goodbye")
        val futureWorld = new Callable[String] {
          def call(): String = "thanks for all the fish"
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

  @Test
  def testScalaStream() {
    val pool = Executors.newCachedThreadPool()
    val mf = new DefaultMustacheFactory()
    mf.setObjectHandler(new ScalaObjectHandler)
    mf.setExecutorService(pool)
    val m = mf.compile(new StringReader("{{#stream}}{{value}}{{/stream}}"), "helloworld")
    val sw = new StringWriter
    val writer = m.execute(sw, new {
      val stream = Stream(
        new { val value = "hello" },
        new { val value = "world" })
    })
    writer.close()
    Assert.assertEquals("helloworld", sw.toString)
  }

  @Test
  def testUnit() {
    val mf = new DefaultMustacheFactory()
    mf.setObjectHandler(new ScalaObjectHandler)
    val m = mf.compile(new StringReader("{{test}}"), "unit")
    val sw = new StringWriter
    m.execute(sw, new {
      val test = if (false) "test"
    }).close()
    Assert.assertEquals("", sw.toString)
  }

  @Test
  def testOptions() {
    val mf = new DefaultMustacheFactory()
    mf.setObjectHandler(new ScalaObjectHandler)
    val m = mf.compile(new StringReader("{{foo}}{{bar}}"), "unit")
    val sw = new StringWriter
    m.execute(sw, new {
      val foo = Some("Hello")
      val bar = None
    }).close()
    Assert.assertEquals("Hello", sw.toString)
  }
}
