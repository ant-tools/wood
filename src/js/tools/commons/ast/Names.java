package js.tools.commons.ast;

import js.tools.commons.util.Params;
import js.tools.commons.util.Strings;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.StringLiteral;

/**
 * Functions for extracting names from Rhino AST nodes. Depending on specific node type returned name can be qualified
 * or not.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class Names
{
  /**
   * Get Rhino AST node name. This method is a convenient alternative for {@link #getName(AstNode, String)} when there
   * is no value to append on name.
   * 
   * @param node source Rhino AST node.
   * @return node name.
   */
  public static String getName(AstNode node)
  {
    return getName(node, "");
  }

  /**
   * Get Rhino AST node name concatenated with given string value. Returned string is node name and given value joined
   * by dot; it can be seen as value qualified by node name. If node is null this method simply returns given value.
   * 
   * @param node source Rhino AST node, null accepted,
   * @param value string value to append to node name.
   * @return given string value qualified by the Rhino AST node name.
   */
  public static String getName(AstNode node, String value)
  {
    Params.notNull(value, "Value");
    if(node == null) {
      return value;
    }
    switch(node.getType()) {
    case Token.STRING:
      StringLiteral string = (StringLiteral)node;
      return add(value, string.getValue());

    case Token.NUMBER:
      NumberLiteral number = (NumberLiteral)node;
      return add(value, number.getValue());

    case Token.NAME:
      Name name = (Name)node;
      return add(value, name.getIdentifier());

    case Token.THIS:
      return value;

    case Token.GETPROP:
      PropertyGet propertyGet = (PropertyGet)node;
      value = add(value, getName(propertyGet.getTarget(), value));
      return getName(propertyGet.getProperty(), value);

    case Token.GETELEM:
      ElementGet elementGet = (ElementGet)node;
      // construct syntax example: node[js.dom.Node._BACK_REF]
      // where node is target and element is js.dom.Node._BACK_REF
      // right now i do not see why target should be included into name value
      // hence the next comment out
      //
      // value = add(value, getName(elementGet.getTarget(), value));

      return getName(elementGet.getElement(), value);

    case Token.COLON:
      ObjectProperty objectProperty = (ObjectProperty)node;
      return add(value, getName(objectProperty.getLeft(), value));

    case Token.LP:
      return add(value, "anonymous");

    case Token.CALL:
      FunctionCall functionCall = (FunctionCall)node;
      return getName(functionCall.getTarget(), value);

    case Token.FUNCTION:
      FunctionNode functionNode = (FunctionNode)node;
      return getName(functionNode.getFunctionName(), value);
    }
    // throw new IllegalStateException("Unprocessed name component: " + node.getClass().toString());
    return value;
  }

  /**
   * Concatenate two strings joined by a dot. If first string is empty returns only the second one.
   * 
   * @param s first string,
   * @param v second string.
   * @return concatenated strings.
   */
  private static String add(String s, String v)
  {
    return s.isEmpty() ? v : Strings.concat(s, '.', v);
  }
}
