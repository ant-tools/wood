package js.tools.css;

import java.util.List;

/**
 * A rule set has a selector and a list of property / values.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public interface Ruleset
{
  List<String> getSelectors();

  List<PropertyValue> getPropertyValues();
}