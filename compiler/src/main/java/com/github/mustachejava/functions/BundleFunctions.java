package com.github.mustachejava.functions;

import com.github.mustachejava.TemplateFunction;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;

/**
 * Factory for Mustache.java translation functions based on localized Resource bundles.
 *
 * Example with a class:
 * <pre>
 * public class ... {
 *     Function trans = BundleFunctions.newPreTranslate("com.project.locale", Locale.US);
 *     ...
 * }
 * </pre>
 *
 * Example with a map:
 * <pre>
 * Map &lt;String, Object&gt; scopes =  new HashMap%lt;String, Object&gt;();
 * scopes.put("trans", BundleFunctions.newPostTranslateNullableLabel("com.project.locale", Locale.US);
 * ...
 * </pre>
 *
 * Usage in template:
 * {@code {{#trans}}TranslatedLabel1{{/trans}}}
 *
 * @author R.A. Porter
 */
public class BundleFunctions {
  private static abstract class BundleFunc {
    protected final ResourceBundle res;
    protected final boolean returnLabels;

    protected BundleFunc(String bundle, Locale locale, boolean returnLabels) {
      this.res = ResourceBundle.getBundle(bundle, locale);
      this.returnLabels = returnLabels;
    }

    final protected String lookup(String key) {
      if (res.containsKey(key)) {
        return res.getString(key);  // return translation
      } else {
        return returnLabels ? key : null;
      }
    }
  }

  static class PreTranslateFunc extends BundleFunc implements TemplateFunction {
    private PreTranslateFunc(String bundle, Locale locale, boolean returnLabels) {
      super(bundle, locale, returnLabels);
    }

    @Override
    public String apply(String input) {
      return super.lookup(input);
    }
  }

  static class PostTranslateFunc extends BundleFunc implements Function {
    private PostTranslateFunc(String bundle, Locale locale, boolean returnLabels) {
      super(bundle, locale, returnLabels);
    }

    @Override
    public Object apply(Object input) {
      return super.lookup((String) input);
    }
  }

  /**
   * Returns a Function that operates prior to template evaluation and returns unknown keys intact.
   *
   * Given the following HTML:
   * <pre>
   * {{#trans}}Label1{{/trans}}
   * {{#trans}}Label.unknown{{/trans}}
   * {{#trans}}Label.{{replaceMe}}{{/trans}}
   * </pre>
   * and the following properties in the provided bundle:
   * <pre>
   * Label1=hello
   * Label.replaced=world
   * </pre>
   * and a mapping from {@code replaceMe} to the value {@code replaced}, the following output
   * will be generated:
   * <pre>
   * hello
   * Label.unknown
   * Label.replaced
   * </pre>
   *
   * @param bundle name of the resource bundle
   * @param locale translation locale
   * @return Function that operates prior to template evaluation and returns unknown keys intact
   */
  public static Function newPreTranslate(String bundle, Locale locale) {
    return new PreTranslateFunc(bundle, locale, true);
  }

  /**
   * Returns a Function that operates prior to template evaluation and returns nulls for unknown keys.
   *
   * Given the following HTML:
   * <pre>
   * {{#trans}}Label1{{/trans}}
   * {{#trans}}Label.unknown{{/trans}}
   * {{#trans}}Label.{{replaceMe}}{{/trans}}
   * </pre>
   * and the following properties in the provided bundle:
   * <pre>
   * Label1=hello
   * Label.replaced=world
   * </pre>
   * and a mapping from {@code replaceMe} to the value {@code replaced}, the following output
   * will be generated:
   * <pre>
   * hello
   * </pre>
   *
   * @param bundle name of the resource bundle
   * @param locale translation locale
   * @return Function that operates prior to template evaluation and returns nulls for unknown keys
   */
  public static Function newPreTranslateNullableLabel(String bundle, Locale locale) {
    return new PreTranslateFunc(bundle, locale, false);
  }

  /**
   * Returns a Function that operates after template evaluation and returns unknown keys intact.
   *
   * Given the following HTML:
   * <pre>
   * {{#trans}}Label1{{/trans}}
   * {{#trans}}Label.unknown{{/trans}}
   * {{#trans}}Label.{{replaceMe}}{{/trans}}
   * </pre>
   * and the following properties in the provided bundle:
   * <pre>
   * Label1=hello
   * Label.replaced=world
   * </pre>
   * and a mapping from {@code replaceMe} to the value {@code replaced}, the following output
   * will be generated:
   * <pre>
   * hello
   * Label.unknown
   * world
   * </pre>
   *
   * @param bundle name of the resource bundle
   * @param locale translation locale
   * @return Function that operates after template evaluation and returns unknown keys intact
   */
  public static Function newPostTranslate(String bundle, Locale locale) {
    return new PostTranslateFunc(bundle, locale, true);
  }

  /**
   * Returns a Function that operates after template evaluation and returns nulls for unknown keys.
   *
   * Given the following HTML:
   * <pre>
   * {{#trans}}Label1{{/trans}}
   * {{#trans}}Label.unknown{{/trans}}
   * {{#trans}}Label.{{replaceMe}}{{/trans}}
   * </pre>
   * and the following properties in the provided bundle:
   * <pre>
   * Label1=hello
   * Label.replaced=world
   * </pre>
   * and a mapping from {@code replaceMe} to the value {@code replaced}, the following output
   * will be generated:
   * <pre>
   * hello
   * world
   * </pre>
   *
   * @param bundle name of the resource bundle
   * @param locale translation locale
   * @return Function that operates after template evaluation and returns nulls for unknown keys
   */
  public static Function newPostTranslateNullableLabel(String bundle, Locale locale) {
    return new PostTranslateFunc(bundle, locale, false);
  }
}
