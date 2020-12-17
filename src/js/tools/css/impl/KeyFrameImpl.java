package js.tools.css.impl;

import java.util.ArrayList;
import java.util.List;

import js.tools.css.KeyFrame;
import js.tools.css.PropertyValue;

public class KeyFrameImpl implements KeyFrame
{
  private List<Integer> percents;
  private List<PropertyValue> propertyValues = new ArrayList<PropertyValue>();

  public KeyFrameImpl(List<Integer> percents)
  {
    this.percents = percents;
  }

  @Override
  public List<Integer> getPercents()
  {
    return percents;
  }

  @Override
  public List<PropertyValue> getPropertyValues()
  {
    return propertyValues;
  }
}
