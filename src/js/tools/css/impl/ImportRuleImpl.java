package js.tools.css.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.tools.css.CssException;
import js.tools.css.ImportRule;

final class ImportRuleImpl implements ImportRule
{
  private static final Pattern PATTERN = Pattern.compile("import\\s+(?:URL\\()?(?:\")?([^\"\\)]+)(?:\")?(?:\\))?", Pattern.CASE_INSENSITIVE);

  private String href;

  ImportRuleImpl(String importRule)
  {
    Matcher matcher = ImportRuleImpl.PATTERN.matcher(importRule);
    if(!matcher.find()) throw new CssException("Invalid import rule syntax |%s|.", importRule);
    this.href = matcher.group(1);
  }

  @Override
  public void setHref(String href)
  {
    this.href = href;
  }

  @Override
  public String getHref()
  {
    return this.href;
  }
}
