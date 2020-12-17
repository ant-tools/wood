package js.tools.commons.json;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import js.tools.commons.BugError;
import js.tools.commons.util.Classes;
import js.tools.commons.util.Strings;

/**
 * Non generic object value. This class helps parser to create object instance of proper type and set fields values.
 * <p>
 * This class does not support generic classes because type variables are removed from byte code. In example below
 * <code>T</code> field will be of type <code>java.lang.Object</code>. Note that parameterized types are stored into
 * byte code and <code>integers</code> field can be properly reflected. Anyway, since type variables are not accessible
 * we cannot use generic objects.
 * 
 * <pre>
 * private static class Box&lt;T&gt;
 * {
 *   T value;
 *   List&lt;Integer&gt; integers;
 * }
 * </pre>
 * <p>
 * Note1: do not confuse type variables (a.k.a. type parameters) with parameterized types. Remember that a parameterized
 * type is obtained by referencing a generic class with a concrete type argument; in example would be
 * <code>Box&lt;Integer&gt;</code>.To sum up, type variables are not stored into byte code whereas parameterized types
 * are.
 * <p>
 * Note2: in case you wonder why collections and maps are supported and generic objects not. Simple. For collections we
 * have a single type variable and we know it is component type. On maps we know first type variables is the key and the
 * second is value. For generic object fields I do not know a way to find out the bound between a field and class type
 * variables.
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
class ObjectValue extends Value
{
  /** Object not generic type. */
  private Class<?> clazz;

  /** Object instance. */
  protected Object instance;

  /**
   * Temporarily store currently working field name. This field name is stored by {@link #setFieldName(String)} and used
   * by {@link #setValue(Object)}. It is caller responsibility to ensure proper setters invocation order.
   */
  private String fieldName;

  ObjectValue(){}
  
  /**
   * Create object value instance.
   * 
   * @param clazz object class.
   */
  ObjectValue(Class<?> clazz)
  {
    this.clazz = clazz;
    this.instance = Classes.newInstance(clazz);
  }

  /**
   * Get wrapped object instance.
   * 
   * @return object instance.
   */
  @Override
  public Object instance()
  {
    return instance;
  }

  /**
   * Get the type of this object instance.
   * 
   * @return instance type.
   */
  @Override
  public Type getType()
  {
    return clazz;
  }

  /**
   * Set object instance to null.
   * 
   * @param value unused, always null.
   * @throws UnsupportedOperationException if <code>value</code> is not null.
   */
  @Override
  public void set(Object value) throws UnsupportedOperationException
  {
    if(value != null) {
      throw new UnsupportedOperationException();
    }
    instance = null;
  }

  /**
   * Get the type of named field or null if field does not exist.
   * 
   * @return field type or null.
   * @throws SecurityException 
   * @throws NoSuchFieldException 
   */
  public Type getValueType() throws NoSuchFieldException, SecurityException
  {
    Field field = clazz.getDeclaredField(fieldName);
    return field != null ? field.getGenericType() : null;
  }

  /**
   * Store the name for currently working field. Given <code>fieldName</code> is not checked for existence but just
   * stored into {@link #fieldName}.
   * 
   * @param fieldName current working field name.
   */
  public final void setFieldName(String fieldName)
  {
    this.fieldName = Strings.toMemberName(fieldName);
  }

  /**
   * Set value for the field identified by field name stored by a previous call to {@link #setFieldName(String)}. If
   * named field is missing warn to log and just return.
   * <p>
   * This setter uses {@link Classes#getFieldEx(Class, String)} to access named class field and benefit from searching
   * on superclass too.
   * 
   * @param value field value, null accepted.
   */
  public void setValue(Object value)
  {
    try {
      Field field = clazz.getDeclaredField(fieldName);
      if(value == null && field.getType().isPrimitive()) {
        return;
      }
      field.setAccessible(true);
      field.set(instance, Converter.toObject(value, field.getType()));
    }
    catch(NoSuchFieldException e) {
      // log.warn("Missing field |%s| from class |%s|. Ignore JSON value.", fieldName, clazz);
    }
    catch(IllegalArgumentException e) {
      // log.error("Illegal argument |%s| while trying to set field |%s| from class |%s|.", value.getClass(), fieldName,
      // clazz);
    }
    catch(IllegalAccessException e) {
      throw new BugError(e);
    }
  }
}
