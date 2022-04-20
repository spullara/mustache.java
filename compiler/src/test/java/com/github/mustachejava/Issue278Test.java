package com.github.mustachejava;

import com.github.mustachejava.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.mustachejava.TestUtil.getContents;
import static org.junit.Assert.assertEquals;


/**
 * @author K. Wright
 * @version 1.0
 * @since 2022-04-20
 */
public class Issue278Test {
    private static File root;

    static Function<String, String> transformFunc = input ->
        Arrays
            .stream(input.split("\n"))
            .map(x -> "^^^\n***" + x)
            .collect(Collectors.joining("\n", "", "\n"));


    @Test
    public void testMultilineFunc() throws MustacheException, IOException {
        MustacheFactory c = new SpecMustacheFactory(root);
        Mustache m = c.compile("issue278.html");
        StringWriter sw = new StringWriter();
        Map<String, Object> scope = new HashMap<>();
        scope.put("transform", transformFunc);
        m.execute(sw, scope);
        assertEquals(getContents(root, "issue278.txt"), sw.toString());
    }

    @BeforeClass
    public static void setUp() throws Exception {
        File compiler = new File("compiler").exists() ? new File("compiler") : new File(".");
        root = new File(compiler, "src/test/resources/issue_278");
    }
}
