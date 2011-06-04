package mustache.plugin;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: 6/4/11
 * Time: 12:30 PM
 */
public class MustacheFileType extends LanguageFileType {
  protected MustacheFileType() {
    super(new MustacheLanguage());
  }

  @NotNull
  public String getName() {
    return "mustache";
  }

  @NotNull
  public String getDescription() {
    return "Mustache is a logicless templating language";
  }

  @NotNull
  public String getDefaultExtension() {
    return "mustache";
  }

  public Icon getIcon() {
    return IconLoader.getIcon("/mustache.png");
  }
}
