package js.wood.eval;

/**
 * A measure has a numeric value and measurement units. Anyway, measurement units can be missing in which case measure
 * instance is a scalar.
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
class Measure
{
  /** Measure numeric value. */
  private double value;

  /** Measurement unit. */
  private String units;

  /**
   * Parse given measure value and initialize immutable instance state.
   * 
   * @param measure measure value.
   */
  public Measure(String measure)
  {
    StringBuilder builder = new StringBuilder();
    for(int i = 0; i < measure.length(); ++i) {
      char c = measure.charAt(i);
      if(!Character.isDigit(c) && c != '.') {
        this.units = measure.substring(i).trim();
        break;
      }
      builder.append(c);
    }

    try {
      this.value = Double.parseDouble(builder.toString());
    }
    catch(NumberFormatException unused) {
      throw new IllegalArgumentException(String.format("Invalid measure value format |%s|.", measure));
    }
  }

  /**
   * Get measure value.
   * 
   * @return measure value.
   * @see #value
   */
  public double getValue()
  {
    return value;
  }

  /**
   * Test if this measure instance is a scalar, that is, has no units.
   * 
   * @return true if this measure is a scalar.
   */
  public boolean isScalar()
  {
    return units == null;
  }

  /**
   * Get measurement units.
   * 
   * @return measurement units.
   * @see #units
   */
  public String getUnits()
  {
    return units;
  }
}