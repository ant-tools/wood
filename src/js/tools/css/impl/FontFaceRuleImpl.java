package js.tools.css.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.tools.css.CssException;
import js.tools.css.FontFaceRule;

final class FontFaceRuleImpl implements FontFaceRule
{
  private static final Pattern PATTERN = Pattern.compile(
      "font\\-face\\s+\\{\\s*font-family\\s*\\:\\s*['\"](.+)['\"];\\s*src\\:\\s*URL\\(['\"](.+)['\"]\\);\\s*\\}", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

  private String fontFamily;
  private URL src;

  public FontFaceRuleImpl(String fontFaceRule)
  {
    Matcher matcher = PATTERN.matcher(fontFaceRule);
    if(!matcher.find()) {
      throw new CssException("Invalid import rule syntax |%s|.", fontFaceRule);
    }
    this.fontFamily = matcher.group(1);
    try {
      this.src = new URL(matcher.group(2));
    }
    catch(MalformedURLException e) {
      throw new CssException("Invalid import rule syntax |%s|.", fontFaceRule);
    }
  }

  @Override
  public String getFontFamily()
  {
    return this.fontFamily;
  }

  @Override
  public URL getSrc()
  {
    return this.src;
  }
}
