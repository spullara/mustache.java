package com.github.mustachejava.reflect;

import com.github.mustachejava.Binding;
import com.github.mustachejava.Code;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.util.Wrapper;
import java.util.List;
import java.util.Map;

public class MapObjectHandler extends AbstractObjectHandler {

  @Override
  public Wrapper find(String name, List<Object> scopes) {
    return scopes1 -> {
      for (int i = scopes1.size() - 1; i >= 0; i--) {
        Object scope = scopes1.get(i);
        if (scope != null) {
          int index = name.indexOf(".");
          if (index == -1) {
            // Special case Maps
            if (scope instanceof Map) {
              Map map = (Map) scope;
              if (map.containsKey(name)) {
                return map.get(name);
              }
            }
          } else {
            // Dig into the dot-notation through recursion
            List<Object> subscope = ObjectHandler.makeList(scope);
            Wrapper wrapper = find(name.substring(0, index), subscope);
            if (wrapper != null) {
              scope = wrapper.call(subscope);
              if (scope == null) {
                continue;
              }
              subscope = ObjectHandler.makeList(scope);
              return find(name.substring(index + 1), ObjectHandler.makeList(subscope)).call(subscope);
            }
          }
        }
      }
      return null;
    };
  }

  @Override
  public Binding createBinding(String name, TemplateContext tc, Code code) {
    Wrapper wrapper = find(name, null);
    return wrapper::call;
  }
}
