package nl.onedott.mustachejava.extension.springmvc.mustache.code;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.codes.ValueCode;
import nl.onedott.mustachejava.extension.springmvc.mustache.SpringFeaturedMustacheFactory;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Bart Tegenbosch
 */
public class MessageSourceCode extends ValueCode {

    private final Object[] args;

    public MessageSourceCode(final TemplateContext tc, final DefaultMustacheFactory df, final String variable, final boolean encoded) {
        super(tc, df, variable, encoded);

        //TODO: extract arguments from variable?
        this.args = new Object[]{};
    }

    @Override
    public Writer execute(final Writer writer, final Object[] scopes) {
        SpringFeaturedMustacheFactory factory = (SpringFeaturedMustacheFactory) df;

        try {
            writer.write(factory.resolveMessage(this.name, args, this.name));
        } catch (IOException e) {
            throw new MustacheException(e);
        }
        return super.execute(writer, scopes);
    }
}
