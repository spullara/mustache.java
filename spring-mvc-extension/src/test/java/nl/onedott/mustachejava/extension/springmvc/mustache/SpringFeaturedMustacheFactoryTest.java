package nl.onedott.mustachejava.extension.springmvc.mustache;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.SimpleTheme;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ThemeResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpringFeaturedMustacheFactoryTest {

    private final static Locale ENGLISH = Locale.ENGLISH;
    private final static Locale DUTCH = Locale.forLanguageTag("nl_NL");
    private StaticWebApplicationContext wac;
    private SpringFeaturedMustacheFactory mustacheFactory;
    private MockHttpServletRequest request;

    @Before
    public void setup() throws IOException {
        request = new MockHttpServletRequest();
        RequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);

        wac = new StaticWebApplicationContext();
        wac.refresh();

        request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

        Resource resourceLocation = wac.getResource("classpath:mustache/");
        mustacheFactory = new SpringFeaturedMustacheFactory(resourceLocation.getFile());
    }

    @Test
    public void renderMessage() throws Exception {
        enableMessageSource();
        assertEquals("The sky is blue and snow is white", renderWithLocale("message", ENGLISH));
        assertEquals("The sky is blauw and snow is wit", renderWithLocale("message", DUTCH));
    }

    @Test
    public void renderMessage_printCodeAsDefault() throws Exception {
        assertEquals("The sky is sky.color and snow is snow.color", renderWithLocale("message", ENGLISH));
    }

    @Test
    public void renderTheme() throws Exception {
        enableMessageSource();
        enableTheming(request);

        assertEquals("My theme is mustache", renderWithLocale("theme", ENGLISH));
        assertEquals("My theme is snor", renderWithLocale("theme", DUTCH));
    }

    @Test(expected = MustacheException.class)
    public void renderTheme_themeNotFound() throws Exception {
        assertEquals("My theme is mustache", renderWithLocale("theme", ENGLISH));
    }


    private String renderWithLocale(final String name, final Locale locale) {
        mustacheFactory.setLocaleResolver(new LocaleResolver() {
            @Override
            public Locale resolveLocale(final HttpServletRequest request) {
                return locale;
            }

            @Override
            public void setLocale(final HttpServletRequest request, final HttpServletResponse response, final Locale locale) {

            }
        });

        Mustache mustache = mustacheFactory.compile(name + ".mustache");

        StringWriter writer = new StringWriter();
        mustache.execute(writer, new Object());
        return writer.toString().trim();
    }

    private void enableMessageSource() {
        wac.addMessage("sky.color", ENGLISH, "blue");
        wac.addMessage("snow.color", ENGLISH, "white");
        wac.addMessage("sky.color", DUTCH, "blauw");
        wac.addMessage("snow.color", DUTCH, "wit");
        wac.addMessage("cssTheme", ENGLISH, "mustache");
        wac.addMessage("cssTheme", DUTCH, "snor");
    }

    private void enableTheming(final MockHttpServletRequest request) {
        ThemeResolver themeResolver = mock(ThemeResolver.class);
        when(themeResolver.resolveThemeName(any(HttpServletRequest.class))).thenReturn("my-theme");
        request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, themeResolver);

        ThemeSource themeSource = mock(ThemeSource.class);
        when(themeSource.getTheme(any(String.class))).thenReturn(new SimpleTheme(
                "my-theme",
                (MessageSource) request.getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE)));

        request.setAttribute(DispatcherServlet.THEME_SOURCE_ATTRIBUTE, themeSource);
    }
}
