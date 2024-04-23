package com.github.mustachejava.resolver;

import com.github.mustachejava.MustacheResolver;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * MustacheResolver implementation that resolves mustache files from the classpath.
 */
public class ClasspathResolver implements MustacheResolver {

    private final String resourceRoot;

    public ClasspathResolver() {
        this.resourceRoot = null;
    }

    /**
     * Use the classpath to resolve mustache templates.
     *
     * @param resourceRoot where to find the templates
     */
    public ClasspathResolver(String resourceRoot) {
        this.resourceRoot = resourceRoot;
    }

    @Override
    public Reader getReader(String resourceName) {
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();

        String fullResourceName = concatResourceRootAndResourceName(resourceName);
        String normalizeResourceName = URI.create(fullResourceName).normalize().getPath();

        URL resource = ccl.getResource(normalizeResourceName);
        if (resource != null)
            if (resource.getProtocol().equals("jar")) {
                if (normalizeResourceName.endsWith("/")) {
                    // This is a directory
                    return null;
                } else if (ccl.getResource(normalizeResourceName + "/") != null) {
                    // This is a directory
                    return null;
                }
            } else if (resource.getProtocol().equals("file")) {
                if (new File(resource.getPath()).isDirectory()) {
                    // This is a directory
                    return null;
                }
            }
        else
            resource = ClasspathResolver.class.getClassLoader().getResource(normalizeResourceName);

        if (resource != null) {
            try {
                return new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    private String concatResourceRootAndResourceName(String resourceName) {
        if ((resourceRoot == null) || (resourceName == null)) {
            return resourceName;
        } else {
            //Ensure there is only one (and only one) forward slash between the resourceRoot and resourceName paths
            if (resourceName.startsWith("/") && resourceRoot.endsWith("/")) {
                return resourceRoot.substring(0, resourceRoot.length() - 1) + resourceName;
            } else if ((resourceName.startsWith("/") && !resourceRoot.endsWith("/")) || (!resourceName.startsWith("/") && resourceRoot.endsWith("/"))) {
                return resourceRoot + resourceName;
            } else {
                return resourceRoot + "/" + resourceName;
            }
        }
    }
}
