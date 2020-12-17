package js.tools.commons.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import js.tools.commons.BugError;
import js.tools.commons.util.Classes;

/**
 * Parser collection value.
 * 
 * @author Iulian Rotaru
 * @since 1.0.2
 */
final class CollectionValue extends Value
{
  /** Default implementations for collection interfaces. */
  private static Map<Class<?>, Class<?>> COLLECTIONS = new HashMap<Class<?>, Class<?>>();
  static {
    Map<Class<?>, Class<?>> m = new HashMap<Class<?>, Class<?>>();
    m.put(Collection.class, Vector.class);
    m.put(List.class, ArrayList.class);
    m.put(ArrayList.class, ArrayList.class);
    m.put(Vector.class, Vector.class);
    m.put(Set.class, HashSet.class);
    m.put(HashSet.class, HashSet.class);
    m.put(TreeSet.class, TreeSet.class);
    COLLECTIONS = Collections.unmodifiableMap(m);
  }

  /** Collection instance. */
  private Collection<Object> instance;

  /** The actual type of collection parameterized type. */
  private Type type;

  /**
   * Construct parser collection with elements of given type.
   * 
   * @param type collection elements type.
   */
  @SuppressWarnings("unchecked")
  CollectionValue(Type type)
  {
    if(!(type instanceof ParameterizedType)) {
      throw new JsonException("This JSON parser mandates generic collections usage but got |%s|.", type);
    }

    ParameterizedType parameterizedType = (ParameterizedType)type;
    Class<?> rawType = (Class<?>)parameterizedType.getRawType();

    this.type = parameterizedType.getActualTypeArguments()[0];

    Class<?> implementation = COLLECTIONS.get(rawType);
    if(implementation == null) {
      throw new BugError("No registered implementation for collection |%s|.", type);
    }
    instance = (Collection<Object>)Classes.newInstance(implementation);

  }

  /**
   * Get collection instance. Since this method is invoked after {@link #set(Object)} returned collection is initialized
   * from JSON characters stream.
   * 
   * @return collection instance.
   */
  @Override
  public Object instance()
  {
    return instance;
  }

  /**
   * Get the actual type of this collection parameterized type.
   * 
   * @return collection component type.
   */
  @Override
  public Type getType()
  {
    return type;
  }

  /**
   * Collect parsed item from JSON characters stream.
   * 
   * @param value parsed collection item.
   */
  @Override
  public void set(Object value)
  {
    instance.add(Converter.toObject(value, type));
  }
}
