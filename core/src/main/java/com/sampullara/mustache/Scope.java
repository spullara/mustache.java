package com.sampullara.mustache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * The scope of the executing Mustache can include an object and a map of strings.  Each scope can also have a
 * parent scope that is checked after nothing is found in the current scope.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 4:14:26 PM
 */
public class Scope extends HashMap<Object, Object> {

  public static final Iterable<Scope> EMPTY = new ArrayList<Scope>(0);
  public static final Object NULL = new Object() {
    public String toString() {
      return "";
    }
  };

  private Object parent;
  private Scope parentScope;
  private static Logger logger = Logger.getLogger(Mustache.class.getName());

  private static ObjectHandler defaultObjectHandler = new DefaultObjectHandler();

  private ObjectHandler objectHandler = defaultObjectHandler;

  public Scope() {
  }

  public Scope(Object parent) {
    if (parent instanceof Scope) {
      this.parentScope = (Scope) parent;
      objectHandler = parentScope.objectHandler;
    } else {
      this.parent = parent;
    }
  }

  public Scope(Scope parentScope) {
    this.parentScope = parentScope;
    objectHandler = parentScope.objectHandler;
  }

  public Scope(Object parent, Scope parentScope) {
    this(parentScope);
    this.parent = parent;
  }

  public static void setDefaultObjectHandler(ObjectHandler defaultObjectHandler) {
    Scope.defaultObjectHandler = defaultObjectHandler;
  }

  public void setObjectHandler(ObjectHandler handleObject) {
    this.objectHandler = handleObject;
  }

  public ObjectHandler getObjectHandler() {
    return objectHandler;
  }

  public Scope getParentScope() {
    return parentScope;
  }

  @Override
  public Object get(Object o) {
    long start = 0;
    if (Mustache.profile) {
      start = System.nanoTime();
    }
    try {
      return get(o, this);
    } finally {
      if (Mustache.profile) {
        long diff = System.nanoTime() - start;
        Average newaverage = new Average();
        Average average = profile.putIfAbsent(o.toString(), newaverage);
        (average == null ? newaverage : average).increment(diff);
      }
    }
  }

  public Object get(Object o, Scope scope) {
    String name = o.toString();
    Object value = null;
    Iterable<String> components = split(name, ".");
    Scope current = this;
    Scope currentScope = scope;
    if (components == null) {
      value = current.localGet(currentScope, name);
      if (value == null || value == NULL) {
        return null;
      }
    } else {
      for (String component : components) {
        value = current.localGet(currentScope, component);
        if (value == null || value == NULL) {
          return null;
        }
        currentScope = current;
        current = new Scope(value);
      }
    }
    return value;
  }

  private static final ConcurrentMap<String, Average> profile = Mustache.profile ? new ConcurrentHashMap<String, Average>() : null;

  private static class Average implements Comparable<Average> {
    private AtomicLong total = new AtomicLong(0);
    private AtomicLong num = new AtomicLong(0);

    public void increment(long increase) {
      num.incrementAndGet();
      total.addAndGet(increase);
    }

    public int compareTo(Average other) {
      double l = other.average() - average();
      return l < 0 ? -1 : l > 0 ? 1 : 0;
    }

    public double average() {
      return total.doubleValue() / num.longValue();
    }
  }

  public static void report() {
    List<Map.Entry<String, Average>> entries = new ArrayList<Map.Entry<String, Average>>(
            profile.entrySet());
    if (entries.size() > 0) {
      logger.info("Top 10 Average");
      Collections.sort(entries, new Comparator<Map.Entry<String, Average>>() {
        @Override
        public int compare(Map.Entry<String, Average> o1, Map.Entry<String, Average> o2) {
          return o1.getValue().compareTo(o2.getValue());
        }
      });
      for (Map.Entry<String, Average> entry : entries.subList(0,
              Math.min(entries.size() - 1, 10))) {
        logger.info(
                entry.getKey() + ": " + entry.getValue().average() + " (" + entry.getValue().total + " / " + entry.getValue().num + ")");
      }
      logger.info("Top 10 Total");
      Collections.sort(entries, new Comparator<Map.Entry<String, Average>>() {
        @Override
        public int compare(Map.Entry<String, Average> o1, Map.Entry<String, Average> o2) {
          long l = o2.getValue().total.longValue() - o1.getValue().total.longValue();
          return l < 0 ? -1 : l > 0 ? 1 : 0;
        }
      });
      for (Map.Entry<String, Average> entry : entries.subList(0, 10)) {
        logger.info(
                entry.getKey() + ": " + entry.getValue().average() + " (" + entry.getValue().total + " / " + entry.getValue().num + ")");
      }
      profile.clear();
    }
  }

  private Object localGet(Scope scope, String name) {
    Object v = super.get(name);
    if (v == null) {
      if (parent != null) {
        v = objectHandler.handleObject(parent, scope, name);
      }
    }
    if (v == null) {
      if (parentScope != null) {
        v = parentScope.get(name, scope);
      }
    }
    return v;
  }

  public Object getParent() {
    return parent;
  }

  private static Iterable<String> split(final String s, final String d) {
    if (!s.contains(d)) return null;
    return new Iterable<String>() {
      public Iterator<String> iterator() {
        return new Iterator<String>() {
          int length = s.length();
          int current = 0;

          public boolean hasNext() {
            return current < length;
          }

          public String next() {
            int start = current;
            int i = s.indexOf(d, start);
            if (i == -1) {
              current = length;
              return s.substring(start);
            } else {
              current = i + d.length();
              return s.substring(start, i);
            }
          }

          public void remove() {
          }
        };
      }
    };
  }

  public String toString() {
    return (size() == 0 ? "" : super.toString()) + (parent == null ? "" : " <- " + parent) + (parentScope == null ? "" : " <- " + parentScope);
  }
}