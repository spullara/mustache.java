package nl.onedott.mustachejava.extension.springmvc.mustache;

import com.github.mustachejava.util.Wrapper;
import nl.onedott.mustachejava.extension.springmvc.SpringRequestContextSupport;
import nl.onedott.mustachejava.extension.springmvc.annotation.Message;
import nl.onedott.mustachejava.extension.springmvc.annotation.Theme;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.support.StaticWebApplicationContext;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MessageSupportReflectionObjectHandlerTest extends SpringRequestContextSupport {
    private StaticWebApplicationContext context;

    @Before
    public void setup() throws Exception {
        super.setup();
        context = webApplicationContext();
        context.addMessage("greeting", Locale.ENGLISH, "Hello");
        context.addMessage("name", Locale.ENGLISH, "mustache");
        context.addMessage("color", Locale.ENGLISH, "red");
        context.refresh();
    }

    @Test
    public void testReturnMessageSupportReflectionWrapper() throws Exception {
        MessageSupportReflectionObjectHandler h = new MessageSupportReflectionObjectHandler(context);

        // Return MessageSupportReflectionWrapper when annotated
        Wrapper wrapper1 = h.find("greeting", new Object[] { new MustacheGreetingContext() });
        assertTrue(wrapper1 instanceof MessageSupportReflectionWrapper);

        // Return default wrappers when not annotated
        Wrapper wrapper2 = h.find("age", new Object[] { new MustacheGreetingContext() });
        assertFalse(wrapper2 instanceof MessageSupportReflectionWrapper);
    }

    @Test
    public void testResolvedMessage() throws Exception {
        Object[] scopes = new Object[] { new MustacheGreetingContext() };
        MessageSupportReflectionObjectHandler h = new MessageSupportReflectionObjectHandler(context);

        // Message
        Wrapper wrapper1 = h.find("greeting", scopes);
        assertEquals("Hello", wrapper1.call(scopes));

        // Message
        Wrapper wrapper2 = h.find("name", scopes);
        assertEquals("mustache", wrapper2.call(scopes));

        // Theme message
        Wrapper wrapper3 = h.find("color", scopes);
        assertEquals("red", wrapper3.call(scopes));
    }

    public static class MustacheGreetingContext {

        private final String name;

        public MustacheGreetingContext() {
            this("name");
        }

        public MustacheGreetingContext(final String name) {
            this.name = name;
        }

        public int getAge() {
            return 9;
        }

        @Message
        public String getGreeting() {
            return "greeting";
        }

        @Message
        public String getName() {
            return name;
        }

        @Theme
        public String getColor() {
            return "color";
        }
    }
}
