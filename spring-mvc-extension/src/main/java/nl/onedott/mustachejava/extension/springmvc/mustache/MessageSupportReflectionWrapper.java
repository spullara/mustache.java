package nl.onedott.mustachejava.extension.springmvc.mustache;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.GuardException;
import nl.onedott.mustachejava.extension.springmvc.SpringMustacheException;
import nl.onedott.mustachejava.extension.springmvc.annotation.Message;
import nl.onedott.mustachejava.extension.springmvc.annotation.Theme;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/**
 * Enables resolving messages using the return values of the method as message codes.
 * Also supports embedded values e.g. {@code "${my.property}"}.
 * @author Bart Tegenbosch
 */
public class MessageSupportReflectionWrapper extends ReflectionWrapper {
    private final MessageSource messageSource;

    public MessageSupportReflectionWrapper(final ReflectionWrapper rw, final MessageSource messageSource) {
        super(rw);
        this.messageSource = messageSource;
    }

    @Override
    public Object call(final Object[] scopes) throws GuardException {
        if (method != null) {
            String code = returnValue(scopes).toString();
            Locale locale = resolveLocale();

            if (messageSource instanceof ApplicationContext) {
                ConfigurableListableBeanFactory beanFactory =
                        (ConfigurableListableBeanFactory) ((ApplicationContext) messageSource)
                                .getAutowireCapableBeanFactory();
                code = beanFactory.resolveEmbeddedValue(code);
            }

            if (method.isAnnotationPresent(Message.class)) {
                try {
                    return messageSource.getMessage(code, getArguments(), locale);
                } catch (NoSuchMessageException e) {
                    throw new SpringMustacheException(e);
                }
            }

            if (method.isAnnotationPresent(Theme.class)) {
                org.springframework.ui.context.Theme theme =
                        RequestContextUtils.getTheme(getRequest());

                if (theme == null) {
                    throw new SpringMustacheException("No theme found");
                }


                return theme.getMessageSource().getMessage(code, getArguments(), locale);
            }
        }
        return super.call(scopes);
    }

    private Object returnValue(Object[] scopes) {
        guardCall(scopes);
        Object scope = oh.coerce(unwrap(scopes));

        try {
            return method.invoke(scope, arguments);
        } catch (IllegalAccessException e) {
            throw new MustacheException(e);
        } catch (InvocationTargetException e) {
            throw new MustacheException(e);
        }
    }

    public Locale resolveLocale() {
        return RequestContextUtils.getLocale(getRequest());
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attributes.getRequest();
    }
}
