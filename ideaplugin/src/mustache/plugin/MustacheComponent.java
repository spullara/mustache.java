package mustache.plugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.fileTypes.FileTypeManager;
import org.jetbrains.annotations.NotNull;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: 6/4/11
 * Time: 12:22 PM
 */
public class MustacheComponent implements ApplicationComponent {
  public MustacheComponent() {
  }

  public void initComponent() {
    ApplicationManager.getApplication().runWriteAction(
        new Runnable() {
          public void run() {
            FileTypeManager.getInstance().associateExtension(new MustacheFileType(), "mustache");
          }
        }
    );
  }

  public void disposeComponent() {
    // TODO: insert component disposal logic here
  }

  @NotNull
  public String getComponentName() {
    return "MustacheLanguage";
  }
}
