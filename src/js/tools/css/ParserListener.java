package js.tools.css;

public interface ParserListener
{
  ImportRule onImportRule(ImportRule importRule);

  PropertyValue onPropertyValue(PropertyValue propertyValue);
}
