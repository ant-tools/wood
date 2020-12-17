package js.tools.css.impl;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import js.tools.css.FontFaceRule;
import js.tools.css.ImportRule;
import js.tools.css.KeyFramesRule;
import js.tools.css.Ruleset;
import js.tools.css.StyleSheet;

final class StyleSheetImpl implements StyleSheet
{
  private List<ImportRule> importRules = new ArrayList<ImportRule>();
  private List<FontFaceRule> fontFaceRules = new ArrayList<FontFaceRule>();
  private List<KeyFramesRule> keyFramesRules = new ArrayList<KeyFramesRule>();
  private List<Ruleset> rulesets = new ArrayList<Ruleset>();

  @Override
  public List<ImportRule> getImportRules()
  {
    return this.importRules;
  }

  @Override
  public List<FontFaceRule> getFontFaceRules()
  {
    return this.fontFaceRules;
  }

  @Override
  public List<KeyFramesRule> getKeyFramesRules()
  {
    return keyFramesRules;
  }

  @Override
  public List<Ruleset> getRulesets()
  {
    return this.rulesets;
  }

  @Override
  public void serialize(Writer writer)
  {
    Serializer serializer = new Serializer(writer);
    serializer.serialize(this);
  }

  @Override
  public void minify(Writer writer)
  {
    Serializer serializer = new Serializer(writer);
    serializer.minify(this);
  }

  @Override
  public void dump()
  {
    Serializer serializer = new Serializer();
    serializer.dump(this);
  }

  void addImportRule(ImportRule importRule)
  {
    this.importRules.add(importRule);
  }

  void addFontFaceRule(FontFaceRule fontFaceRule)
  {
    this.fontFaceRules.add(fontFaceRule);
  }

  void addKeyFramesRule(KeyFramesRule keyFramesRule)
  {
    keyFramesRules.add(keyFramesRule);
  }

  void addRuleset(Ruleset ruleset)
  {
    this.rulesets.add(ruleset);
  }
}
