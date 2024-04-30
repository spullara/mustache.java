package com.github.mustachejava.resolver;

import org.junit.Test;

import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ClasspathResolverTest {

    @Test
    public void getReaderNullRootAndResourceHasRelativePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver();
        try (Reader reader = underTest.getReader("nested_partials_template.html")) {
            assertNotNull(reader);
        }
    }

    @Test
    public void getReaderWithRootAndResourceHasRelativePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates");
        try (Reader reader = underTest.getReader("absolute_partials_template.html")) {
            assertNotNull(reader);
        }
    }

    @Test
    public void getReaderWithRootThatHasTrailingForwardSlashAndResourceHasRelativePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates/");
        try (Reader reader = underTest.getReader("absolute_partials_template.html")) {
            assertNotNull(reader);
        }
    }

    @Test
    public void getReaderWithRootAndResourceHasAbsolutePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates");
        try (Reader reader = underTest.getReader("/absolute_partials_template.html")) {
            assertNotNull(reader);
        }
    }

    @Test
    public void getReaderWithRootThatHasTrailingForwardSlashAndResourceHasAbsolutePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates/");
        try (Reader reader = underTest.getReader("/absolute_partials_template.html")) {
            assertNotNull(reader);
        }
    }

    @Test
    public void getReaderNullRootDoesNotFindFileWithAbsolutePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver();
        try (Reader reader = underTest.getReader("/nested_partials_template.html")) {
            assertNull(reader);
        }
    }

    @Test (expected = NullPointerException.class)
    public void getReaderWithRootAndNullResource() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates");
        underTest.getReader(null);
    }

    @Test (expected = NullPointerException.class)
    public void getReaderNullRootAndNullResourceThrowsNullPointer() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver();
        underTest.getReader(null);
    }

    @Test
    public void getReaderWithRootAndResourceHasDoubleDotRelativePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates");
        try (Reader reader = underTest.getReader("absolute/../absolute_partials_template.html")) {
            assertNotNull(reader);
        }
    }

    @Test
    public void getReaderWithRootAndResourceHasDotRelativePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates");
        try (Reader reader = underTest.getReader("absolute/./nested_partials_sub.html")) {
            assertNotNull(reader);
        }
    }

    // questionable: different than FileSystemResolver
    @Test (expected = IllegalArgumentException.class)
    public void getReaderNullRootAndResourceHasInvalidPathThrows() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver();
        underTest.getReader("\0");
    }

    // questionable: different than FileSystemResolver
    @Test (expected = IllegalArgumentException.class)
    public void getReaderWithRootAndResourceHasInvalidPathThrows() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates");
        underTest.getReader("\0");
    }

    // questionable: probably unintended behavior
    @Test
    public void getReaderNullRootAndResourceIsDirectory() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver();
        try (Reader reader = underTest.getReader("templates/absolute")) {
            assertNull(reader);
        }
    }

    // questionable: probably unintended behavior
    @Test
    public void getReaderWithRootAndResourceIsDirectory() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates");
        try (Reader reader = underTest.getReader("absolute")) {
            assertNull(reader);
        }
    }

    // questionable: different than FileSystemResolver
    @Test
    public void getReaderWithRootAndResourceAboveRoot() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates/absolute");
        try (Reader reader = underTest.getReader("../absolute_partials_template.html")) {
            assertNotNull(reader);
        }
    }

    @Test
    public void getReaderWithoutContextClassLoader() throws Exception {
        ClassLoader savedContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[]{}, null));

            ClasspathResolver underTest = new ClasspathResolver();
            try (Reader reader = underTest.getReader("template.mustache")) {
                assertNotNull(reader);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(savedContextClassLoader);
        }
    }
}
