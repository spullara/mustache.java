package nl.onedott.mustachejava.extension.springmvc.view;

import com.github.mustachejava.DefaultMustacheFactory;
import nl.onedott.mustachejava.extension.springmvc.SpringRequestContextSupport;
import nl.onedott.mustachejava.extension.springmvc.mustache.MessageSupportReflectionObjectHandler;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.ModelMap;

import static org.junit.Assert.assertEquals;

public class MustacheViewTest extends SpringRequestContextSupport {
    private DefaultMustacheFactory mustacheFactory;
    private MustacheView view;

    @Before
    public void setup() throws Exception {
        super.setup();

        mustacheFactory = new DefaultMustacheFactory("templates/");
        mustacheFactory.setObjectHandler(new MessageSupportReflectionObjectHandler(webApplicationContext()));

        view = new MustacheView();
        view.setApplicationContext(webApplicationContext());
        view.setMustacheFactory(mustacheFactory);
    }

    @Test
    public void testRenderMergedTemplateModel() throws Exception {
        ModelMap model = new ModelMap("dog", new Dog("Bello"));
        view.setUrl("view.mustache");
        view.render(model, request(), response());
        assertEquals("My dog's name is Bello and says \"woof!\"", response().getContentAsString().trim());
    }

    public class Dog {
        private final String name;

        private Dog(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String speak() {
            return "woof!";
        }
    }
}
