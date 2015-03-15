package com.github.mustachejava.reflect.guards;

import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.reflect.Guard;
import com.github.mustachejava.util.Wrapper;

import java.util.List;

import static com.github.mustachejava.ObjectHandler.makeList;
import static com.github.mustachejava.reflect.ReflectionObjectHandler.unwrap;

/**
 * Dig into the dot notation to guard it from changing.
 *
 * User: sam
 * Date: 6/26/12
 * Time: 9:09 PM
 */
public class WrappedGuard implements Guard {
  protected final ObjectHandler oh;
  protected final int index;
  protected final Wrapper[] wrappers;
  private final List<Guard> wrapperGuard;

  public WrappedGuard(ObjectHandler oh, int index, List<Wrapper> wrappers, List<Guard> wrapperGuard) {
    this.oh = oh;
    this.index = index;
    this.wrappers = wrappers.toArray(new Wrapper[wrappers.size()]);
    this.wrapperGuard = wrapperGuard;
  }

  @Override
  public boolean apply(List<Object> objects) {
    Object scope = unwrap(oh, index, wrappers, objects);
    for (Guard predicate : wrapperGuard) {
      if (!predicate.apply(makeList(scope))) {
        return false;
      }
    }
    return true;
  }

  public String toString() {
    return "[WrappedGuard: " + index + " " + wrapperGuard + "]";
  }

}
