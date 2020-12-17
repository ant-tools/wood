package js.wood.eval;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Numeric value formatter.
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
class Number
{
  private static final NumberFormat FORMAT = NumberFormat.getNumberInstance(Locale.getDefault());
  static {
    FORMAT.setGroupingUsed(false);
  }

  /**
   * Format numeric value.
   * 
   * @param number numeric value.
   * @return number string representation.
   */
  public static String format(double number)
  {
    return FORMAT.format(number);
  }
}
