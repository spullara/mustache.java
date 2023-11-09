package com.github.mustachejava;

import com.github.mustachejavabenchmarks.NullWriter;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class SpecDisc155Test {

    static class Friend {
        String name = "Sam";
    }

    static class I18N {
        String translationKey;
        Function<String, String> key = content -> {
            translationKey = content;
            return "";
        };
        String defaultText;
        TemplateFunction t = content -> {
            defaultText = content;
            return "";
        };

        Map<String, String> translations;

        {
            translations = new HashMap<>();
            translations.put("goodmorning", "좋은 아침이에요 {{name}}");
        }

        MustacheFactory mf = new DefaultMustacheFactory();

        TemplateFunction i18n = content -> {
            try {
                mf.compile(new StringReader(content), "i18n").execute(new NullWriter(), this).flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (translations.containsKey(translationKey)) {
                return translations.get(translationKey);
            } else {
                return defaultText;
            }
        };
    }

    @Test
    public void testDisc155() throws IOException {
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache m = mf.compile(new StringReader("{{#friend}}{{#i18n}}{{#key}}goodmorning{{/key}}{{#t}}Good morning, {{name}}{{/t}}{{/i18n}}{{/friend}}"), "test");
        StringWriter sw = new StringWriter();
        m.execute(sw, new Object[] {new I18N(), new Object() { public Friend friend = new Friend();}}).flush();
        assertEquals("좋은 아침이에요 Sam", sw.toString());
    }
}
