package com.sampullara.mustache.json;

import java.util.Iterator;

import com.sampullara.mustache.DefaultObjectHandler;
import com.sampullara.mustache.Scope;
import org.codehaus.jackson.JsonNode;

/**
 * Handles Jackson JsonNodes
 * <p/>
 * User: spullara
 * Date: 11/22/11
 * Time: 6:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class JsonObjectHandler extends DefaultObjectHandler {
  @Override
  public Iterator iterate(Object object) {
    if (object instanceof JsonNode) {
      JsonNode jn = (JsonNode) object;
      if (jn.isArray()) {
        return jn.iterator();
      }
      return new SingleValueIterator(jn);
    }
    return super.iterate(object);
  }

  @Override
  public Object handleObject(Object parent, Scope scope, String name) {
    if (parent instanceof JsonNode) {
      JsonNode jn = (JsonNode) parent;
      JsonNode result = jn.get(name);
      if (result == null || result.isNull()) return null;
      if (result.isTextual()) {
        return result.getTextValue();
      } else if (result.isBoolean()) {
        return result.getBooleanValue();
      } else {
        return result;
      }
    }
    return super.handleObject(parent, scope, name);
  }
}