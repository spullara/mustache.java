package com.github.mustachejava.impl;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.base.Function;
import com.google.common.util.concurrent.ListeningExecutorService;

import com.github.mustachejava.Code;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.util.LatchedWriter;

/**
* Created by IntelliJ IDEA.
* User: spullara
* Date: 1/9/12
* Time: 2:57 PM
* To change this template use File | Settings | File Templates.
*/
public class IterableCode extends DefaultCode {

  private final String variable;
  private final String file;
  private DefaultMustacheFactory cf;
  private ListeningExecutorService les;

  public IterableCode(DefaultMustacheFactory cf, List<Code> codes, String variable, String sm, String em, String file) {
    super(cf.getObjectHandler(), codes.toArray(new Code[0]), variable, "#", sm, em);
    this.cf = cf;
    this.variable = variable;
    this.file = file;
    les = cf.getListeningExecutorService();
  }

  @Override
  public Writer execute(Writer writer, final Object... scopes) {
    Object resolve = get(variable, scopes);
    if (resolve != null) {
      if (resolve instanceof Function) {
        writer = handleFunction(writer, (Function) resolve, scopes);
      } else if (resolve instanceof Callable) {
        writer = handleCallable(writer, (Callable) resolve, scopes);
      } else {
        writer = execute(writer, resolve, scopes);
      }
    }
    return appendText(writer);
  }

  protected Writer handleCallable(Writer writer, final Callable callable, final Object[] scopes) {
    if (les == null) {
      try {
        writer = execute(writer, callable.call(), scopes);
      } catch (Exception e) {
        throw new MustacheException(e);
      }
    } else {
      final Writer finalWriter = writer;
      final LatchedWriter latchedWriter = new LatchedWriter(writer);
      writer = latchedWriter;
      les.execute(new Runnable() {
        @Override
        public void run() {
          try {
            Object call = callable.call();
            execute(finalWriter, call, scopes);
            latchedWriter.done();
          } catch (Throwable e) {
            latchedWriter.failed(e);
          }
        }
      });
    }
    return writer;
  }

  // Store the templateText locally so it won't fall out of the weak cache until this is gone
  private Set<String> templates = Collections.synchronizedSet(new HashSet<String>());
  
  protected Writer handleFunction(Writer writer, Function function, Object[] scopes) {
    StringWriter sw = new StringWriter();
    runIdentity(sw);
    Object newtemplate = function.apply(sw.toString());
    if (newtemplate != null) {
      String templateText = newtemplate.toString();
      Mustache mustache = cf.getTemplate(templateText);
      if (mustache == null) {
        mustache = cf.compile(new StringReader(templateText), file, sm, em);
        templates.add(templateText);
        cf.putTemplate(templateText, mustache);
      }
      writer = mustache.execute(writer, scopes);
    }
    return writer;
  }

  protected Writer execute(Writer writer, Object resolve, Object[] scopes) {
    for (Iterator i = iterate(resolve); i.hasNext(); ) {
      Object next = i.next();
      Object[] iteratorScopes = scopes;
      if (next != null) {
        iteratorScopes = new Object[scopes.length + 1];
        System.arraycopy(scopes, 0, iteratorScopes, 0, scopes.length);
        iteratorScopes[scopes.length] = next;
      }
      writer = runCodes(writer, iteratorScopes);
    }
    return writer;
  }
}
