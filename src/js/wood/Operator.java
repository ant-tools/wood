package js.wood;

import java.util.Locale;

/**
 * WOOD operators. Operators are used by layout files to declare component relations.
 * 
 * @author Iulian Rotaru
 */
public enum Operator
{
  /** Define editable area into templates. */
  EDITABLE,
  /** Inject template content into editable area. */
  TEMPLATE,
  /** Insert widget layout into calling scope. */
  WIDGET,
  /** Used in conjunction with {@link #WIDGET} and {@link #TEMPLATE} to define layout parameters list. */
  PARAM,
  /** Declare script class, especially page class, to include into generated page. */
  SCRIPT;

  public String value()
  {
    return name().toLowerCase(Locale.getDefault());
  }
}
