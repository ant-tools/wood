package js.wood.eval;

import js.util.Strings;

/**
 * An expression contains opcode and arguments, which arguments contain values and optional nested expressions.
 * <p>
 * Here is expression syntax; for now only ADD opcode is supported. The number of arguments is specific to every opcode.
 * 
 * <pre>
 * expression = OPEN opcode *arguments CLOSE
 * opcode     = ADD
 * arguments  = argument *(SP argument) 
 * argument   = literal | expression
 * literal    = 1*VCHAR
 *  
 * ; terminal symbols definition
 * OPEN  = "("    ; mark for expression open
 * CLOSE = ")"    ; mark for expression close
 * ADD   = "add"  ; generic sum for numeric values and string concatenation
 * 
 * ; SP and VCHAR are described by RFC5234, Appendix B.1
 * </pre>
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
class Expression
{
  /** Expression opening mark. */
  public static final char OPEN_MARK = '(';

  /** Expression closing mark. */
  public static final char CLOSE_MARK = ')';

  /** Expression operation code. */
  private Opcode opcode;

  /** Arguments list contains values and optional nested expressions. */
  private Object[] arguments;

  /** Cached value for string representation. */
  private String string;

  /**
   * Create immutable expression instance with given opcode and arguments.
   * 
   * @param opcode operation code,
   * @param arguments expression arguments.
   */
  public Expression(Opcode opcode, Object[] arguments)
  {
    this.opcode = opcode;
    this.arguments = arguments;
    this.string = Strings.concat(OPEN_MARK, opcode, Strings.join(arguments), CLOSE_MARK);
  }

  /**
   * Get expression operation code.
   * 
   * @return expression opcode.
   * @see #opcode
   */
  public Opcode getOpcode()
  {
    return opcode;
  }

  /**
   * Get expression arguments.
   * 
   * @return expression arguments.
   * @see #arguments
   */
  public Object[] getArguments()
  {
    return arguments;
  }

  /**
   * Get instance string representation.
   * 
   * @return string representation.
   */
  @Override
  public String toString()
  {
    return string;
  }
}