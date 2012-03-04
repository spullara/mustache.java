package com.sampullara.util;

import java.util.concurrent.Callable;

import com.google.common.util.concurrent.AbstractFuture;

/**
 * I know you want this done later, but I might as well do it right now.
 */
public class ImmediateFuture<T> extends AbstractFuture<T> {
  public ImmediateFuture(Callable<T> task) {
    try {
      set(task.call());
    } catch (Exception e) {
      setException(e);
    }
  }
}
