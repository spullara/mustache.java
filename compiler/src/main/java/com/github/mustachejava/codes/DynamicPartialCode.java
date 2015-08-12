package com.github.mustachejava.codes;

import com.github.mustachejava.*;

import java.io.IOException;
import java.io.Writer;

public class DynamicPartialCode extends DefaultCode {
  protected final String extension;
  protected final String dir;
  protected Mustache partial;
  protected int recrusionLimit;

  protected DynamicPartialCode(TemplateContext tc, DefaultMustacheFactory df, Mustache mustache, String type, String variable) {
    super(tc, df, mustache, variable, type);
    // Use the  name of the parent to get the name of the partial
    String file = tc.file();
    int dotindex = file.lastIndexOf(".");
    extension = dotindex == -1 ? "" : file.substring(dotindex);
    int slashindex = file.lastIndexOf("/");
    dir = file.substring(0, slashindex + 1);
    recrusionLimit = df.getRecursionLimit();
  }

  public DynamicPartialCode(TemplateContext tc, DefaultMustacheFactory cf, String variable) {
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
      throw new MustacheException(e);
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
  public Writer execute(Writer writer, final Object[] scopes) {
    DepthLimitedWriter depthLimitedWriter;
    if (writer instanceof DepthLimitedWriter) {
      depthLimitedWriter = (DepthLimitedWriter) writer;
    } else {
      depthLimitedWriter = new DepthLimitedWriter(writer);
    }
    if (depthLimitedWriter.incr() > recrusionLimit) {
      throw new MustacheException("Maximum partial recursion limit reached: " + recrusionLimit);
    }
    try{
       if(get(scopes)!=null)
          partial = df.compilePartial(get(scopes) +".html");
          }catch(Exception e){
        throw new MustacheException("Failed to compile partial: " + name);
      }
    if (partial == null) {
        throw new MustacheException("Failed to compile partial: " + name);
       }
    Writer execute = partial.execute(depthLimitedWriter, scopes);
    depthLimitedWriter.decr();
    return appendText(execute);
  }

  @Override
  public synchronized void init() {
    filterText();
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
