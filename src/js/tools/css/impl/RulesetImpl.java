package js.tools.css.impl;

import java.util.ArrayList;
import java.util.List;

import js.tools.css.PropertyValue;
import js.tools.css.Ruleset;

final class RulesetImpl implements Ruleset
{
  private List<String> selectors;
  private List<PropertyValue> propertyValues;

  RulesetImpl(String selector)
  {
    this();
    this.selectors.add(selector);
  }

  RulesetImpl()
  {
    this.selectors = new ArrayList<String>();
    this.propertyValues = new ArrayList<PropertyValue>();
  }

  @Override
  public List<PropertyValue> getPropertyValues()
  {
    return this.propertyValues;
  }

  @Override
  public List<String> getSelectors()
  {
    return this.selectors;
  }

  void addPropertyValue(final PropertyValueImpl propertyValue)
  {
    this.propertyValues.add(propertyValue);
  }

  void addSelector(String selector)
  {
    this.selectors.add(selector);
  }
}
