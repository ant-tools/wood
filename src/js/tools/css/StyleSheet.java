package js.tools.css;

import java.io.Writer;
import java.util.List;

public interface StyleSheet
{
  List<ImportRule> getImportRules();

  List<FontFaceRule> getFontFaceRules();
  
  List<KeyFramesRule> getKeyFramesRules();

  List<Ruleset> getRulesets();

  void serialize(Writer writer);

  void minify(Writer writer);

  void dump();
}
