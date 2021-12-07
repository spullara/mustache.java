package com.github.mustachejava.resolver;

import com.github.mustachejava.MustacheException;
import org.junit.Test;

import java.io.File;
import java.io.Reader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FileSystemResolverTest {

    private static String resources = "src/test/resources";

    @Test
    public void getReaderDefaultRootAndResourceHasRelativePath() throws Exception {
        FileSystemResolver underTest = new FileSystemResolver();
        try (Reader reader = underTest.getReader(resources + "/nested_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderFileRootAndResourceHasRelativePath() throws Exception {
        File fileRoot = new File(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(fileRoot);
        try (Reader reader = underTest.getReader("absolute_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderPathRootAndResourceHasRelativePath() throws Exception {
        Path pathRoot = Paths.get(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(pathRoot);
        try (Reader reader = underTest.getReader("absolute_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderDefaultRootDoesNotFindFileWithAbsolutePath() throws Exception {
        FileSystemResolver underTest = new FileSystemResolver();
        try (Reader reader = underTest.getReader("/" + resources + "/nested_partials_template.html")) {
            assertThat(reader, is(nullValue()));
        }
    }

    @Test
    public void getReaderFileRootAndResourceHasAbsolutePath() throws Exception {
        File fileRoot = new File(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(fileRoot);
        try (Reader reader = underTest.getReader("/absolute_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderPathRootAndResourceHasAbsolutePath() throws Exception {
        Path pathRoot = Paths.get(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(pathRoot);
        try (Reader reader = underTest.getReader("/absolute_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test (expected = NullPointerException.class)
    public void getReaderDefaultRootAndNullResourceThrowsNullPointer() throws Exception {
        FileSystemResolver underTest = new FileSystemResolver();
        underTest.getReader(null);
    }

    @Test (expected = NullPointerException.class)
    public void getReaderFileRootAndNullResourceThrowsNullPointer() throws Exception {
        File fileRoot = new File(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(fileRoot);
        underTest.getReader(null);
    }

    @Test (expected = NullPointerException.class)
    public void getReaderPathRootAndNullResourceThrowsNullPointer() throws Exception {
        Path pathRoot = Paths.get(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(pathRoot);
        underTest.getReader(null);
    }

    @Test
    public void getReaderDefaultRootAndResourceHasDoubleDotRelativePath() throws Exception {
        FileSystemResolver underTest = new FileSystemResolver();
        try (Reader reader = underTest.getReader(resources + "/templates_filepath/../nested_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderFileRootAndResourceHasDoubleDotRelativePath() throws Exception {
        File fileRoot = new File(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(fileRoot);
        try (Reader reader = underTest.getReader("absolute/../absolute_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderPathRootAndResourceHasDoubleDotRelativePath() throws Exception {
        Path pathRoot = Paths.get(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(pathRoot);
        try (Reader reader = underTest.getReader("absolute/../absolute_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderDefaultRootAndResourceHasDotRelativePath() throws Exception {
        FileSystemResolver underTest = new FileSystemResolver();
        try (Reader reader = underTest.getReader(resources + "/templates_filepath/./absolute_partials_template.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderFileRootAndResourceHasDotRelativePath() throws Exception {
        File fileRoot = new File(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(fileRoot);
        try (Reader reader = underTest.getReader("absolute/./nested_partials_sub.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderPathRootAndResourceHasDotRelativePath() throws Exception {
        Path pathRoot = Paths.get(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(pathRoot);
        try (Reader reader = underTest.getReader("absolute/./nested_partials_sub.html")) {
            assertThat(reader, is(notNullValue()));
        }
    }

    @Test
    public void getReaderDefaultRootAndResourceHasInvalidPath() throws Exception {
        FileSystemResolver underTest = new FileSystemResolver();
        try (Reader reader = underTest.getReader("\0")) {
            assertThat(reader, is(nullValue()));
        }
    }

    @Test
    public void getReaderFileRootAndResourceHasInvalidPath() throws Exception {
        File fileRoot = new File(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(fileRoot);
        try (Reader reader = underTest.getReader("\0")) {
            assertThat(reader, is(nullValue()));
        }
    }

    @Test
    public void getReaderPathRootAndResourceHasInvalidPath() throws Exception {
        Path pathRoot = Paths.get(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(pathRoot);
        try (Reader reader = underTest.getReader("\0")) {
            assertThat(reader, is(nullValue()));
        }
    }

    @Test
    public void getReaderPathRootAndNonDefaultFileSystem() throws Exception {
        Path zipFile = Paths.get(resources + "/templates.jar");
        try (FileSystem zipFileSystem = FileSystems.newFileSystem(zipFile, (ClassLoader) null)) {
            Path pathRoot = zipFileSystem.getPath("templates");
            FileSystemResolver underTest = new FileSystemResolver(pathRoot);
            try (Reader reader = underTest.getReader("absolute_partials_template.html")) {
                assertThat(reader, is(notNullValue()));
            }
        }
    }

    @Test
    public void getReaderDefaultRootAndResourceIsDirectory() throws Exception {
        FileSystemResolver underTest = new FileSystemResolver();
        try (Reader reader = underTest.getReader(resources + "/templates_filepath")) {
            assertThat(reader, is(nullValue()));
        }
    }

    @Test
    public void getReaderFileRootAndResourceIsDirectory() throws Exception {
        File fileRoot = new File(resources);
        FileSystemResolver underTest = new FileSystemResolver(fileRoot);
        try (Reader reader = underTest.getReader("templates_filepath")) {
            assertThat(reader, is(nullValue()));
        }
    }

    @Test
    public void getReaderPathRootAndResourceIsDirectory() throws Exception {
        Path pathRoot = Paths.get(resources);
        FileSystemResolver underTest = new FileSystemResolver(pathRoot);
        try (Reader reader = underTest.getReader("templates_filepath")) {
            assertThat(reader, is(nullValue()));
        }
    }

    // questionable: all the AboveRoot tests, which expose the information of
    //               whether the resource file exists

    @Test
    public void getReaderDefaultRootAndResourceAboveRootNotFound() throws Exception {
        FileSystemResolver underTest = new FileSystemResolver();
        try (Reader reader = underTest.getReader("../this_file_does_not_exist.html")) {
            assertThat(reader, is(nullValue()));
        }
    }

    @Test (expected = MustacheException.class)
    public void getReaderFileRootAndResourceAboveRootFound() throws Exception {
        File fileRoot = new File(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(fileRoot);
        underTest.getReader("../nested_partials_template.html");
    }

    @Test
    public void getReaderFileRootAndResourceAboveRootNotFound() throws Exception {
        File fileRoot = new File(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(fileRoot);
        try (Reader reader = underTest.getReader("../this_file_does_not_exist.html")) {
            assertThat(reader, is(nullValue()));
        }
    }

    @Test (expected = MustacheException.class)
    public void getReaderPathRootAndResourceAboveRootFound() throws Exception {
        Path pathRoot = Paths.get(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(pathRoot);
        underTest.getReader("../nested_partials_template.html");
    }

    @Test
    public void getReaderPathRootAndResourceAboveRootNotFound() throws Exception {
        Path pathRoot = Paths.get(resources + "/templates_filepath");
        FileSystemResolver underTest = new FileSystemResolver(pathRoot);
        try (Reader reader = underTest.getReader("../this_file_does_not_exist.html")) {
            assertThat(reader, is(nullValue()));
        }
    }
}
