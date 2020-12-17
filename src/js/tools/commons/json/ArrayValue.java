package js.tools.commons.json;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser array value.
 * 
 * @author Iulian Rotaru
 * @since 1.0.2
 */
final class ArrayValue extends Value
{
  /** Values parsed from JSON array. */
  private List<Object> values;

  /** Array type can be array class or generic array type. */
  private Type type;

  /** Array instance. */
  private Object instance;

  /**
   * Create parser array value helper for given type.
   * 
   * @param type array component type.
   */
  ArrayValue(Type type)
  {
    this.values = new ArrayList<Object>();
    this.type = type;
  }

  /**
   * Get array instance initialized from JSON characters stream.
   * 
   * @return array instance.
   */
  @Override
  public Object instance()
  {
    if(instance == null) {
      Class<?> arrayClass = null;
      Type arrayType = getType();
      if(arrayType instanceof Class) {
        arrayClass = (Class<?>)arrayType;
      }
      else {
        assert arrayType instanceof ParameterizedType;
        arrayClass = (Class<?>)((ParameterizedType)arrayType).getRawType();
      }

      instance = Array.newInstance(arrayClass, values.size());
      for(int i = 0; i < values.size(); i++) {
        Object value = values.get(i);
        Array.set(instance, i, Converter.toObject(value, arrayClass));
      }
    }
    return instance;
  }

  /**
   * Created Java array component type.
   * 
   * @return array component type.
   */
  @Override
  public Type getType()
  {
    if(type instanceof Class) {
      return ((Class<?>)type).getComponentType();
    }
    assert type instanceof GenericArrayType;
    return ((GenericArrayType)type).getGenericComponentType();
  }

  /**
   * Collect array parsed item from JSON characters stream.
   * 
   * @param value parsed array item.
   */
  @Override
  public void set(Object value)
  {
    values.add(Converter.toObject(value, type));
  }
}
