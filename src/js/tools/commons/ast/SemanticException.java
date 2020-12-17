package js.tools.commons.ast;

import org.mozilla.javascript.ast.AstNode;

/**
 * Unchecked exception thrown when source script does not follow j(s)-script dialect semantic.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class SemanticException extends RuntimeException
{
  /** Java serialization version. */
  private static final long serialVersionUID = -2320153216011555935L;

  /** Default constructor. */
  public SemanticException()
  {
    super();
  }

  /**
   * Create semantic exception with given message.
   * 
   * @param message exception message.
   */
  public SemanticException(String message)
  {
    super(message);
  }

  /**
   * Create semantic exception with formatted message.
   * 
   * @param format message format as supported by {@link String#format(String, Object...)},
   * @param args optional arguments is message contains formatting tags.
   */
  public SemanticException(String format, Object... args)
  {
    this(String.format(format, args));
  }

  /**
   * Create semantic exception with formatted message prefixed by script source line number.
   * 
   * @param astNode source Rhino AST node,
   * @param format message format as supported by {@link String#format(String, Object...)},
   * @param args optional arguments is message contains formatting tags.
   */
  public SemanticException(AstNode astNode, String format, Object... args)
  {
    this("[" + astNode.getLineno() + "]" + String.format(format, args));
  }

  /**
   * Create semantic exception with given root cause.
   * 
   * @param cause root cause.
   */
  public SemanticException(Throwable cause)
  {
    super(cause);
  }

  /**
   * Create semantic exception with given message and root cause.
   * 
   * @param message exception message,
   * @param cause root cause.
   */
  public SemanticException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
