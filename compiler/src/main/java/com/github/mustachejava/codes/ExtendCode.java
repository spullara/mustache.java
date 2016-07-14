package com.github.mustachejava.codes;

import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.TemplateContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extending a template through in-place replacement of the overridden codes.
 * 
 * User: sam
 * Date: 11/27/11
 * Time: 10:39 AM
 */
public class ExtendCode extends PartialCode {

  private final DefaultMustacheFactory mf;

  public ExtendCode(TemplateContext tc, DefaultMustacheFactory mf, Mustache codes, String name) throws MustacheException {
    super(tc, mf, codes, "<", name);
    this.mf = mf;
  }

  private Code[] replaceCodes(Code[] supercodes, Map<String, ExtendNameCode> replaceMap, Set<Code> seen) {
    Code[] newcodes = supercodes.clone();
    for (int i = 0; i < supercodes.length; i++) {
      Code code = supercodes[i];
      if (seen.add(code)) {
        if (code instanceof ExtendNameCode) {
          ExtendNameCode enc = (ExtendNameCode) code;
          ExtendNameCode extendReplaceCode = replaceMap.get(enc.getName());
          if (extendReplaceCode != null) {
            ExtendNameCode newcode = (ExtendNameCode) (newcodes[i] = (Code) extendReplaceCode.clone());
            // We need to set the appended text of the new code to that of the old code
            newcode.appended = enc.appended;
          } else {
            enc.setCodes(replaceCodes(enc.getCodes(), replaceMap, seen));
          }
        } else {
          Code[] codes = code.getCodes();
          if (codes != null) {
            code.setCodes(replaceCodes(codes, replaceMap, seen));
          }
        }
        seen.remove(code);
      }
    }
    return newcodes;
  }

  @SuppressWarnings("StatementWithEmptyBody")
  @Override
  public synchronized void init() {
    filterText();
    Map<String, ExtendNameCode> replaceMap = new HashMap<>();
    for (Code code : mustache.getCodes()) {
      if (code instanceof ExtendNameCode) {
        // put name codes in the map
        ExtendNameCode erc = (ExtendNameCode) code;
        replaceMap.put(erc.getName(), erc);
        erc.init();
      } else if ((code instanceof WriteCode) || (code instanceof CommentCode)) {
        // ignore text and comments
      } else {
        // fail on everything else
        throw new IllegalArgumentException(
                "Illegal code in extend section: " + code.getClass().getName());
      }
    }
    Mustache original = mf.compilePartial(partialName());
    partial = (Mustache) original.clone();
    Code[] supercodes = partial.getCodes();
    // recursively replace named sections with replacements
    Set<Code> seen = new HashSet<>();
    seen.add(partial);
    partial.setCodes(replaceCodes(supercodes, replaceMap, seen));
  }

}
