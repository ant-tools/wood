package js.tools.css.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import js.tools.css.CssException;
import js.tools.css.FontFaceRule;
import js.tools.css.ImportRule;
import js.tools.css.KeyFramesRule;
import js.tools.css.Parser;
import js.tools.css.ParserListener;
import js.tools.css.StyleSheet;

public final class ParserImpl implements Parser
{
  private StringBuilder atRuleBuilder = new StringBuilder();
  private StringBuilder selectorBuilder = new StringBuilder();
  private StringBuilder propertyBuilder = new StringBuilder();
  private StringBuilder valueBuilder = new StringBuilder();

  private State state = State.INSIDE_SELECTOR;
  private State savedState;
  private char previousChar;
  private int bracesCount;

  private List<String> selectorNames = new ArrayList<String>();
  private List<PropertyValueImpl> values = new ArrayList<PropertyValueImpl>();

  private StyleSheetImpl styleSheet = new StyleSheetImpl();
  private ParserListener listener;

  private AtRuleParser atRuleParser;

  @Override
  public void setListener(ParserListener listener)
  {
    this.listener = listener;
  }

  @Override
  public StyleSheet parse(String string)
  {
    if(string == null || string.trim().isEmpty()) {
      return this.styleSheet;
    }
    return parse(new StringReader(string));
  }

  @Override
  public StyleSheet parse(Reader reader)
  {
    if(!(reader instanceof BufferedReader)) {
      reader = new BufferedReader(reader);
    }
    try {
      for(;;) {
        int i = reader.read();
        if(i == -1) break;
        parse((char)i);
      }
    }
    catch(IOException e) {
      throw new CssException(e);
    }
    finally {
      try {
        reader.close();
      }
      catch(IOException e) {}
    }
    return this.styleSheet;
  }

  private void parse(char c)
  {
    if(c == Chars.SLASH && this.state != State.SLASH_DETECTED && this.state != State.INSIDE_COMMENT) {
      this.savedState = this.state;
      this.state = State.SLASH_DETECTED;
      return;
    }

    if(this.state == State.SLASH_DETECTED) {
      if(c == Chars.STAR) {
        this.state = State.INSIDE_COMMENT;
        dispatch(c);
        return;
      }

      this.state = this.savedState;
      dispatch(Chars.SLASH);
    }

    dispatch(c);
    this.previousChar = c;
  }

  @SuppressWarnings("incomplete-switch")
  private void dispatch(char c)
  {
    switch(this.state) {
    // TODO create WAIT_FOR_SELECTOR state before entering INSIDE_SELECTOR
    // for now selector parser takes care to trim starting white chars

    case INSIDE_SELECTOR:
      if(c == Chars.AT_RULE_MARK) {
        this.state = State.AT_RULE;
      }
      else {
        parseSelector(c);
      }
      break;

    case AT_RULE:
      if(c == Chars.AT_FONT_FACE_MARK) {
        this.state = State.FONT_FACE_RULE;
        parseFontFaceRule(c);
      }
      else if(c == Chars.AT_IMPORT_MARK) {
        this.state = State.IMPORT_RULE;
        parseImportRule(c);
      }
      else if(c == Chars.AT_KEY_FRAMES_MARK) {
        this.state = State.KEY_FRAMES_RULE;
        atRuleParser = new KeyFramesRuleImpl(c);
      }
      else if(c == Chars.BROWSER_SPECIFIC_MARK) {
        this.state = State.BROWSER_SPECIFIC;
        bracesCount = 0;
        ignoreBrowserSpecific(c);
      }
      else {
        // TODO perhaps to be more merciful and ignore unknown at rules
        throw new IllegalStateException();
      }
      break;

    case FONT_FACE_RULE:
      parseFontFaceRule(c);
      break;

    case IMPORT_RULE:
      parseImportRule(c);
      break;

    case KEY_FRAMES_RULE:
      assert atRuleParser != null;
      if(!atRuleParser.parse(c)) {
        styleSheet.addKeyFramesRule((KeyFramesRule)atRuleParser);
        state = State.INSIDE_SELECTOR;
        atRuleParser = null;
      }
      break;

    case INSIDE_COMMENT:
      ignoreComment(c);
      break;

    case INSIDE_PROPERTY_NAME:
      parsePropertyName(c);
      break;

    case INSIDE_VALUE:
      parseValue(c);
      break;

    case INSIDE_VALUE_ROUND_BRACKET:
      parseValueInsideRoundBrackets(c);
      break;

    case BROWSER_SPECIFIC:
      ignoreBrowserSpecific(c);
      break;
    }
  }

  private void parseValue(char c)
  {
    if(c == Chars.SEMI_COLON) {
      String property = this.propertyBuilder.toString().trim();
      if(property.charAt(0) == Chars.BROWSER_SPECIFIC_MARK) {
        this.propertyBuilder.setLength(0);
        this.valueBuilder.setLength(0);
        this.state = State.INSIDE_PROPERTY_NAME;
        return;
      }
      
      String value = this.valueBuilder.toString().trim();
      PropertyValueImpl propertyValue = new PropertyValueImpl(property, value);

      if(this.listener != null) {
        this.listener.onPropertyValue(propertyValue);
      }
      this.values.add(propertyValue);

      this.propertyBuilder.setLength(0);
      this.valueBuilder.setLength(0);
      this.state = State.INSIDE_PROPERTY_NAME;
      return;
    }

    if(c == Chars.LEFT_PARENTHESIS) {
      this.valueBuilder.append(Chars.LEFT_PARENTHESIS);
      this.state = State.INSIDE_VALUE_ROUND_BRACKET;
      return;
    }

    if(c == Chars.RIGHT_BRACE) {
      throw new CssException("The value |%s| for property |%s| in the selector |%s| should end with an ';', not with '}'.", this.valueBuilder, this.propertyBuilder, this.selectorBuilder);
    }
    this.valueBuilder.append(c);
  }

  private void parseValueInsideRoundBrackets(char c)
  {
    if(c == Chars.RIGHT_PARENTHESIS) {
      this.valueBuilder.append(Chars.RIGHT_PARENTHESIS);
      this.state = State.INSIDE_VALUE;
      return;
    }
    this.valueBuilder.append(c);
  }

  private void parsePropertyName(char c)
  {
    if(c == Chars.COLON) {
      this.state = State.INSIDE_VALUE;
      return;
    }

    if(c == Chars.SEMI_COLON) {
      throw new CssException("Unexpected char |%c| for property |%s| in the selector |%s| should end with an ';', not with '}'.", c, this.propertyBuilder, this.selectorBuilder);
    }

    if(c != Chars.RIGHT_BRACE) {
      this.propertyBuilder.append(c);
      return;
    }

    RulesetImpl ruleset = new RulesetImpl();
    for(String selector : this.selectorNames) {
      ruleset.addSelector(selector.trim());
    }
    this.selectorNames.clear();

    String selector = this.selectorBuilder.toString().trim();
    // TODO add selector listener here
    ruleset.addSelector(selector);
    this.selectorBuilder.setLength(0);

    for(PropertyValueImpl pv : this.values) {
      ruleset.addPropertyValue(pv);
    }
    this.values.clear();

    if(!ruleset.getPropertyValues().isEmpty()) {
      if(this.listener != null) {
        // TODO enable if add ruleset to listener
        // ruleset = (RulesetImpl)listener.onRuleset(ruleset);
      }
      if(ruleset != null) {
        this.styleSheet.addRuleset(ruleset);
      }
    }
    this.state = State.INSIDE_SELECTOR;
  }

  private void parseSelector(char c)
  {
    if(c == Chars.LEFT_BRACE) {
      this.state = State.INSIDE_PROPERTY_NAME;
      return;
    }

    if(c != Chars.COMMA) {
      this.selectorBuilder.append(c);
      return;
    }

    String selector = this.selectorBuilder.toString().trim();
    if(selector.isEmpty()) {
      throw new CssException("Found an ',' in a selector name without any actual name before it.");
    }

    this.selectorBuilder.setLength(0);
    this.selectorNames.add(selector);
  }

  private void parseFontFaceRule(char c)
  {
    this.atRuleBuilder.append(c);
    if(c != Chars.RIGHT_BRACE) {
      return;
    }

    FontFaceRule fontFaceRule = new FontFaceRuleImpl(this.atRuleBuilder.toString().trim());
    this.styleSheet.addFontFaceRule(fontFaceRule);
    this.atRuleBuilder.setLength(0);
    this.state = State.INSIDE_SELECTOR;
  }

  private void parseImportRule(char c)
  {
    if(c != Chars.SEMI_COLON) {
      this.atRuleBuilder.append(c);
      return;
    }

    ImportRule importRule = new ImportRuleImpl(this.atRuleBuilder.toString().trim());
    if(this.listener != null) {
      importRule = this.listener.onImportRule(importRule);
    }
    this.styleSheet.addImportRule(importRule);
    this.atRuleBuilder.setLength(0);
    this.state = State.INSIDE_SELECTOR;
  }

  private void ignoreComment(char c)
  {
    if(previousChar == Chars.STAR && c == Chars.SLASH) {
      state = savedState;
    }
  }

  private void ignoreBrowserSpecific(char c)
  {
    if(c == Chars.LEFT_BRACE) {
      bracesCount++;
    }
    else if(c == Chars.RIGHT_BRACE) {
      bracesCount--;
      if(bracesCount == 0) {
        state = State.INSIDE_SELECTOR;
      }
    }
  }
}
