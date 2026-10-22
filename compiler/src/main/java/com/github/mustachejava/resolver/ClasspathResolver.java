package com.github.mustachejava.resolver;

import com.github.mustachejava.MustacheResolver;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;

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
        if (resource == null) {
            resource = ClasspathResolver.class.getClassLoader().getResource(normalizeResourceName);
        }


        if (resource != null) {
            try {
                if (resource.getProtocol().equals("jar") && normalizeResourceName.endsWith("/")) {
                    return null;
                }
                if (resource.getProtocol().equals("file") && new File(resource.getPath()).isDirectory()) {
                    return null;
                }
                URLConnection connection = resource.openConnection();
                if (connection instanceof JarURLConnection) {
                    JarEntry entry = ((JarURLConnection) connection).getJarEntry();
                    if (entry == null || entry.isDirectory()) {
                        return null;
                    }
                }
                return new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
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
