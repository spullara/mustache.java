/*
 * Copyright (c) 2014, Onedott, bart@onedott.nl
 * All rights reserved.
 */

package nl.onedott.mustachejava.extension.springmvc.view;

import com.github.mustachejava.MustacheFactory;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 *
 * @author Bart Tegenbosch
 */
public class MustacheViewResolver extends AbstractTemplateViewResolver {

    private final MustacheFactory mustacheFactory;

    public MustacheViewResolver(final MustacheFactory mustacheFactory) {
        this.mustacheFactory = mustacheFactory;
    }

    @Override
    protected Class<?> requiredViewClass() {
        return MustacheView.class;
    }

    @Override
    protected AbstractUrlBasedView buildView(final String viewName) throws Exception {
        MustacheView view = (MustacheView) super.buildView(viewName);
        view.setMustacheFactory(mustacheFactory);
        return view;
    }
}
