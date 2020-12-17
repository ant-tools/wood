package js.tools.commons.ast;

import org.mozilla.javascript.Node;

/**
 * Specific processing on a Rhino AST node used in conjunction with scanning process, see {@link Scanner}.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public abstract class AstHandler
{
  /** Scanner logger to standard console streams. */
  protected Log log;

  /**
   * Bind scanner logger to current AST handler instance.
   * 
   * @param log scanner logger.
   */
  public void setLog(Log log)
  {
    this.log = log;
  }

  /**
   * Execute specific processing on given Rhino AST node. This method is invoked by {@link Scanner} while traversing AST
   * nodes tree.
   * 
   * @param node Rhino AST node.
   */
  public abstract void handle(Node node);
}
