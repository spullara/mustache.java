package nl.onedott.mustachejava.extension.springmvc.mustache;

import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.Wrapper;
import nl.onedott.mustachejava.extension.springmvc.annotation.Message;
import nl.onedott.mustachejava.extension.springmvc.annotation.Theme;
import org.springframework.context.MessageSource;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * Enables wrapper with message support.
 * @author Bart Tegenbosch
 */
public class MessageSupportReflectionObjectHandler extends ReflectionObjectHandler {

    private final MessageSource messageSource;

    public MessageSupportReflectionObjectHandler(final MessageSource messageSource) {
        Assert.notNull(messageSource);
        this.messageSource = messageSource;
    }

    @Override
    public Wrapper find(final String name, final Object[] scopes) {
        Wrapper wrapper = super.find(name, scopes);
        if (wrapper instanceof ReflectionWrapper) {
            ReflectionWrapper w = (ReflectionWrapper) wrapper;
            Method method = w.getMethod();

            if (method.isAnnotationPresent(Message.class) || method.isAnnotationPresent(Theme.class)) {
                wrapper = new MessageSupportReflectionWrapper(w, messageSource);
            }
        }

        return wrapper;
    }
}
