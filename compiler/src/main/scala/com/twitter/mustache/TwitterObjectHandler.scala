package com.twitter.mustache

import com.twitter.util.Future
import java.io.Writer
import com.github.mustachejava.reflect.ReflectionObjectHandler
import com.github.mustachejava.Iteration
import java.util.concurrent.Callable
import java.lang.reflect.{Method, Field}

class TwitterObjectHandler extends ReflectionObjectHandler {

  // Allow any method or field
  override def checkMethod(member: Method) {}
  override def checkField(member: Field) {}

  override def find(name: String, scopes: Array[ AnyRef ]) = {
    val wrapper = super.find(name, scopes)
    if (wrapper == null) {
      null
    } else {
      wrapper
    }
  }

  override def coerce(value: Object) = {
    value match {
      case o: Option[ _ ] => o match {
        case Some(some: Object) => coerce(some)
        case None => null
      }
      case f: Future[ _ ] => {
        new Callable[Any]() {
          def call() = {
            val value = f.get().asInstanceOf[ Object ]
            coerce(value)
          }
        }
      }
      case _ => value
    }
  }

  override def iterate(iteration: Iteration, writer: Writer, value: Object, scopes: Array[ Object ]) = {
    value match {
      case t: Traversable[ AnyRef ] => {
        var newWriter = writer
        t map {
          next =>
            newWriter = iteration.next(newWriter, coerce(next), scopes)
        }
        newWriter
      }
      case _ => super.iterate(iteration, writer, value, scopes)
    }
  }

  override def falsey(iteration: Iteration, writer: Writer, value: Object, scopes: Array[ Object ]) = {
    value match {
      case t: Traversable[ AnyRef ] => {
        if (t.isEmpty) {
          iteration.next(writer, value, scopes)
        } else {
          writer
        }
      }
      case _ => super.falsey(iteration, writer, value, scopes)
    }
  }
}
