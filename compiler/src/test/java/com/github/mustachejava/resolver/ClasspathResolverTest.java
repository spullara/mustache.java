package com.github.mustachejava.resolver;

import org.junit.Test;

import java.io.Reader;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ClasspathResolverTest {

    @Test
    public void getReaderNullRootAndResourceHasRelativePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver();
        try (Reader reader = underTest.getReader("nested_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderWithRootAndResourceHasRelativePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates");
        try (Reader reader = underTest.getReader("absolute_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderWithRootThatHasTrailingForwardSlashAndResourceHasRelativePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates/");
        try (Reader reader = underTest.getReader("absolute_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderWithRootAndResourceHasAbsolutePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates");
        try (Reader reader = underTest.getReader("/absolute_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderWithRootThatHasTrailingForwardSlashAndResourceHasAbsolutePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates/");
        try (Reader reader = underTest.getReader("/absolute_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderNullRootDoesNotFindFileWithAbsolutePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver();
        try (Reader reader = underTest.getReader("/nested_partials_template.html")) {
            assertThat(reader, is(nullValue()));
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
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderWithRootAndResourceHasDotRelativePath() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates");
        try (Reader reader = underTest.getReader("absolute/./nested_partials_sub.html")) {
            assertThat(reader, is(notNullValue()));
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
            assertThat(reader, is(nullValue()));
        }
    }

    // questionable: probably unintended behavior
    @Test
    public void getReaderWithRootAndResourceIsDirectory() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates");
        try (Reader reader = underTest.getReader("absolute")) {
            assertThat(reader, is(nullValue()));
        }
    }

    // questionable: different than FileSystemResolver
    @Test
    public void getReaderWithRootAndResourceAboveRoot() throws Exception {
        ClasspathResolver underTest = new ClasspathResolver("templates/absolute");
        try (Reader reader = underTest.getReader("../absolute_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }
}
