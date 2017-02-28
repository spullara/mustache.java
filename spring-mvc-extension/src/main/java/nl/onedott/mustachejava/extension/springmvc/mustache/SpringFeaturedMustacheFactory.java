package nl.onedott.mustachejava.extension.springmvc.mustache;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheVisitor;
import org.springframework.context.NoSuchMessageException;
import org.springframework.ui.context.Theme;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Locale;

/**
 * {@link com.github.mustachejava.MustacheFactory} that is capable of resolving messages.
 *
 * @author Bart Tegenbosch
 */
public class SpringFeaturedMustacheFactory extends DefaultMustacheFactory {

    private LocaleResolver localeResolver = new CookieLocaleResolver();

    public SpringFeaturedMustacheFactory(final File resourceRoot) {
        super(resourceRoot);
    }

    @Override
    public MustacheVisitor createMustacheVisitor() {
        return new SpringFeaturedVisitor(this);
    }

    public void setLocaleResolver(final LocaleResolver localeResolver) {
        Assert.notNull(localeResolver);
        this.localeResolver = localeResolver;
    }

    public String resolveMessage(String code, Object[] args, String defaultValue) {
        try {
            return RequestContextUtils.getWebApplicationContext(getRequest())
                    .getMessage(code, args, defaultValue, resolveLocale());

        } catch (NoSuchMessageException e) {
            throw new MustacheException(e);
        }
    }

    public String resolveThemeMessage(String name) {
        Theme theme = RequestContextUtils.getTheme(getRequest());
        if (theme == null) {
            throw new MustacheException("No theme found");
        }

        try {
            return theme.getMessageSource().getMessage(name, new Object[]{}, resolveLocale());
        } catch (NoSuchMessageException e) {
            throw new MustacheException(e);
        }
    }

    public Locale resolveLocale() {
        Locale locale = null;
        if (localeResolver != null) {
            locale = localeResolver.resolveLocale(getRequest());
        }
        if (locale != null) {
            return locale;
        }
        return Locale.getDefault();
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
}
