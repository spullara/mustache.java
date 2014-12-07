package com.github.mustachejava.util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * Exposes first / last / index / value on each element.
 */
public class DecoratedCollection<T> extends AbstractCollection<Element<T>> {

  private final Collection<T> c;

  public DecoratedCollection(Collection<T> c) {
    this.c = c;
  }

  @Override
  public Iterator<Element<T>> iterator() {
    final Iterator<T> iterator = c.iterator();
    return new Iterator<Element<T>>() {
      int index = 0;

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Element<T> next() {
        T next = iterator.next();
        int current = index++;
        return new Element<>(current, current == 0, !iterator.hasNext(), next);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public int size() {
    return c.size();
  }
}

class Element<T> {
  public final int index;
  public final boolean first;
  public final boolean last;
  public final T value;

  public Element(int index, boolean first, boolean last, T value) {
    this.index = index;
    this.first = first;
    this.last = last;
    this.value = value;
  }
}

