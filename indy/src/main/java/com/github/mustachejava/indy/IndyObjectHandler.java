package com.github.mustachejava.indy;

import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.Wrapper;
import org.objectweb.asm.Opcodes;

/**
 * Creates custom classes instead of using reflection for handling objects. Leverages
 * the ReflectionObjectHandler to create the original wrappers and converts them to
 * new versions.
 */
public class IndyObjectHandler extends ReflectionObjectHandler implements Opcodes {

  @Override
  public Wrapper find(String name, Object[] scopes) {
    ReflectionWrapper rw = (ReflectionWrapper) super.find(name, scopes);
    return rw == null ? null : IndyWrapper.create(rw);
  }
}
