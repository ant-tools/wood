package js.wood.eval;

/**
 * Operator performs a primitive operation on given arguments and returns a value. An operator provides only a single
 * method that execute operator logic, see {@link #exec(String[])}.
 * <p>
 * Operator instances are designed to be reused as singletons and implementation should not store internal state, that
 * is, an operator provides only functional processing.
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
abstract class Operator
{
  /**
   * Execute operator logic on given arguments and return result. Implementation is free to throw illegal argument
   * exception if given arguments does not fit operator logic needs. All uses {@link EvalException} to signal erroneous
   * conditions.
   * 
   * @param arguments variable number of arguments.
   * @return result value.
   * @throws IllegalArgumentException if arguments does not suit operator logic.
   * @throws EvalException for any erroneous conditions.
   */
  public abstract String exec(String... arguments) throws IllegalArgumentException, EvalException;
}
