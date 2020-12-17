package js.tools.commons.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import js.tools.commons.BugError;
import js.tools.commons.util.Classes;

/**
 * Parser helper class for values of type map. This class helps creating map instance of proper type and adding entries.
 * <p>
 * Maps are processed as objects on JSON streams and object property name is always string. For this reason map keys are
 * always string. Anyway, map value can have any type.
 * 
 * @author Iulian Rotaru
 */
final class MapValue extends ObjectValue
{
  /** Default implementations for maps interfaces. */
  private static Map<Class<?>, Class<?>> MAPS = new HashMap<Class<?>, Class<?>>();
  static {
    Map<Class<?>, Class<?>> m = new HashMap<Class<?>, Class<?>>();
    m.put(Map.class, HashMap.class);
    m.put(HashMap.class, HashMap.class);
    m.put(SortedMap.class, TreeMap.class);
    m.put(TreeMap.class, TreeMap.class);
    m.put(Hashtable.class, Hashtable.class);
    m.put(Properties.class, Properties.class);
    MAPS = Collections.unmodifiableMap(m);
  }

  /** Map key type initialized from constructor map type, first actual type argument. */
  private Class<?> keyType;

  /** Map value type initialized from constructor map type, second actual type argument. */
  private Class<?> valueType;

  /** Key instance for currently, on working entry. */
  private Object key;

  /**
   * Construct map value helper instance for given map type. This constructor argument is the map type but should be a
   * parameterized type in order to find out map raw class and value type. Constructor enact sanity checks on given map
   * <code>type</code>, uses its raw type to instantiate map and second type argument to initialize {@link #valueType}.
   * 
   * @param type parameterized map type.
   * @throws JsonParserException if <code>type</code> is not parameterized, first type argument is not string or second
   *           is missing.
   */
  MapValue(Type type)
  {
    super();

    if(!(type instanceof ParameterizedType)) {
      throw new JsonException("This JSON parser mandates generic maps usage but got |%s|.", type);
    }

    ParameterizedType parameterizedType = (ParameterizedType)type;
    Class<?> rawType = (Class<?>)parameterizedType.getRawType();

    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
    if(actualTypeArguments.length != 2) {
      throw new JsonException("Invalid map generic arguments list. Need 2 arguments but got |%d|.", actualTypeArguments.length);
    }

    keyType = (Class<?>)actualTypeArguments[0];
    valueType = (Class<?>)actualTypeArguments[1];

    Class<?> implementation = MAPS.get(rawType);
    if(implementation == null) {
      throw new BugError("No registered implementation for map |%s|.", type);
    }
    instance = Classes.newInstance(implementation);
  }

  /**
   * Get map key type.
   * 
   * @return map key type.
   * @see #keyType
   */
  public Type keyType()
  {
    return keyType;
  }

  /**
   * Get map value type.
   * 
   * @return map value type.
   * @see #valueType
   */
  @Override
  public Type getValueType()
  {
    return valueType;
  }

  /**
   * Store the key instance for on working entry. Stored key is used but a sequential {@link #setValue(Object)}.
   * 
   * @param key entry key.
   * @see #key
   */
  public void setKey(Object key)
  {
    this.key = key;
  }

  /**
   * Add map entry, overriding existing one if happen to have the same key name. This method uses {@link #key} stored by
   * a previous call to {@link #setKey(Object)}; together with given <code>value</code> creates a new entry that is
   * stored into map.
   * 
   * @param value map value.
   */
  @SuppressWarnings(
  {
      "unchecked", "rawtypes"
  })
  @Override
  public void setValue(Object value)
  {
    ((Map)instance).put(Converter.toObject(key, keyType), Converter.toObject(value, valueType));
  }
}
