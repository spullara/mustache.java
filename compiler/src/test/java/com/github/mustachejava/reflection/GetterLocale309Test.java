package com.github.mustachejava.reflection;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.reflect.SimpleObjectHandler;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class GetterLocale309Test {
  @Parameterized.Parameters(name = "locale={0}, simple={1}")
  public static Collection<Object[]> locales() {
    return Arrays.asList(new Object[][] {
        {Locale.ROOT, false}, {Locale.ENGLISH, false}, {new Locale("tr", "TR"), false}, {new Locale("az", "AZ"), false},
        {Locale.ROOT, true}, {Locale.ENGLISH, true}, {new Locale("tr", "TR"), true}, {new Locale("az", "AZ"), true}
    });
  }

  private final Locale locale;
  private final boolean simple;
  private Locale previous;
  private Locale previousDisplay;
  private Locale previousFormat;

  public GetterLocale309Test(Locale locale, boolean simple) {
    this.locale = locale;
    this.simple = simple;
  }

  @Before
  public void setLocale() {
    previous = Locale.getDefault();
    previousDisplay = Locale.getDefault(Locale.Category.DISPLAY);
    previousFormat = Locale.getDefault(Locale.Category.FORMAT);
    Locale.setDefault(locale);
  }

  @After
  public void restoreLocale() {
    Locale.setDefault(previous);
    Locale.setDefault(Locale.Category.DISPLAY, previousDisplay);
    Locale.setDefault(Locale.Category.FORMAT, previousFormat);
  }

  private String render(String template, Object scope) {
    Mustache mustache = factory().compile(new StringReader(template), "getter-locale");
    return execute(mustache, scope);
  }

  private DefaultMustacheFactory factory() {
    DefaultMustacheFactory factory = new DefaultMustacheFactory();
    if (simple) factory.setObjectHandler(new SimpleObjectHandler());
    return factory;
  }

  private String execute(Mustache mustache, Object scope) {
    StringWriter out = new StringWriter();
    mustache.execute(out, scope);
    return out.toString();
  }

  public static class Bean {
    public String getIngredients() { return "cheese"; }
    public String getSauces() { return "tomato"; }
    public boolean isIndexed() { return true; }
    public boolean isIdle() { return false; }
  }

  public static class InheritedBean extends Bean {
  }

  public interface Ingredients {
    String getIngredients();
  }

  private static class InterfaceBean implements Ingredients {
    @Override
    public String getIngredients() { return "interface"; }
  }

  public static class PriorityBean {
    public String ingredients = "field";
    public String getIngredients() { return "getter"; }
    public String sauces() { return "direct"; }
    public String getSauces() { return "getter"; }
    public String onlyField = "fallback";
  }

  public static class DifferentBean {
    public String getIngredients() { return "different"; }
  }

  public static class UnicodeBean {
    public String getSSName() { return "unicode"; }
  }

  @Test
  public void getterNamesDoNotDependOnLocale() {
    assertEquals("cheese/tomato", render("{{ingredients}}/{{sauces}}", new Bean()));
  }

  @Test
  public void booleanGetterNamesDoNotDependOnLocale() {
    assertEquals("true/false", render("{{#indexed}}true{{/indexed}}{{^idle}}/false{{/idle}}", new Bean()));
  }

  @Test
  public void inheritedGetterNamesDoNotDependOnLocale() {
    assertEquals("cheese", render("{{ingredients}}", new InheritedBean()));
  }

  @Test
  public void interfaceGetterNamesDoNotDependOnLocale() {
    assertEquals("interface", render("{{ingredients}}", new InterfaceBean()));
  }

  @Test
  public void memberLookupPriorityIsPreserved() {
    assertEquals("getter/direct/fallback", render("{{ingredients}}/{{sauces}}/{{onlyField}}", new PriorityBean()));
  }

  @Test
  public void cachedTemplateResolvesNewClassesAfterLocaleChanges() {
    Mustache mustache = factory().compile(new StringReader("{{ingredients}}"), "cached-getter-locale");
    Locale.setDefault(Locale.ENGLISH);
    assertEquals("cheese", execute(mustache, new Bean()));
    Locale.setDefault(new Locale("tr", "TR"));
    assertEquals("different", execute(mustache, new DifferentBean()));
    Locale.setDefault(new Locale("az", "AZ"));
    assertEquals("cheese", execute(mustache, new Bean()));
  }

  @Test
  public void multiCharacterUppercaseMappingIsPreserved() {
    assertEquals("unicode", render("{{ßName}}", new UnicodeBean()));
  }
}
