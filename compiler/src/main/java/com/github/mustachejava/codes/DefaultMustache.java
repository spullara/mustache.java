package com.github.mustachejava.codes;

import com.github.mustachejava.Code;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.asm.CodeCompiler;
import com.github.mustachejava.asm.CompiledCodes;
import org.objectweb.asm.Opcodes;

import java.io.Writer;

/**
 * Default Mustache
 */
public class DefaultMustache extends DefaultCode implements Mustache, Opcodes {
  private static boolean compile = Boolean.getBoolean("mustache.compile");

  private Code[] codes;
  private boolean inited = false;

  public DefaultMustache(TemplateContext tc, MustacheFactory cf, Code[] codes, String name) {
    super(tc, cf.getObjectHandler(), null, name, null);
    setCodes(codes);
  }

  @Override
  public Code[] getCodes() {
    return codes;
  }

  private CompiledCodes compiledCodes = null;

  public Writer run(Writer writer, Object[] scopes) {
    if (compiledCodes == null) {
      if (codes != null) {
        for (Code code : codes) {
          writer = code.execute(writer, scopes);
        }
      }
      return writer;
    } else {
      return compiledCodes.runCodes(writer, scopes);
    }
  }

  @Override
  public final void setCodes(Code[] newcodes) {
    codes = newcodes;
    if (!compile || codes == null) {
      compiledCodes = null;
    } else {
      compiledCodes = CodeCompiler.compile(codes, newcodes);
    }
  }

  @Override
  public void identity(Writer writer) {
    // No self output at the top level
    runIdentity(writer);
  }

  @Override
  public synchronized void init() {
    if (!inited) {
      inited = true;
      super.init();
    }
  }
}
