#Mustache extension for Spring MVC

To enable mustache views in your Spring MVC application add the `@EnableMustache` annotation to your configuration class.

###Support for messages and themes

This extension provides annotations to let you handle message resolving in your model attributes.

<table>
    <thead>
        <tr>
            <th>Annotation</th>
            <th>Description</th>
    </thead>
    <tbody>
        <tr>
            <td>@Message</td>
            <td>Annotated methods will have their return values resolved to a message.</td>
        </tr>
        <tr>
            <td>@Theme</td>
            <td>Annotated methods will have their return values resolved to a theme message.</td>
        </tr>
    </tbody>
</table>

Both `@Message` and `@Theme` can resolve embedded values.

    public class MustacheGreetingContext {

        private final String name;

        public MustacheGreetingContext() {
            this("name");
        }

        public MustacheGreetingContext(final String name) {
            this.name = name;
        }

        @Message
        public String getGreeting() {
            return "${my.greeting.property}";
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
