package js.wood;

import js.dom.Document;
import js.dom.EList;
import js.dom.Element;
import js.util.Params;

/**
 * Operators handler for operator names prefixed with <code>data-</code>, HTML custom attribute prefix. This operators
 * handler is used when developer select {@link NamingStrategy#DATA_ATTR} naming strategy.
 * 
 * @author Iulian Rotaru
 */
public class DataAttrOperatorsHandler extends AttOperatorsHandler
{
  /** Prefix for custom HTML attribute. */
  private static final String DATA_PREFIX = "data-";

  @Override
  public EList findByOperator(Document document, Operator operator)
  {
    Params.notNull(document, "Layout document");
    return document.findByXPath(buildAttrXPath(DATA_PREFIX + operator.value()));
  }

  @Override
  public EList findByOperator(Element element, Operator operator)
  {
    Params.notNull(element, "Layout element");
    return element.findByXPath(buildAttrXPath(DATA_PREFIX + operator.value()));
  }

  @Override
  public Element getByOperator(Element element, Operator operator)
  {
    Params.notNull(element, "Layout element");
    return element.getByXPath(buildAttrXPath(DATA_PREFIX + operator.value()));
  }

  @Override
  public String getOperand(Element element, Operator operator)
  {
    Params.notNull(element, "Layout element");
    return element.getAttr(DATA_PREFIX + operator.value());
  }

  @Override
  public Element getByOperator(Document document, Operator operator, String operand)
  {
    Params.notNull(document, "Layout document");
    return document.getByXPath(buildAttrXPath(DATA_PREFIX + operator.value(), operand));
  }

  @Override
  public void removeOperator(Element element, Operator operator)
  {
    Params.notNull(element, "Layout element");
    element.removeAttr(DATA_PREFIX + operator.value());
  }
}
