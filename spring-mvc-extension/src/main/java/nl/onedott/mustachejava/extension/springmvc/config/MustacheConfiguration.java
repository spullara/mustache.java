package nl.onedott.mustachejava.extension.springmvc.config;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import nl.onedott.mustachejava.extension.springmvc.annotation.EnableMustache;
import nl.onedott.mustachejava.extension.springmvc.mustache.MessageSupportReflectionObjectHandler;
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
 * Imported by using the {@link nl.onedott.mustachejava.extension.springmvc.annotation.EnableMustache} annotation.
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
        DefaultMustacheFactory factory = new DefaultMustacheFactory(resourceLocation.getFile());
        factory.setObjectHandler(new MessageSupportReflectionObjectHandler(context));
        return factory;
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
