package js.wood.eval;

import js.wood.WoodException;

/**
 * Interpreter is the core of the evaluation engine providing facade method {@link #evaluate(String, String)}. This
 * class parses expression value, executes operator with given arguments and returns resulting value. For allowed
 * expression syntax see {@link Expression}.
 * <p>
 * This class is all one need to evaluate an expression and in fact is the single one public, see sample code below.
 * 
 * <pre>
 * Interpreter interpreter = new Interpreter();
 * interpreter.evaluate(&quot;(add 1 2 3)&quot;, sourceFile);
 * </pre>
 * 
 * Note that interpreter instance is reusable, there is no need to create new instance for every evaluation.
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
public class Interpreter
{
  /**
   * Evaluation engine facade. This method parses given expression value and delegates {@link #evaluate(Expression)}.
   * Returns resulting value and throws exception if expression is not valid.
   * <p>
   * Source file parameter is optional and can be null. It is used for error tracking and not included in exception
   * message if null.
   * 
   * @param expression expression value to interpret,
   * @param sourceFile optional source file where expression is declared, for error tracking.
   * @return evaluation result.
   * @throws WoodException if expression is not valid.
   */
  public String evaluate(String expression, String sourceFile) throws WoodException
  {
    try {
      Parser parser = new Parser(expression);
      return evaluate(parser.parse());
    }
    catch(Exception e) {
      StringBuilder message = new StringBuilder("Invalid expression |");
      message.append(expression);
      message.append('|');
      if(sourceFile != null) {
        message.append(" in source file |");
        message.append(sourceFile);
        message.append('|');
      }
      message.append(". ");
      message.append(e.getMessage());
      throw new WoodException(message.toString());
    }
  }

  /**
   * Execute the actual evaluation process. An expression has an operator and a variable number of arguments, or
   * operands. At its core evaluation process is implemented by expression operator, see {@link Operator}. This method
   * delegates {@link Operator#exec(String[])} but first process recursively nested expression, in depth-first order.
   * This way, when outermost operator is executed all nested expression are evaluated.
   * 
   * @param expression parsed expression instance.
   * @return evaluation result.
   * @throws Exception any evaluation exception is bubbled up to caller, see {@link #evaluate(String, String)}.
   */
  private String evaluate(Expression expression) throws Exception
  {
    String[] arguments = new String[expression.getArguments().length];
    for(int i = 0; i < expression.getArguments().length; ++i) {
      Object argument = expression.getArguments()[i];
      if(argument instanceof Expression) {
        arguments[i] = evaluate((Expression)argument);
      }
      else {
        assert argument instanceof String;
        arguments[i] = (String)argument;
      }
    }

    return expression.getOpcode().instance().exec(arguments);
  }
}
