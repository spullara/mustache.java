package com.sampullara.util;

import com.google.common.util.concurrent.AbstractFuture;

import java.util.concurrent.Callable;

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
