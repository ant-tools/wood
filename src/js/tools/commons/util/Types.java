package js.tools.commons.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import js.tools.commons.BugError;

/**
 * Duck typing predicates. This utility class supplies predicates for type discovery at runtime; it is inspired by
 * <code>JavaScript</code> paradigm. Although uncommon for utility classes this one allows for sub-classing, see sample
 * code.
 * 
 * <pre>
 * class Types extends js.util.Types {
 *  public static boolean isWunderObject(Object o) {
 *      return o instanceof WunderObject;
 *  }
 * }
 * ...
 * if(Types.isInstanceOf(object, StandardObject.class)) { // predicate provided by base class
 *  ...
 * }
 * if(Types.isWunderObject(object)) { // predicate provided by extension
 *  ...     
 * }
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public class Types
{
  /**
   * Test if a requested type is identity equal with one from a given types list. If <code>type</code> is null return
   * false. If a type to match happened to be null is considered no match.
   * 
   * @param type type to search for, possible null,
   * @param typesToMatch types list to compare with.
   * @return true if requested type is one from given types list.
   * @throws IllegalArgumentException if <code>typesToMach</code> is empty.
   */
  public static boolean equalsAny(Type type, Type... typesToMatch) throws IllegalArgumentException
  {
    Params.notNullOrEmpty(typesToMatch, "Types to match");
    if(type == null) {
      return false;
    }
    for(Type typeToMatch : typesToMatch) {
      if(type.equals(typeToMatch)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Test if given type is void. Returns true if type is {@link Void#TYPE} or {@link Void} class, i.e. is
   * <code>void</code> keyword or <code>Void</code> class.
   * 
   * @param t type to test for void.
   * @return true if requested type is void.
   */
  public static boolean isVoid(Type t)
  {
    return Void.TYPE.equals(t) || Void.class.equals(t);
  }

  /** Java language primitive values boxing classes. */
  private static Map<Type, Type> BOXING_MAP = new HashMap<Type, Type>();
  static {
    BOXING_MAP.put(boolean.class, Boolean.class);
    BOXING_MAP.put(byte.class, Byte.class);
    BOXING_MAP.put(char.class, Character.class);
    BOXING_MAP.put(short.class, Short.class);
    BOXING_MAP.put(int.class, Integer.class);
    BOXING_MAP.put(long.class, Long.class);
    BOXING_MAP.put(float.class, Float.class);
    BOXING_MAP.put(double.class, Double.class);
  }

  /**
   * Test if object instance is not null and extends or implements expected type. This predicate consider primitive and
   * related boxing types as equivalent, e.g. <code>1.23</code> is instance of {@link Double}.
   * 
   * @param o object instance to test, possible null,
   * @param t expected type.
   * @return true if instance is not null and extends or implements requested type.
   */
  public static boolean isInstanceOf(Object o, Type t)
  {
    if(o == null) {
      return false;
    }
    if(t instanceof Class) {
      Class<?> clazz = (Class<?>)t;
      if(clazz.isPrimitive()) {
        return BOXING_MAP.get(clazz) == o.getClass();
      }
      return clazz.isInstance(o);
    }
    return false;
  }

  /**
   * Test if object instance is a boolean, primitive or boxing class. Returns true if given object is primitive boolean
   * or {@link Boolean} instance or false otherwise. Return also false if object is null.
   * 
   * @param o object instance, possible null.
   * @return true if instance to test is boolean.
   */
  public static boolean isBoolean(Object o)
  {
    return o != null && isBoolean(o.getClass());
  }

  /**
   * Test if type is a boolean primitive or boxing class.
   * 
   * @param t type to test.
   * @return true if type is boolean.
   */
  public static boolean isBoolean(Type t)
  {
    return equalsAny(t, boolean.class, Boolean.class);
  }

  /** Java standard classes used to represent numbers, including primitives. */
  private static Type[] NUMERICAL_TYPES = new Type[]
  {
      int.class, long.class, double.class, Integer.class, Long.class, Double.class, byte.class, short.class, float.class, Byte.class, Short.class, Float.class, BigDecimal.class, Number.class
  };

  /**
   * Test if object instance is primitive numeric value or related boxing class. Returns true if given instance is a
   * primitive numeric value or related boxing class. Returns false if instance is null.
   * 
   * @param o object instance, possible null.
   * @return true if instance is a number.
   */
  public static boolean isNumber(Object o)
  {
    return o != null && isNumber(o.getClass());
  }

  /**
   * Test if type is numeric. A type is considered numeric if is a Java standard class representing a number.
   * 
   * @param t type to test.
   * @return true if <code>type</code> is numeric.
   */
  public static boolean isNumber(Type t)
  {
    for(int i = 0; i < NUMERICAL_TYPES.length; i++) {
      if(NUMERICAL_TYPES[i] == t) {
        return true;
      }
    }
    return false;
  }

  /**
   * Test if type is a character, primitive or boxing.
   * 
   * @param t type to test.
   * @return true if type is character.
   */
  public static boolean isCharacter(Type t)
  {
    return equalsAny(t, char.class, Character.class);
  }

  /**
   * Test if type is enumeration. This predicate delegates {@link Class#isEnum()} if type is a class. If not, returns
   * false.
   * 
   * @param t type to test.
   * @return true if type is enumeration.
   */
  public static boolean isEnum(Type t)
  {
    if(t instanceof Class<?>) {
      return ((Class<?>)t).isEnum();
    }
    return false;
  }

  /**
   * Test if type is a calendar date.
   * 
   * @param t type to test.
   * @return true if type is a calendar date.
   */
  public static boolean isDate(Type t)
  {
    return isKindOf(t, Date.class);
  }

  /**
   * Test if type is like a primitive? Return true only if given type is a number, boolean, enumeration, character, date
   * or string.
   * 
   * @param t type to test.
   * @return true if this type is like a primitive.
   */
  public static boolean isPrimitiveLike(Type t)
  {
    if(isNumber(t)) {
      return true;
    }
    if(isBoolean(t)) {
      return true;
    }
    if(isEnum(t)) {
      return true;
    }
    if(isCharacter(t)) {
      return true;
    }
    if(isDate(t)) {
      return true;
    }
    if(t == String.class) {
      return true;
    }
    return false;
  }

  /**
   * Test if type is array. If type is a class return {@link Class#isArray()} predicate value; otherwise test if type is
   * {@link GenericArrayType}.
   * 
   * @param t type to test.
   * @return true if type is array.
   */
  public static boolean isArray(Type t)
  {
    if(t instanceof Class<?>) {
      return ((Class<?>)t).isArray();
    }
    if(t instanceof GenericArrayType) {
      return true;
    }
    return false;
  }

  /**
   * Test instance if is array like. If instance to test is not null delegates {@link #isArrayLike(Type)}; otherwise
   * return false.
   * 
   * @param o instance to test, possible null in which case returns false.
   * @return true if instance is array like; returns false if instance to test is null.
   */
  public static boolean isArrayLike(Object o)
  {
    return o != null && isArrayLike(o.getClass());
  }

  /**
   * Test if type is array like, that is, array or collection. Uses {@link #isArray(Type)} and
   * {@link #isCollection(Type)}.
   * 
   * @param t type to test.
   * @return true if type is array like.
   */
  public static boolean isArrayLike(Type t)
  {
    return isArray(t) || isCollection(t);
  }

  /**
   * Test if instance is a collection. If instance to test is not null delegates {@link #isCollection(Type)}; otherwise
   * return false.
   * 
   * @param o instance to test, possible null in which case returns false.
   * @return true if instance is collection; returns false if instance to test is null.
   */
  public static boolean isCollection(Object o)
  {
    return o != null && isCollection(o.getClass());
  }

  /**
   * Test if type is collection. Returns true if type implements, directly or through inheritance, {@link Collection}
   * interface.
   * 
   * @param t type to test.
   * @return true if type is collection.
   */
  public static boolean isCollection(Type t)
  {
    return Types.isKindOf(t, Collection.class);
  }

  /**
   * Test if instance is a map. If instance to test is not null delegates {@link #isMap(Type)}; otherwise returns false.
   * 
   * @param o instance to test, possible null.
   * @return true if instance is a map; returns false if instance to test is null.
   */
  public static boolean isMap(Object o)
  {
    return o != null && Types.isMap(o.getClass());
  }

  /**
   * Test if type is map. Returns true if type implements, directly or through inheritance, {@link Map} interface.
   * 
   * @param t type to test.
   * @return true if type is map.
   */
  public static boolean isMap(Type t)
  {
    return Types.isKindOf(t, Map.class);
  }

  /**
   * Convert object instance to iterable. If object instance is an array or a collection returns an iterable instance
   * able to iterate array respective collection items. Otherwise return an empty iterable.
   * <p>
   * This utility method is designed to be used with <code>foreach</code> loop. Note that if object instance is not
   * iterable <code>foreach</code> loop is not executed.
   * 
   * <pre>
   * Object o = getObjectFromSomeSource();
   * for(Object item : Types.asIterable(o)) {
   *   // do something with item instance
   * }
   * </pre>
   * 
   * @param o object instance.
   * @return object iterable, possible empty.
   */
  public static Iterable<?> asIterable(final Object o)
  {
    if(o != null && o.getClass().isArray()) {
      return new Iterable<Object>()
      {
        private Object array = o;
        private int index;

        @Override
        public Iterator<Object> iterator()
        {
          return new Iterator<Object>()
          {
            @SuppressWarnings("unqualified-field-access")
            @Override
            public boolean hasNext()
            {
              return index < Array.getLength(array);
            }

            @SuppressWarnings("unqualified-field-access")
            @Override
            public Object next()
            {
              return Array.get(array, index++);
            }

            @Override
            public void remove()
            {
              throw new UnsupportedOperationException("Array iterator has no remove operation.");
            }
          };
        }
      };
    }

    if(isCollection(o)) {
      return (Iterable<?>)o;
    }

    return new Iterable<Object>()
    {
      @Override
      public Iterator<Object> iterator()
      {
        return new Iterator<Object>()
        {
          @Override
          public boolean hasNext()
          {
            return false;
          }

          @Override
          public Object next()
          {
            throw new UnsupportedOperationException("Empty iterator has no next operation.");
          }

          @Override
          public void remove()
          {
            throw new UnsupportedOperationException("Empty iterator has no remove operation.");
          }
        };
      }
    };
  }

  /**
   * Determine if a given type is a kind of a requested type to match. Returns true if <code>type</code> is a subclass
   * or implements <code>typeToMatch</code> - not necessarily direct. Boxing classes for primitive values are
   * compatible. This depart from {@link Class#isAssignableFrom(Class)} that consider primitive and related boxing class
   * as different.
   * <p>
   * If either type or type to match are parameterized types uses the raw class. If either type or type to match are
   * null returns false.
   * 
   * @param t type to test,
   * @param typeToMatch desired type to match.
   * @return true if <code>type</code> is subclass of or implements <code>typeToMatch</code>.
   */
  private static boolean isKindOf(Type t, Type typeToMatch)
  {
    if(t == null || typeToMatch == null) {
      return false;
    }
    if(t.equals(typeToMatch)) {
      return true;
    }

    Class<?> clazz = typeToClass(t);
    Class<?> classToMatch = typeToClass(typeToMatch);

    if(clazz.isPrimitive()) {
      return BOXING_MAP.get(clazz) == classToMatch;
    }
    if(classToMatch.isPrimitive()) {
      return BOXING_MAP.get(classToMatch) == clazz;
    }

    return classToMatch.isAssignableFrom(clazz);
  }

  /**
   * Cast Java reflective type to language class. If <code>type</code> is instance of {@link Class} just return it. If
   * is parameterized type returns the raw class.
   * 
   * @param t Java reflective type.
   * @return the class described by given <code>type</code>.
   */
  private static Class<?> typeToClass(Type t)
  {
    if(t instanceof Class<?>) {
      return (Class<?>)t;
    }
    if(t instanceof ParameterizedType) {
      return (Class<?>)((ParameterizedType)t).getRawType();
    }
    throw new BugError("Unknown type %s to convert to class.", t);
  }
}
