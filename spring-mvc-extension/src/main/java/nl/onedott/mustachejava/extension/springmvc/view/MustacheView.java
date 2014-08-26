package nl.onedott.mustachejava.extension.springmvc.view;

import com.github.mustachejava.MustacheFactory;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.AbstractTemplateView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author Bart Tegenbosch
 */
public class MustacheView extends AbstractTemplateView {

    private MustacheFactory mustacheFactory;

    @Override
    protected void renderMergedTemplateModel(final Map<String, Object> model, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        mustacheFactory.compile(getUrl()).execute(response.getWriter(), model);
    }

    public void setMustacheFactory(final MustacheFactory mustacheFactory) {
        Assert.notNull(mustacheFactory, "mustacheFactory cannot be null");
        this.mustacheFactory = mustacheFactory;
    }
}
