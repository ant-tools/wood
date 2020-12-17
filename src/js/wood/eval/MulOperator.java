package js.wood.eval;

import js.util.Params;

/**
 * Multiply two or many numeric values, with optional measurement units.
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
class MulOperator extends Operator
{
  /**
   * Execute arithmetic multiply operation on given arguments list. Arguments may have optional measurement units, see
   * {@link Value} for valid syntax. Anyway, if present, all arguments should have the same units.
   * <p>
   * This operator implementation gets a variable number arguments but not less than two. Apply multiply operation on
   * all. Since multiplication is commutative arguments order is not relevant.
   * 
   * <pre>
   *    (mul 2px 20px) -> 40px
   *    (mul 2 20px)   -> 40px
   *    (mul 2 4 20px) -> 160px
   *    (mul 2 20)     -> 40
   *    (mul 2em 20px) -> illegal
   * </pre>
   * 
   * @param arguments variable number or arguments.
   * @return multiplication result with optional measurement units.
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
      result *= values[i].getQuantity();
    }

    return Number.format(result) + units;
  }
}
