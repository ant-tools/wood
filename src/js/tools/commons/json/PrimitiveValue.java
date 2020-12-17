package js.tools.commons.json;

import java.lang.reflect.Type;

/**
 * Helper class for primitive value parsing.
 * 
 * @author Iulian Rotaru
 */
final class PrimitiveValue extends Value
{
  /** Primitive value class. */
  private Class<?> clazz;

  /** Primitive value boxing class. */
  private Object instance;

  /**
   * Create primitive value helper for given class.
   * 
   * @param clazz primitive value class.
   */
  PrimitiveValue(Class<?> clazz)
  {
    this.clazz = clazz;
  }

  @Override
  public Object instance()
  {
    return instance;
  }

  @Override
  public Type getType()
  {
    return clazz;
  }

  /**
   * Set this primitive value from string value.
   * 
   * @param value string value.
   * @throws JsonParserException if value is not a string.
   */
  @Override
  public void set(Object value)
  {
    instance = Converter.toObject(value, clazz);
  }
}
