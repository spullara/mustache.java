package com.github.mustachejava;

/**
 * This factory is similar to DefaultMustacheFactory but handles whitespace according to the mustache specification.
 * Therefore the rendering is less performant than with the DefaultMustacheFactory.
 */
public class SpecMustacheFactory extends DefaultMustacheFactory {
    @Override
    public MustacheVisitor createMustacheVisitor() {
        return new SpecMustacheVisitor(this);
    }
}
