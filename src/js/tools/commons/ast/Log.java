package js.tools.commons.ast;

import org.mozilla.javascript.Node;

/**
 * Logging facility for scanner and AST handlers. This logger send warnings to standard error and debug prints to
 * standard out. Warning messages contains file name and line number for source j(s)-script file.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class Log
{
  /** Warning message index kept updated at every displayed warn. */
  private int warnIndex = 0;
  /** Source file path included into warning message. */
  private String source;
  /** Currently processing Rhino AST node for source line number. */
  private Node node;

  /**
   * Set source file path for currently processing j(s)-script.
   * 
   * @param source source file path.
   */
  public void setCurrentSource(String source)
  {
    this.source = source;
  }

  /**
   * Set currently processing Rhino AST node. Given node is alive, in the sense that its internal line number is
   * updating while externally parsed. It is used to inject source line number into warning message.
   * 
   * @param node currently processing node.
   */
  public void setCurrentNode(Node node)
  {
    this.node = node;
  }

  /**
   * Send formatted warning message to standard error stream. Printed message contains, beside formatted message, an
   * warning index and the source file path and line number.
   * 
   * @param format formatted message as supported by {@link String#format(String, Object...)},
   * @param args optional arguments if message contains formatted tags.
   */
  public void warn(String format, Object... args)
  {
    System.err.println(String.format("[%d][%s:%d] %s", ++warnIndex, source, node.getLineno(), String.format(format, args)));
  }

  /**
   * Utility to send a single character to standard output stream.
   * 
   * @param c char to print to standard out.
   */
  public void print(char c)
  {
    System.out.print(c);
  }

  /**
   * Write a debug message to standard output stream but does not append line end.
   * 
   * @param message debug message to write.
   */
  public void print(String message)
  {
    System.out.print(message);
  }

  /**
   * Write a debug message to standard output and move to next line.
   * 
   * @param message debug message.
   */
  public void println(String message)
  {
    System.out.println(message);
  }
}
