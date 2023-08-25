package com.github.mustachejava.reflect;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public abstract class BaseObjectHandler extends AbstractObjectHandler {

  protected Field getField(Class aClass, String name) throws NoSuchFieldException {
    Field member;
    try {
      member = aClass.getDeclaredField(name);
    } catch (NoSuchFieldException nsfe) {
      Class superclass = aClass.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return getField(superclass, name);
      }
      throw nsfe;
    }
    checkField(member);
    member.setAccessible(true);
    return member;
  }

  protected Method getMethod(Class<?> aClass, String name, Class<?>... params) throws NoSuchMethodException {
    Method member;
    try {
      member = aClass.getMethod(name, params);
      if (member.getDeclaringClass() == Object.class) {
        throw new NoSuchMethodException();
      }
    } catch (NoSuchMethodException nsme) {
      try {
        member = aClass.getDeclaredMethod(name, params);
      } catch (NoSuchMethodException nsme2) {
        Class superclass = aClass.getSuperclass();
        if (superclass != null && superclass != Object.class) {
          return getMethod(superclass, name);
        }
        throw nsme2;
      }
    }
    checkMethod(member);
    member.setAccessible(true);
    return member;
  }

  protected AccessibleObject findMember(Class sClass, String name) {
    if (String.class == sClass && "value".equals(name)) { // under java11 it would return a wrapper we don't want
      return null;
    }
    if (checkClass(sClass)) {
      // We won't be able to get methods or members on the class directly, so we will look at superclasses and interfaces
      for (Class anInterface : sClass.getInterfaces()) {
        AccessibleObject member = findMember(anInterface, name);
        if (member != null) return member;
      }
      Class superclass = sClass.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        AccessibleObject ao = findMember(superclass, name);
        if (ao != null) return ao;
      }
    }
    return findMemberOnClass(sClass, name);
  }

  private AccessibleObject findMemberOnClass(Class sClass, String name) {
    if (name.isEmpty()) return null;
    AccessibleObject ao;
    try {
      ao = getMethod(sClass, name);
    } catch (NoSuchMethodException e) {
      String propertyname = name.substring(0, 1).toUpperCase() +
              (name.length() > 1 ? name.substring(1) : "");
      try {
        ao = getMethod(sClass, "get" + propertyname);
      } catch (NoSuchMethodException e2) {
        try {
          ao = getMethod(sClass, "is" + propertyname);
        } catch (NoSuchMethodException e3) {
          try {
            ao = getField(sClass, name);
          } catch (NoSuchFieldException e4) {
            ao = null;
          }
        }
      }
    }
    return ao;
  }

  // We default to not allowing private methods
  protected void checkMethod(Method member) throws NoSuchMethodException {
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchMethodException("Only public, protected and package members allowed");
    }
  }

  // We default to not allowing private fields
  protected void checkField(Field member) throws NoSuchFieldException {
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchFieldException("Only public, protected and package members allowed");
    }
  }

  // We default to not allowing private classes
  protected boolean checkClass(Class sClass) {
    return (sClass.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC;
  }
}
