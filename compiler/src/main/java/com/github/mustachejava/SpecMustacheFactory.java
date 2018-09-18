package com.github.mustachejava;

import com.github.mustachejava.resolver.DefaultResolver;

import java.io.File;

/**
 * This factory is similar to DefaultMustacheFactory but handles whitespace according to the mustache specification.
 * Therefore the rendering is less performant than with the DefaultMustacheFactory.
 */
public class SpecMustacheFactory extends DefaultMustacheFactory {
    @Override
    public MustacheVisitor createMustacheVisitor() {
        return new SpecMustacheVisitor(this);
    }

    public SpecMustacheFactory() {
        super();
    }

    public SpecMustacheFactory(MustacheResolver mustacheResolver) {
        super(mustacheResolver);
    }

    /**
     * Use the classpath to resolve mustache templates.
     *
     * @param classpathResourceRoot the location in the resources where templates are stored
     */
    public SpecMustacheFactory(String classpathResourceRoot) {
        super(classpathResourceRoot);
    }

    /**
     * Use the file system to resolve mustache templates.
     *
     * @param fileRoot the root of the file system where templates are stored
     */
    public SpecMustacheFactory(File fileRoot) {
        super(fileRoot);
    }
}
