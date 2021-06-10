package nl.onedott.mustachejava.extension.springmvc;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.SimpleTheme;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ThemeResolver;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Bart Tegenbosch
 */
public abstract class SpringRequestContextSupport {

    private MockServletContext servletContext;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private StaticWebApplicationContext webApplicationContext;

    public void setup() throws Exception {
        servletContext = new MockServletContext();
        webApplicationContext = new StaticWebApplicationContext();
        webApplicationContext.setServletContext(servletContext);
        response = new MockHttpServletResponse();
        request = new MockHttpServletRequest();
        request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, webApplicationContext);

        RequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);

        setupTheme("default");
    }

    public StaticWebApplicationContext webApplicationContext() {
        return webApplicationContext;
    }

    public MockHttpServletRequest request() {
        return request;
    }

    public MockHttpServletResponse response() {
        return response;
    }

    public void setupTheme(String themeName) {
        ThemeResolver themeResolver = mock(ThemeResolver.class);
        when(themeResolver.resolveThemeName(any(HttpServletRequest.class))).thenReturn(themeName);

        ThemeSource themeSource = mock(ThemeSource.class);
        when(themeSource.getTheme(any(String.class))).thenReturn(new SimpleTheme(themeName, webApplicationContext));

        request.setAttribute(DispatcherServlet.THEME_RESOLVER_ATTRIBUTE, themeResolver);
        request.setAttribute(DispatcherServlet.THEME_SOURCE_ATTRIBUTE, themeSource);
    }
}
