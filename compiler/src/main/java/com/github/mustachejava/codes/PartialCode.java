package com.github.mustachejava.codes;

import com.github.mustachejava.*;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class PartialCode extends DefaultCode {
  protected final String extension;
  protected final String dir;
  protected Mustache partial;
  protected int recrusionLimit;
  protected boolean isRecursive;

  protected PartialCode(TemplateContext tc, DefaultMustacheFactory df, Mustache mustache, String type, String variable) {
    super(tc, df, mustache, variable, type);

    // Use the  name of the parent to get the name of the partial
    String file = tc.file();
    int dotindex = file.lastIndexOf(".");
    extension = dotindex == -1 ? "" : file.substring(dotindex);
    int slashindex = file.lastIndexOf("/");
    dir = file.substring(0, slashindex + 1);
    recrusionLimit = df.getRecursionLimit();
  }

  public PartialCode(TemplateContext tc, DefaultMustacheFactory cf, String variable) {
    this(tc, cf, null, ">", variable);
  }

  @Override
  public void identity(Writer writer) {
    try {
      if (name != null) {
        super.tag(writer, type);
      }
      appendText(writer);
    } catch (IOException e) {
      throw new MustacheException(e, tc);
    }
  }

  @Override
  public Code[] getCodes() {
    return partial == null ? null : partial.getCodes();
  }

  @Override
  public void setCodes(Code[] newcodes) {
    partial.setCodes(newcodes);
  }

  @Override
  public Writer execute(Writer writer, final List<Object> scopes) {
    DepthLimitedWriter depthLimitedWriter = null;
    // If the mustache wasn't found to recurse at compilation time we
    // don't need to track the recursion depth and therefore don't need
    // to create and use the DepthLimitedWriter. Another allocation slain.
    if (isRecursive) {
      if (writer instanceof DepthLimitedWriter) {
        depthLimitedWriter = (DepthLimitedWriter) writer;
      } else {
        depthLimitedWriter = new DepthLimitedWriter(writer);
      }
      if (depthLimitedWriter.incr() > recrusionLimit) {
        throw new MustacheException("Maximum partial recursion limit reached: " + recrusionLimit, tc);
      }
      writer = depthLimitedWriter;
    }
    Writer execute = executePartial(writer, scopes);
    if (isRecursive) {
      assert depthLimitedWriter != null;
      depthLimitedWriter.decr();
    }
    return appendText(execute);
  }

  protected Writer executePartial(Writer writer, final List<Object> scopes) {
    return partial.execute(writer, scopes);
  }

  @Override
  public synchronized void init() {
    filterText();
    partial = df.compilePartial(partialName());
    if (partial instanceof DefaultMustache && ((DefaultMustache)partial).isRecursive()) {
      isRecursive = true;
    }
    if (partial == null) {
      throw new MustacheException("Failed to compile partial: " + name, tc);
    }
  }

  /**
   * Builds the file name to be included by this partial tag. Default implementation ppends the tag contents with
   * the current file's extension.
   *
   * @return The filename to be included by this partial tag
   */
  protected String partialName() {
    return df.resolvePartialPath(dir, name, extension);
  }

}
