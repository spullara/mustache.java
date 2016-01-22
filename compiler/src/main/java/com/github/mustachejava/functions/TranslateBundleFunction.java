package com.github.mustachejava.functions;

import com.github.mustachejava.TemplateFunction;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Mustache.java translation function based on localized ResourceBundles.
 *
 * Usage code with a class:
 *   public class ... {
 *   	TemplateFunction trans = new TranslateBundleFunction("com.project.locale", Locale.US);
 *   	...
 *   }
 *
 * Usage code with a Map:
 *   HashMap&lt;String, Object&gt; scopes = new HashMap&lt;String, Object&gt;();
 *   scopes.put("trans", new TranslateBundleFunction("com.project.locale", Locale.US));
 *   ...
 *
 * Usage in template:
 *   ... {{#trans}}TranslatedLabel1{{/trans}} ...
 *
 *   ... {{#trans}}TranslatedLabel2 param1=newparam1 param2=newparma2{{/trans}}
 * @author gw0 [http://gw.tnode.com/] &lt;gw.2012@tnode.com&gt;
 */
public class TranslateBundleFunction implements TemplateFunction {

	private ResourceBundle res;
	
	/**
   * Constructor for a Mustache.java translation function.
	 * 
	 * @param bundle resource bundle name
	 * @param locale translation locale
	 */
	public TranslateBundleFunction(String bundle, Locale locale) {
		this.res = ResourceBundle.getBundle(bundle, locale);
	}
	
	/** Return translation from the localized ResourceBundle. */
	@Override
	public String apply(String input) {
 		String[] inputWithParams = input.split(" ");
 		String key = inputWithParams[0];
		if(!res.containsKey(key)) {
			return input;  // return untranslated label
		}
		String translatedValue = res.getString(key);
		for (int i = 1; i < inputWithParams.length; i++) {
			String[] splitParam = inputWithParams[i].split("=");
			String oldTag = splitParam[0];
			String newTag = splitParam[1];
			translatedValue = translatedValue.replace("{{" + oldTag + "}}", "{{" + newTag + "}}");
		}
		return translatedValue;
	}
}
