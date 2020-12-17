package js.wood;

import js.dom.Document;
import js.dom.EList;
import js.dom.Element;

/**
 * WOOD operators handler. This interface supplies methods for WOOD operators handling. An operator is a standard
 * element attribute with predefined name, see {@link Operator} for supported operators. At its core operators handling
 * is attribute searching, value retrieval and removing.
 * 
 * @author Iulian Rotaru
 */
public interface OperatorsHandler
{
  /**
   * Find all elements from document that possess the given operator.
   * 
   * @param document layout document,
   * @param operator operator to search for.
   * @return elements list, possible empty.
   * @throws IllegalArgumentException if <code>document</code> parameter is null.
   */
  EList findByOperator(Document document, Operator operator);

  /**
   * Returns all descendant elements possessing requested operator.
   * 
   * @param element layout element,
   * @param operator to search for.
   * @return descendant elements list, possible empty.
   * @throws IllegalArgumentException if <code>element</code> parameter is null.
   */
  EList findByOperator(Element element, Operator operator);

  /**
   * Get the first descendant element possessing requested operator.
   * 
   * @param element layout element,
   * @param operator operator to search for.
   * @return first descendant element with operator or null.
   * @throws IllegalArgumentException if <code>element</code> parameter is null.
   */
  Element getByOperator(Element element, Operator operator);

  /**
   * Get first document element that has operator with requested operand value.
   * 
   * @param document document,
   * @param operator operator to search for,
   * @param operand requested operand value.
   * @return first document element possessing the operator with requested operand value or null.
   * @throws IllegalArgumentException if <code>document</code> parameter is null.
   */
  Element getByOperator(Document document, Operator operator, String operand);

  /**
   * Get operator value, that is, its operand.
   * 
   * @param element layout element,
   * @param operator operator.
   * @return operand value.
   * @throws IllegalArgumentException if <code>element</code> parameter is null.
   */
  String getOperand(Element element, Operator operator);

  /**
   * Remove operator from layout element.
   * 
   * @param element layout element,
   * @param operator operator.
   * @throws IllegalArgumentException if <code>element</code> parameter is null.
   */
  void removeOperator(Element element, Operator operator);

}
