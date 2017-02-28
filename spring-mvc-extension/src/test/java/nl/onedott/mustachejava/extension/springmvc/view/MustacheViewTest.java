package nl.onedott.mustachejava.extension.springmvc.view;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = MustacheViewTest.MustacheViewContext.class)
public class MustacheViewTest {

    @Configuration
    public static class MustacheViewContext {}

    @Autowired
    private WebApplicationContext wac;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MustacheFactory mustacheFactory = new DefaultMustacheFactory("mustache/");

    @Before
    public void setup() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void testRenderSimpleTemplate() throws Exception {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("name", "John");
        renderView("hello.mustache", model);
        assertEquals("Hello John", response.getContentAsString().trim());
    }

    @Test(expected = MustacheException.class)
    public void testTemplateNotFound() throws Exception {
        renderView("i-do-not-exist", null);
    }


    private void renderView(String url, Map<String, ?> model) throws Exception {
        MustacheView view = new MustacheView();
        view.setApplicationContext(wac);
        view.setUrl(url);
        view.setMustacheFactory(mustacheFactory);
        view.render(model, request, response);
    }
}
