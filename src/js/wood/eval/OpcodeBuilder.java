package js.wood.eval;

import js.util.Strings;

/**
 * Builder for operation code. This class is designed to work with {@link Parser}. It just accumulates characters
 * supplied by parser, while parser is reading expression opcode.
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
class OpcodeBuilder
{
  /** Opcode value builder. */
  private StringBuilder builder = new StringBuilder();

  /**
   * Append character to this opcode builder but takes care to not exceed {@link Opcode#maxLength()}.
   * 
   * @param c opcode character.
   * @throws EvalException if accumulated characters number exceed {@link Opcode#maxLength()}.
   */
  public void addChar(char c) throws EvalException
  {
    if(c == ',') {
      throw new EvalException("Attempt to use comma (,) as expression separator.");
    }
    if(builder.length() > Opcode.maxLength()) {
      throw new EvalException("Opcode too long.");
    }
    builder.append(c);
  }

  /**
   * Return the opcode constant for this builder accumulated value. Throws exception if this builder value does not
   * designate a valid opcode constant. See {@link Opcode} for valid values.
   * 
   * @return opcode constant.
   * @throws EvalException if this builder value does not designates a valid opcode.
   * @see Opcode
   */
  public Opcode getValue() throws EvalException
  {
    try {
      return Opcode.valueOf(builder.toString().toUpperCase());
    }
    catch(IllegalArgumentException unused) {
      throw new EvalException("Not implemented opcode |%s|. Current implementation supports |%s|.", builder.toString().toUpperCase(), Strings.join(Opcode.values(), ", "));
    }
  }
}