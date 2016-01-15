package com.github.mustachejava.functions;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.github.mustachejava.TestUtil.getContents;
import static org.junit.Assert.assertEquals;


/**
 * @author R.A. Porter
 * @version 1.0
 * @since 1/17/13
 */
public class BundleFunctionsTest {
    private static File root;
    private static final String BUNDLE = "com.github.mustachejava.functions.translatebundle";

    @Test
    public void testPreLabels() throws MustacheException, IOException, ExecutionException, InterruptedException {
        MustacheFactory c = new DefaultMustacheFactory(root);
        Mustache m = c.compile("bundles.html");
        StringWriter sw = new StringWriter();
        Map<String, Object> scope = new HashMap<>();
        scope.put("trans", BundleFunctions.newPreTranslate(BUNDLE, Locale.US));
        scope.put("replaceMe", "replaced");
        m.execute(sw, scope);
        assertEquals(getContents(root, "bundles_pre_labels.txt"), sw.toString());
    }

    @Test
    public void testPostLabels() throws MustacheException, IOException, ExecutionException, InterruptedException {
        MustacheFactory c = new DefaultMustacheFactory(root);
        Mustache m = c.compile("bundles.html");
        StringWriter sw = new StringWriter();
        Map<String, Object> scope = new HashMap<>();
        scope.put("trans", BundleFunctions.newPostTranslate(BUNDLE, Locale.US));
        scope.put("replaceMe", "replaced");
        m.execute(sw, scope);
        assertEquals(getContents(root, "bundles_post_labels.txt"), sw.toString());
    }

    @Test
    public void testPreNullLabels() throws MustacheException, IOException, ExecutionException, InterruptedException {
        MustacheFactory c = new DefaultMustacheFactory(root);
        Mustache m = c.compile("bundles.html");
        StringWriter sw = new StringWriter();
        Map<String, Object> scope = new HashMap<>();
        scope.put("trans", BundleFunctions.newPreTranslateNullableLabel(BUNDLE, Locale.US));
        scope.put("replaceMe", "replaced");
        m.execute(sw, scope);
        assertEquals(getContents(root, "bundles_nulllabels.txt"), sw.toString());
    }

    @Test
    public void testPostNullLabels() throws MustacheException, IOException, ExecutionException, InterruptedException {
        MustacheFactory c = new DefaultMustacheFactory(root);
        Mustache m = c.compile("bundles.html");
        StringWriter sw = new StringWriter();
        Map<String, Object> scope = new HashMap<>();
        scope.put("trans", BundleFunctions.newPostTranslateNullableLabel(BUNDLE, Locale.US));
        // Intentionally leave off the replacement value
        m.execute(sw, scope);
        assertEquals(getContents(root, "bundles_nulllabels.txt"), sw.toString());
    }

    @BeforeClass
    public static void setUp() throws Exception {
        File compiler = new File("compiler").exists() ? new File("compiler") : new File(".");
        root = new File(compiler, "src/test/resources/functions");
    }
}
