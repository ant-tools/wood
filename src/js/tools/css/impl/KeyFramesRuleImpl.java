package js.tools.css.impl;

import java.util.ArrayList;
import java.util.List;

import js.tools.commons.util.Strings;
import js.tools.css.KeyFrame;
import js.tools.css.KeyFramesRule;

/**
 * <pre>
 * KEY_FAMES = AT 'keyframes' SP ANIMATION_NAME SP '{' SP (SP PERCENTAGE SP '{' SP (SP STYLE SP ':' SP VALUE ';')* SP '}' )+ SP '}'
 * AT = '@'
 * SP (* white space *)
 * STYLES = '{' SP (SP STYLE SP ':' SP VALUE ';')* SP '}'
 * </pre>
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
public class KeyFramesRuleImpl implements KeyFramesRule, AtRuleParser
{
  private StringBuilder animationName = new StringBuilder();
  private List<KeyFrame> keyFrames = new ArrayList<KeyFrame>();
  private State state;
  private StringBuilder token = new StringBuilder();
  private KeyFrameImpl keyFrame;
  private PropertyValueImpl propertyValue;

  public KeyFramesRuleImpl(char c)
  {
    state = State.WAIT_SPACE;
  }

  @Override
  public boolean parse(char c)
  {
    switch(state) {
    case WAIT_SPACE:
      if(Character.isWhitespace(c)) {
        state = State.WAIT_ANIMATION_NAME;
      }
      break;

    case WAIT_ANIMATION_NAME:
      if(!Character.isWhitespace(c)) {
        state = State.ANIMATION_NAME;
        animationName.append(c);
      }
      break;

    case ANIMATION_NAME:
      if(c == Chars.LEFT_BRACE) {
        state = State.WAIT_PERCENT;
        break;
      }
      if(!Character.isWhitespace(c)) {
        animationName.append(c);
      }
      break;

    case WAIT_PERCENT:
      if(c == Chars.RIGHT_BRACE) {
        return false;
      }
      if(!Character.isWhitespace(c)) {
        state = State.PERCENT;
        token.setLength(0);
        token.append(c);
      }
      break;

    case PERCENT:
      if(c == Chars.LEFT_BRACE) {
        List<String> values = Strings.split(token.toString(), ',');
        List<Integer> percents = new ArrayList<Integer>();
        for(String value : values) {
          percents.add(Integer.parseInt(value.substring(0, value.length() - 1)));
        }
        token.setLength(0);

        keyFrame = new KeyFrameImpl(percents);
        keyFrames.add(keyFrame);
        state = State.WAIT_PROP_NAME;
        break;
      }
      if(!Character.isWhitespace(c)) {
        token.append(c);
      }
      break;

    case WAIT_PROP_NAME:
      if(c == Chars.RIGHT_BRACE) {
        state = State.WAIT_PERCENT;
        break;
      }
      if(c == Chars.BROWSER_SPECIFIC_MARK) {
        state = State.IGNORE_PROP;
        break;
      }
      if(!Character.isWhitespace(c)) {
        state = State.PROP_NAME;
        token.setLength(0);
        token.append(c);
      }
      break;

    case PROP_NAME:
      if(c == Chars.COLON) {
        propertyValue = new PropertyValueImpl(token.toString());
        keyFrame.getPropertyValues().add(propertyValue);
        state = State.WAIT_PROP_VALUE;
      }
      if(!Character.isWhitespace(c)) {
        token.append(c);
      }
      break;

    case WAIT_PROP_VALUE:
      if(!Character.isWhitespace(c)) {
        state = State.PROP_VALUE;
        token.setLength(0);
        token.append(c);
      }
      break;

    case PROP_VALUE:
      if(c == Chars.SEMI_COLON) {
        propertyValue.setValue(token.toString());
        state = State.WAIT_PROP_NAME;
      }
      token.append(c);
      break;
      
    case IGNORE_PROP:
      if(c == Chars.SEMI_COLON) {
        state = State.WAIT_PROP_NAME;
      }
      break;
    }

    return true;
  }

  @Override
  public String getAnimationName()
  {
    return animationName.toString();
  }

  @Override
  public List<KeyFrame> getKeyFrames()
  {
    return keyFrames;
  }

  private static enum State
  {
    WAIT_SPACE, WAIT_ANIMATION_NAME, ANIMATION_NAME, WAIT_PERCENT, PERCENT, WAIT_PROP_NAME, PROP_NAME, WAIT_PROP_VALUE, PROP_VALUE, IGNORE_PROP
  }
}
