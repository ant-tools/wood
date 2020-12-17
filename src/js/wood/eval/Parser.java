package js.wood.eval;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * Parse source expression and returns expression instance. This parser implements expression syntax as described by
 * {@link Expression expression grammar}.
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
class Parser
{
  /** Source expression iterator. */
  private CharacterIterator iterator;

  /** Current processed expression character. */
  private char expressionChar;

  /**
   * Create instance and initialize expression iterator and current processing character to first.
   * 
   * @param expression source expression string.
   */
  public Parser(String expression)
  {
    this.iterator = new StringCharacterIterator(expression);
    this.expressionChar = iterator.first();
  }

  /**
   * Execute parsing loop on internal iterator, initialized on source expression value. Expression source can contain
   * nested expression. This method is executed recursively for every expression nesting level.
   * 
   * @return expression instance.
   * @throws EvalException if parsing fails for whatever reason.
   */
  public Expression parse() throws EvalException
  {
    // store parser automaton state on this method stack
    // this method is also invoked recursively for nested expressions
    // and every expression iteration should preserve its own state
    State state = State.OPEN_EXPRESSION;

    OpcodeBuilder opcodeBuilder = new OpcodeBuilder();
    ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder();

    ITERATOR_LOOP: while(expressionChar != CharacterIterator.DONE) {

      switch(state) {
      case OPEN_EXPRESSION:
        if(expressionChar == Expression.OPEN_MARK) {
          state = State.OPCODE;
        }
        break;

      case OPCODE:
        if(expressionChar == Expression.CLOSE_MARK) {
          break ITERATOR_LOOP;
        }
        if(!Character.isWhitespace(expressionChar)) {
          opcodeBuilder.addChar(expressionChar);
          break;
        }
        state = State.ARGUMENTS;
        break;

      case WHITE_SPACE:
        if(Character.isWhitespace(expressionChar)) {
          break;
        }
        state = State.ARGUMENTS;
        // fall through ARGUMENTS with first not white space character

      case ARGUMENTS:
        if(expressionChar == Expression.OPEN_MARK) {
          // call recursively this parse method to get nested expression and store it in arguments list
          argumentsBuilder.addExpression(parse());
          state = State.WHITE_SPACE;
          break;
        }
        if(expressionChar == Expression.CLOSE_MARK) {
          argumentsBuilder.flush();
          break ITERATOR_LOOP;
        }
        argumentsBuilder.addChar(expressionChar);
      }

      expressionChar = iterator.next();
    }

    return new Expression(opcodeBuilder.getValue(), argumentsBuilder.getValue());
  }

  /**
   * States set for parser finite automaton.
   * 
   * @author Iulian Rotaru
   * @since 1.1
   */
  private static enum State
  {
    /** Wait for expression opening parenthesis. */
    OPEN_EXPRESSION,

    /** Collect opcode. */
    OPCODE,

    /** Collect arguments and call recursively parser loop for nested expressions. */
    ARGUMENTS,

    /** Skip white spaces. */
    WHITE_SPACE
  }
}