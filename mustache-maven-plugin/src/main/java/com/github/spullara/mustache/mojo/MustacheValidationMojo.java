package com.github.spullara.mustache.mojo;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SimpleSourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collections;
import java.util.Set;

@Mojo(name = "validate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class MustacheValidationMojo extends AbstractMojo {

  @Parameter(defaultValue = "src/main/resources")
  private File sourceDirectory;

  @Parameter(defaultValue = "target/classes")
  private File outputDirectory;

  @Parameter(defaultValue = "mustache")
  private String extension;

  @Parameter(defaultValue = "false")
  private boolean includeStale;

  public void execute() throws MojoExecutionException, MojoFailureException {
    SourceInclusionScanner scanner = includeStale
            ? new StaleSourceScanner(1024, Collections.singleton("**/*." + extension), Collections.<String>emptySet())
            : new SimpleSourceInclusionScanner(Collections.singleton("**/*." + extension), Collections.<String>emptySet());
    scanner.addSourceMapping(new SuffixMapping("." + extension, "." + extension));

    MustacheFactory mustacheFactory = new DefaultMustacheFactory();
    try {
      Set<File> files = scanner.getIncludedSources(sourceDirectory, outputDirectory);
      for (File file : files) {
        try {
          mustacheFactory.compile(new FileReader(file), file.getAbsolutePath());
        } catch (MustacheException e) {
          throw new MojoFailureException(e.getMessage(), e);
        }
      }
    } catch (InclusionScanException | FileNotFoundException e) {
      throw new MojoExecutionException(e.getMessage());
    }
  }
}
