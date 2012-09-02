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
 *   HashMap<String, Object> scopes = new HashMap<String, Object>();
 *   scopes.put("trans", new TranslateBundleFunction("com.project.locale", Locale.US));
 *   ...
 *
 * Usage in template:
 *   ... {{#trans}}TranslatedLabel1{{/trans}} ...
 *
 * @author gw0 [http://gw.tnode.com/] <gw.2012@tnode.com>
 */
public class TranslateBundleFunction implements TemplateFunction {

	private ResourceBundle res;
	
	/** Constructor for a Mustache.java translation function.
	 * 
	 * @param bundle resource bundle name
	 * @param locale translation locale
	 */
	TranslateBundleFunction(String bundle, Locale locale) {
		this.res = ResourceBundle.getBundle(bundle, locale);
	}
	
	/** Return translation from the localized ResourceBundle. */
	@Override
	public String apply(String input) {
		if(res.containsKey(input)) {
			return res.getString(input);  // return translation
			
		} else {
			return input;  // return untranslated label
		}
	}
}
