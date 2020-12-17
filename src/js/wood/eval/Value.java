package js.wood.eval;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Numeric value with measurement units. A value has a numeric quantity and an optional measurement units.
 * <p>
 * Value syntax is defined below. Numeric quantity is the standard signed decimal number. If number is positive sign is
 * optional. If value is a scalar units is an empty string.
 * 
 * <pre>
 * value             = ?(PLUS / MINUS) integer-part ?(SEP fractional-part) ?units
 * integer-part      = 1*DIGIT
 * fractional-part   = 1*DIGIT
 * units             = 1*ALPHA  ; default to empty string
 * 
 * ; terminal symbols definition
 * PLUS  = "+"                  ; sign for positive numbers, default value
 * MINUS = "-"                  ; sign for negative numbers
 * SEP   = "."                  ; decimal separator
 * 
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
class Value
{
  /** Regular expression constant for value argument parsing. */
  private static final Pattern PATTERN = Pattern.compile("^([-+]?[0-9]+(?:\\.[0-9]+)?)([a-z]*)$", Pattern.CASE_INSENSITIVE);

  /** Value numeric quantity. */
  private double quantity;

  /** Optional measurement units, default to empty string. */
  private String units;

  /**
   * Create immutable value instance from given argument. See class description for argument syntax.
   * 
   * @param argument value argument.
   * @throws IllegalArgumentException if <code>argument</code> syntax is not valid.
   */
  public Value(String argument) throws IllegalArgumentException
  {
    Matcher matcher = PATTERN.matcher(argument);
    if(!matcher.find()) {
      throw new IllegalArgumentException(String.format("Bad value argument |%s|.", argument));
    }
    this.quantity = Double.parseDouble(matcher.group(1));
    // units is empty string if not present into argument
    this.units = matcher.group(2);
  }

  /**
   * Get value numeric quantity.
   * 
   * @return value quantity.
   * @see #quantity
   */
  public double getQuantity()
  {
    return quantity;
  }

  /**
   * Get measurement units or empty string if value is scalar.
   * 
   * @return measurement units, possible empty.
   * @see #units
   */
  public String getUnits()
  {
    return units;
  }

  /**
   * Load values from given arguments and return discovered measurement unit, possible empty if not units found. Every
   * argument should obey syntax defined by this class description. All arguments should have the same measurement unit,
   * if present.
   * 
   * @param arguments arguments list, all with the same measurement unit,
   * @param values target values to load.
   * @return measurement units, possible empty.
   * @throws IllegalArgumentException if arguments have different measurement units.
   */
  public static String forArguments(String[] arguments, Value[] values) throws IllegalArgumentException
  {
    assert arguments.length == values.length;

    String units = "";
    for(int i = 0; i < arguments.length; ++i) {
      values[i] = new Value(arguments[i]);
      if(units.isEmpty()) {
        units = values[i].getUnits();
      }
      if(values[i].getUnits().isEmpty()) {
        continue;
      }
      if(!units.equals(values[i].getUnits())) {
        throw new IllegalArgumentException("Different measure units.");
      }
    }
    return units;
  }
}