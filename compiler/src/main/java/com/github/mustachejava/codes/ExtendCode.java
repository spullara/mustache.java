package com.github.mustachejava.codes;

import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.TemplateContext;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Extending a template.
 * <p/>
 * User: sam
 * Date: 11/27/11
 * Time: 10:39 AM
 */
public class ExtendCode extends PartialCode {

  private MustacheFactory mf;

  public ExtendCode(TemplateContext tc, DefaultMustacheFactory mf, Mustache codes, String name) throws MustacheException {
    super(tc, mf, codes, "<", name);
    this.mf = mf;
  }

  private Code[] replaceCodes(Code[] supercodes, Map<String, ExtendNameCode> replaceMap) {
    Code[] newcodes = supercodes.clone();
    for (int i = 0; i < supercodes.length; i++) {
      Code code = supercodes[i];
      if (code instanceof ExtendNameCode) {
        ExtendNameCode enc = (ExtendNameCode) code;
        ExtendNameCode extendReplaceCode = replaceMap.get(enc.getName());
        if (extendReplaceCode != null) {
          newcodes[i] = extendReplaceCode;
          extendReplaceCode.appended = enc.appended;
        } else {
          enc.setCodes(replaceCodes(enc.getCodes(), replaceMap));
        }
      } else {
        Code[] codes = code.getCodes();
        if (codes != null) {
          code.setCodes(replaceCodes(codes, replaceMap));
        }
      }
    }
    return newcodes;
  }

  @Override
  public Writer execute(Writer writer, Object[] scopes) throws MustacheException {
    return partialExecute(writer, scopes);
  }

  @Override
  public synchronized void init() {
    Map<String, ExtendNameCode> replaceMap = new HashMap<String, ExtendNameCode>();
    for (Code code : mustache.getCodes()) {
      if (code instanceof ExtendNameCode) {
        // put name codes in the map
        ExtendNameCode erc = (ExtendNameCode) code;
        replaceMap.put(erc.getName(), erc);
        erc.init();
      } else if (code instanceof WriteCode) {
        // ignore text
      } else {
        // fail on everything else
        throw new IllegalArgumentException(
                "Illegal code in extend section: " + code.getClass().getName());
      }
    }
    partial = mf.compile(partialName());
    Code[] supercodes = partial.getCodes();
    // recursively replace named sections with replacements
    partial.setCodes(replaceCodes(supercodes, replaceMap));
  }

}
