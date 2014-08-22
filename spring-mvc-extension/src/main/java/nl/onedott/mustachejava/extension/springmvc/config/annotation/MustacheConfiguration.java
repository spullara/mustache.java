/*
 * Copyright (c) 2014, Onedott, bart@onedott.nl
 * All rights reserved.
 */

package nl.onedott.mustachejava.extension.springmvc.config.annotation;

import com.github.mustachejava.MustacheFactory;
import nl.onedott.mustachejava.extension.springmvc.mustache.SpringFeaturedMustacheFactory;
import nl.onedott.mustachejava.extension.springmvc.view.MustacheView;
import nl.onedott.mustachejava.extension.springmvc.view.MustacheViewResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.servlet.ViewResolver;

import java.util.Map;

/**
 * Imported by using the {@link EnableMustache} annotation.
 *
 * @author Bart Tegenbosch
 */
@Configuration
public class MustacheConfiguration implements ImportAware {

    @Autowired
    private ApplicationContext context;

    private Resource resourceLocation;

    @Override
    public void setImportMetadata(final AnnotationMetadata importMetadata) {
        String className = EnableMustache.class.getName();
        setProperties(importMetadata.getAnnotationAttributes(className));
    }

    @Bean
    public MustacheFactory mustacheFactory() throws Exception {
        return new SpringFeaturedMustacheFactory(resourceLocation.getFile());
    }

    @Bean
    public ViewResolver viewResolver() throws Exception {
        MustacheViewResolver viewResolver = new MustacheViewResolver(mustacheFactory());
        viewResolver.setViewClass(MustacheView.class);
        viewResolver.setSuffix(".mustache");
        return viewResolver;
    }

    public void setProperties(final Map<String,Object> attributes) {
        String pattern = (String) attributes.get("value");
        resourceLocation = context.getResource(pattern);
    }
}
