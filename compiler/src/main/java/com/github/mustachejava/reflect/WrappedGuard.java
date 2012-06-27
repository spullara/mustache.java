package com.github.mustachejava.reflect;

import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.util.Wrapper;
import com.google.common.base.Predicate;

import javax.annotation.Nullable;
import java.util.List;

import static com.github.mustachejava.reflect.ReflectionObjectHandler.unwrap;

/**
 * Dig into the dot notation to guard it from changing.
 * <p/>
 * User: sam
 * Date: 6/26/12
 * Time: 9:09 PM
 */
public class WrappedGuard implements Predicate<Object[]> {
  private final ObjectHandler oh;
  private final int index;
  private final Wrapper[] wrappers;
  private final List<Predicate<Object[]>> wrapperGuard;

  public WrappedGuard(ObjectHandler oh, int index, List<Wrapper> wrappers, List<Predicate<Object[]>> wrapperGuard) {
    this.oh = oh;
    this.index = index;
    this.wrappers = wrappers.toArray(new Wrapper[wrappers.size()]);
    this.wrapperGuard = wrapperGuard;
  }

  @Override
  public boolean apply(@Nullable Object[] objects) {
    Object scope = unwrap(oh, index, wrappers, objects);
    for (Predicate<Object[]> predicate : wrapperGuard) {
      if (!predicate.apply(new Object[] { scope })) {
         return false;
      }
    }
    return true;
  }
}
