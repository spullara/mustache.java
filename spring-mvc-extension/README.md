#Mustache extension for Spring MVC

To enable mustache views add the `@EnableMustache` annotation to your dispatcher servlet configuration class.
There is support for displaying messages and theme messages using variable prefixes.

For example `{{message:my.greeting}}` will display the value of that message.
Theme messages can be accessed using the `{{theme:theme.cssFile}}` format.

A typical configuration could look like:

    @Configuration
    @EnableMustache("/WEB-INF/view/mustache/")
    public class SpringServletConfig extends WebMvcConfigurationSupport {
    
        @Override
        protected void addInterceptors(final InterceptorRegistry registry) {
            registry.addInterceptor(new LocaleChangeInterceptor()).addPathPatterns("/**");
            registry.addInterceptor(new ThemeChangeInterceptor()).addPathPatterns("/**");
        }
    
        @Bean
        LocaleResolver localeResolver() {
            CookieLocaleResolver localeResolver = new CookieLocaleResolver();
            localeResolver.setDefaultLocale(Locale.ENGLISH);
            return localeResolver;
        }
    
        @Bean
        ThemeResolver themeResolver() {
            CookieThemeResolver themeResolver = new CookieThemeResolver();
            themeResolver.setDefaultThemeName("christmass");
            return themeResolver;
        }
    }
