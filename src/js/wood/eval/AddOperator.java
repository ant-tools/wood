package js.wood.eval;

/**
 * Generic sum for numeric values, numeric with measurement units and string concatenation. This class provides
 * implementation for {@link Opcode#ADD} operator code. It is a generic adder that adapt itself considering given
 * argument types. Note that, at the point this operator is invoked nested expressions are already resolved.
 * <p>
 * It syntax follow {@link Expression} grammar but result depends on argument type, see table below.
 * <table>
 * <tr>
 * <td><b>Expression
 * <td><b>Result
 * <td><b>Adder Method
 * <td><b>Note
 * <tr>
 * <td>(add 12 8)
 * <td>20
 * <td>{@link #addNumbers(String...)}
 * <td>all arguments are numeric values; perform arithmetic sum
 * <tr>
 * <td>(add 12 8px)
 * <td>20px
 * <td>{@link #addMeasures(String...)}
 * <td>there is a measure value and a number promoted to scalar measure; add values and preserve units
 * <tr>
 * <td>(add 12 8 px)
 * <td>128px
 * <td>{@link #addStrings(String...)}
 * <td>there is at least one argument that is not numeric nor measure value; consider all strings an do concatenation
 * </table>
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
class AddOperator extends Operator
{
  /**
   * Generic adder. This method is the facade for adder operator. It detects arguments list type and enact suitable
   * specialized adder method.
   * 
   * @param arguments variable number of arguments.
   * @return sum value.
   * @throws IllegalArgumentException if argument does not fit specialized adder needs.
   */
  @Override
  public String exec(String... arguments) throws IllegalArgumentException
  {
    switch(detectArgumentsType(arguments)) {
    case NUMBER:
      return addNumbers(arguments);

    case MEASURE:
      return addMeasures(arguments);

    case STRING:
      return addStrings(arguments);
    }

    throw new IllegalStateException();
  }

  // ------------------------------------------------------
  // Specialized adder methods

  /**
   * Execute numeric sum for arguments expected to be numeric values.
   * 
   * @param arguments numeric values.
   * @return numeric sum.
   * @throws IllegalArgumentException if found an argument with invalid number format.
   */
  private static String addNumbers(String... arguments) throws IllegalArgumentException
  {
    double value = 0.0;
    for(String argument : arguments) {
      value += parseNumber(argument);
    }
    return Double.toString(value);
  }

  /**
   * Add measure values with a common measurement unit. All given arguments should have the same measurement unit.
   * Scalar measure is accepted but at least one measure should have units; see {@link Measure#isScalar()} for scalar
   * definition.
   * 
   * @param arguments measure values.
   * @return the sum of given measure values.
   * @throws IllegalArgumentException if measure values have not the same measurement unit.
   */
  private static String addMeasures(String... arguments) throws IllegalArgumentException
  {
    double value = 0.0;
    String units = null;

    for(String argument : arguments) {
      Measure measure = new Measure(argument);
      value += measure.getValue();

      if(measure.isScalar()) {
        continue;
      }
      if(units == null) {
        units = measure.getUnits();
        continue;
      }

      // at this step both units variable and measure units are not null; they should be equal
      if(!units.equals(measure.getUnits())) {
        throw new IllegalArgumentException("Different measure units.");
      }
    }

    if(units == null) {
      throw new IllegalStateException("Null units although at least one argument of type measure was detected.");
    }
    return value + units;
  }

  /**
   * Concatenates string values.
   * 
   * @param arguments string values.
   * @return concatenated strings.
   */
  private static String addStrings(String... arguments)
  {
    StringBuilder builder = new StringBuilder();
    for(String argument : arguments) {
      builder.append(argument);
    }
    return builder.toString();
  }

  // ------------------------------------------------------
  // Utility methods

  /**
   * Detect the type of the add operator arguments list. This method consider every argument type and determine the type
   * of entire arguments list, as a whole. Returned type is used to choose the right processing method.
   * <p>
   * Uses next heuristic to detect arguments list type. Rules are executed in listed order.
   * <ol>
   * <li>if at least one argument is not number nor measure return {@link Type#STRING}
   * <li>if at least one argument is measure return {@link Type#MEASURE}
   * <li>if none above return {@link Type#NUMBER}
   * </ol>
   * 
   * @param arguments argument list.
   * @return type of arguments list.
   */
  private static Type detectArgumentsType(String... arguments)
  {
    boolean foundMeasure = false;
    for(String argument : arguments) {
      Type argumentType = Type.forArgument(argument);
      if(argumentType == Type.STRING) {
        return Type.STRING;
      }
      if(argumentType == Type.MEASURE) {
        foundMeasure = true;
      }
    }
    return foundMeasure ? Type.MEASURE : Type.NUMBER;
  }

  /**
   * Parse numeric argument and return value. Throw illegal argument if given argument is not a valid numeric value.
   * 
   * @param argument numeric argument.
   * @return argument value.
   * @throws IllegalArgumentException if argument format is not a valid number.
   */
  private static double parseNumber(String argument) throws IllegalArgumentException
  {
    try {
      return Double.parseDouble(argument);
    }
    catch(NumberFormatException e) {
      throw new IllegalArgumentException(String.format("Bad numeric argument |%s|.", argument));
    }
  }

  // ------------------------------------------------------
  // Utility classes

  /**
   * Type of argument used by add operator.
   * 
   * @author Iulian Rotaru
   * @since 1.1
   */
  private static enum Type
  {
    /** Numeric value. */
    NUMBER,

    /** Measure has value and units. */
    MEASURE,

    /** Plain string. */
    STRING;

    /**
     * Get the type of argument.
     * 
     * @param argument argument to get type for.
     * @return argument type.
     */
    public static Type forArgument(String argument)
    {
      if(!isNumber(argument.charAt(0))) {
        return STRING;
      }

      for(int i = 1; i < argument.length(); ++i) {
        if(!isNumber(argument.charAt(i))) {
          return MEASURE;
        }
      }

      return Type.NUMBER;
    }

    /**
     * Test if character is valid in a numeric value.
     * 
     * @param c character to test.
     * @return true if character is valid in a numeric value.
     */
    private static boolean isNumber(char c)
    {
      return Character.isDigit(c) || c == '-' || c == '+' || c == '.';
    }
  }
}
