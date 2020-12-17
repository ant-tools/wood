package js.wood.eval;

/**
 * Operator codes and factory for operator instances. This class provides implemented operator constants and related
 * operator instances. For every opcode constant there is an {@link Operator} instance retrievable using
 * {@link #instance()} getter.
 * <p>
 * Current implemented operators:
 * <table>
 * <tr>
 * <td><b>Opcode
 * <td><b>Operator Class
 * <td><b>Description
 * <td><b>Sample Syntax
 * <tr>
 * <td>{@link #ADD}
 * <td>{@link AddOperator}
 * <td>Generic sum for numeric values, numeric with measurement units and string concatenation
 * <td>(add 1 2 3)
 * </table>
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
enum Opcode
{
  /** Generic sum for numeric values, numeric with measurement units and string concatenation. */
  ADD,

  /** Arithmetic subtraction. */
  SUB,

  /** Arithmetic multiplication. */
  MUL;

  /** Current implemented operators. */
  private static Operator[] OPERATORS = new Operator[]
  {
      // !!! order should match opcode constants

      new AddOperator(), new SubOperator(), new MulOperator()
  };

  /**
   * Get the instance of the operator designated by opcode constant.
   * 
   * @return operator instance.
   */
  public Operator instance()
  {
    return OPERATORS[ordinal()];
  }

  /** The length of the largest operator name. This values is initialized on the fly by {@link #maxLength()}. */
  private static int maxLength;

  /**
   * Get the length of the largest operator name. Takes care to initialize {@link #maxLength} on the fly; for this
   * traverses all opcode names and keep the maximum length.
   * 
   * @return maximum length of operator name.
   * @see #maxLength
   */
  public static int maxLength()
  {
    if(maxLength == 0) {
      for(Opcode opcode : Opcode.values()) {
        if(maxLength < opcode.name().length()) {
          maxLength = opcode.name().length();
        }
      }
    }
    return maxLength;
  }
}