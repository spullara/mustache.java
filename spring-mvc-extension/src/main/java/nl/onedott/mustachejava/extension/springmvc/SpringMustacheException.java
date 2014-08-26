package nl.onedott.mustachejava.extension.springmvc;

import com.github.mustachejava.MustacheException;

/**
 * @author Bart Tegenbosch
 */
public class SpringMustacheException extends MustacheException {
    public SpringMustacheException(final String message) {
        super(message);
    }

    public SpringMustacheException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SpringMustacheException(final Throwable cause) {
        super(cause);
    }
}
