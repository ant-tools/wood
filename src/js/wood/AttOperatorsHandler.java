package js.wood;

import js.dom.Document;
import js.dom.EList;
import js.dom.Element;
import js.tools.commons.util.Params;

/**
 * Operators handler implementation for operators with simple attribute name.
 * 
 * @author Iulian Rotaru
 */
public class AttOperatorsHandler implements OperatorsHandler
{
  @Override
  public EList findByOperator(Document document, Operator operator)
  {
    Params.notNull(document, "Layout document");
    return document.findByXPath(buildAttrXPath(operator.value()));
  }

  @Override
  public EList findByOperator(Element element, Operator operator)
  {
    Params.notNull(element, "Layout element");
    return element.findByXPath(buildAttrXPath(operator.value()));
  }

  @Override
  public Element getByOperator(Element element, Operator operator)
  {
    Params.notNull(element, "Layout element");
    return element.getByXPath(buildAttrXPath(operator.value()));
  }

  @Override
  public String getOperand(Element element, Operator operator)
  {
    Params.notNull(element, "Layout element");
    return element.getAttr(operator.value());
  }

  @Override
  public Element getByOperator(Document document, Operator operator, String operand)
  {
    Params.notNull(document, "Layout document");
    return document.getByXPath(buildAttrXPath(operator.value(), operand));
  }

  @Override
  public void removeOperator(Element element, Operator operator)
  {
    Params.notNull(element, "Layout element");
    element.removeAttr(operator.value());
  }

  /**
   * Create XPath expression for selecting element by attribute name and optional value.
   * 
   * @param name attribute name,
   * @param value attribute value.
   * @return XPath expression.
   */
  protected static String buildAttrXPath(String name, String... value)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("//*[@");
    sb.append(name);
    if(value.length == 1) {
      sb.append("='");
      sb.append(value[0]);
      sb.append("'");
    }
    sb.append("]");
    return sb.toString();
  }
}
