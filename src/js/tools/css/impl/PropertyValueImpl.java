package js.tools.css.impl;

import js.tools.css.PropertyValue;

final class PropertyValueImpl implements PropertyValue
{
  private String property;
  private String value;

  /**
   * Creates a new PropertyValue based on a property and its value.
   * 
   * @param property The CSS property (such as 'width' or 'color').
   * @param value The value of the property (such as '100px' or 'red').
   */
  PropertyValueImpl(String property, String value)
  {
    this.property = property;
    this.value = value;
  }

  public PropertyValueImpl(String property)
  {
    this.property = property;
  }

  @Override
  public String getProperty()
  {
    return this.property;
  }

  @Override
  public void setValue(String value)
  {
    this.value = value;
  }

  @Override
  public String getValue()
  {
    return this.value;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(this.property);
    sb.append(':');
    sb.append(this.value);
    return sb.toString();
  }

  @Override
  public boolean equals(Object obj)
  {
    if(this == obj) return true;
    if(obj == null) return false;
    if(getClass() != obj.getClass()) return false;
    PropertyValueImpl other = (PropertyValueImpl)obj;
    if(this.property == null) {
      if(other.property != null) return false;
    }
    else if(!this.property.equals(other.property)) return false;
    if(this.value == null) {
      if(other.value != null) return false;
    }
    else if(!this.value.equals(other.value)) return false;
    return true;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.property == null) ? 0 : this.property.hashCode());
    result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
    return result;
  }
}
