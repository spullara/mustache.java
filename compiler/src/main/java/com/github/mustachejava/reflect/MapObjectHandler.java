package com.github.mustachejava.reflect;

import com.github.mustachejava.Binding;
import com.github.mustachejava.Code;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.util.Wrapper;

import java.util.Map;

public class MapObjectHandler extends SimpleObjectHandler {

  @Override
  public Object get(String name, Object scope) {
    if (scope instanceof Map) {
      Map map = (Map) scope;
      if (map.containsKey(name)) {
        return map.get(name);
      }
    }
    return NOT_FOUND;
  }

  @Override
  public Binding createBinding(String name, TemplateContext tc, Code code) {
    Wrapper wrapper = find(name, null);
    return wrapper::call;
  }
}
