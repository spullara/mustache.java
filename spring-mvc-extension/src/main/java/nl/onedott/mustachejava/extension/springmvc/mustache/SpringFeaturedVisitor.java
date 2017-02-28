package nl.onedott.mustachejava.extension.springmvc.mustache;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.DefaultMustacheVisitor;
import com.github.mustachejava.TemplateContext;
import nl.onedott.mustachejava.extension.springmvc.mustache.code.MessageSourceCode;
import nl.onedott.mustachejava.extension.springmvc.mustache.code.ThemeCode;

/**
 * @author Bart Tegenbosch
 */
public class SpringFeaturedVisitor extends DefaultMustacheVisitor {

    private static final String MESSAGE_PREFIX = "message:";
    private static final String THEME_PREFIX = "theme:";

    public SpringFeaturedVisitor(final DefaultMustacheFactory df) {
        super(df);
    }

    /**
     * Recognize additional code types that integrate with Spring MVC.
     */
    @Override
    public void value(final TemplateContext tc, final String variable, final boolean encoded) {
        if (variable.startsWith(MESSAGE_PREFIX)) {
            messageSourceValue(tc, variable.substring(MESSAGE_PREFIX.length()), encoded);

        } else if (variable.startsWith(THEME_PREFIX)) {
            themeValue(tc, variable.substring(THEME_PREFIX.length()), encoded);

        } else {
            super.value(tc, variable, encoded);
        }
    }

    public void messageSourceValue(final TemplateContext tc, final String variable, final boolean encoded) {
        list.add(new MessageSourceCode(tc, df, variable, encoded));
    }

    public void themeValue(final TemplateContext tc, final String variable, final boolean encoded) {
        list.add(new ThemeCode(tc, df, variable, encoded));
    }
}
