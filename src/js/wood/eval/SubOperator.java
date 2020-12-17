package js.wood.eval;

import js.util.Params;

/**
 * Subtract one or more values, with optional measurement units from a given value.
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
class SubOperator extends Operator
{
  /**
   * Execute arithmetic subtract operation on given arguments list. Arguments may have optional measurement units, see
   * {@link Value} for valid syntax. Anyway, if present, all arguments should have the same units.
   * <p>
   * This operator implementation gets a variable number arguments but not less than two. First argument is always the
   * minuend from which the others, the subtrahends are subtracted.
   * 
   * <pre>
   *    (sub 20px 2px) -> 18px
   *    (sub 20 2px)   -> 18px
   *    (sub 20 4 2px) -> 14px
   *    (sub 20 2)     -> 18
   *    (sub 20em 2px) -> illegal
   * </pre>
   * 
   * @param arguments variable number or arguments, first the minuend.
   * @return subtract result with optional measurement units.
   * @throws IllegalArgumentException if arguments count is less than two or have different measurement units.
   */
  @Override
  public String exec(String... arguments) throws IllegalArgumentException
  {
    Params.GTE(arguments.length, 2, "Arguments count");

    Value[] values = new Value[arguments.length];
    String units = Value.forArguments(arguments, values);

    double result = values[0].getQuantity();
    for(int i = 1; i < values.length; ++i) {
      result -= values[i].getQuantity();
    }

    return Number.format(result) + units;
  }
}
