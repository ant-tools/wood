package js.tools.css.impl;

enum State
{
  /**
   * Inside a selector
   */
  INSIDE_SELECTOR,

  SLASH_DETECTED,

  AT_RULE,

  /** Processing a comment. */
  INSIDE_COMMENT,

  /** Inside a property name. */
  INSIDE_PROPERTY_NAME,

  /** Inside property value. */
  INSIDE_VALUE,

  /** Inside value and also inside open rounded bracket. */
  INSIDE_VALUE_ROUND_BRACKET,

  /** Processing font face AT rule */
  FONT_FACE_RULE,

  /** Import AT rule. */
  IMPORT_RULE,

  /** Key frames AT rule. */
  KEY_FRAMES_RULE,

  /** Browser specific rules start with dash. */
  BROWSER_SPECIFIC
}
