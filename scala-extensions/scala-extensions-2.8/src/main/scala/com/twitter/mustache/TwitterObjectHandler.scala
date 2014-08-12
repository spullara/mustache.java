package com.twitter.mustache

import com.twitter.util.Future
import java.util.concurrent.Callable

class TwitterObjectHandler extends ScalaObjectHandler {

  override def coerce(value: Object) = {
    value match {
      case f: Future[_] => {
        new Callable[Any]() {
          def call() = {
            val value = f.get().asInstanceOf[Object]
            coerce(value)
          }
        }
      }
      case _ => super.coerce(value)
    }
  }
}
