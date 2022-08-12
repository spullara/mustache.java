package com.github.mustachejava;


import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

class Model {
  public String repoOwner;
  public String repoName;
  public List<Link> links = new ArrayList<>();

  public static class Link {

    private String urlTemplate;

    public TemplateFunction url = (s) -> urlTemplate;

    public Link(String urlTemplate) {
      this.urlTemplate = urlTemplate;
    }
  }
}

public class TemplateFunctionTest {
  @Test
  public void testModel() {
    MustacheFactory mf = new DefaultMustacheFactory();
    Model model = new Model();
    model.repoOwner = "foo";
    model.repoName = "bar";
    model.links.add(new Model.Link("https://github.com/{{repoOwner}}/{{repoName}}"));
    String template = "owner: {{repoOwner}}\n" +
            "name: {{repoName}}\n" +
            "{{#links}}\n" +
            " - {{url}}\n" +
            "{{/links}}";
    Mustache mustache = mf.compile(new StringReader(template), "test");
    StringWriter writer = new StringWriter();
    mustache.execute(writer, model);
    assertEquals("owner: foo\n" +
            "name: bar\n" +
            " - https://github.com/foo/bar\n", writer.toString());
  }
}
