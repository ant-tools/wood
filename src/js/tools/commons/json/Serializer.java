package js.tools.commons.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Stack;
import java.util.TimeZone;

import js.tools.commons.BugError;
import js.tools.commons.util.Params;
import js.tools.commons.util.Types;

/**
 * Serialize primitive values, objects, arrays, collections and maps to JSON string representation. This class is
 * invoked internally by {@link JsonImpl} facade and is not intended to be reused; create a new serializer instance for
 * every value to stringify. Once instance created invoke {@link #serialize(Writer, Object)} with external created
 * characters writer instance. After serialization completes writer is flushed by left opened.
 * <p>
 * This class handle serialization in a best effort manner. If for some reason a field cannot be processed it is
 * replaced by null, but takes care to record failing condition to error log. Currently there are two conditions that
 * could lead to <code>null</code> replacement:
 * <ol>
 * <li>a value is a Hibernate PersistentCollection mapped lazily and attempt to access it outside Hibernate Session,
 * <li>circular dependency discovered, see {@link #circularDependenciesStack}.
 * </ol>
 * <p>
 * Serializer supports an extension used merely by this library: add class name before actual serialized value.
 * Considering below <code>js.test.Page</code> class:
 * 
 * <pre>
 * 	  class js.test.Page
 * 	  {
 * 	    String name;
 * 	    State state;
 * 	  }
 * </pre>
 * <p>
 * it is serialized as follow: <code>{"class":"js.test.Page","name":"index.htm","state":"ACTIVE"}</code>.
 * 
 * @author Iulian Rotaru
 * @since 1.0.2
 */
final class Serializer
{
  /** JSON keyword for null values. */
  private static final String KEYWORD_NULL = "null";

  /** External created writer instance initialized by {@link #serialize(Writer, Object)} entry point. */
  private Writer writer;

  /** Include class name before actual serialized value. This is a non standard extension used primarily by library. */
  private boolean includeClass;

  /**
   * Circular dependencies stack keeps track of processed values. Values are pushed just before entering the actual
   * serialization and extracted at final. See {@link #serialize(Object)}.
   */
  private Stack<Object> circularDependenciesStack = new Stack<Object>();

  /** Create default serializer. */
  public Serializer()
  {
  }

  /**
   * Create serializer instance with inband type information.
   * 
   * @param includeClass always true to include class name before serialized value.
   * @throws IllegalArgumentException if <code>includeClass</code> is not true.
   */
  public Serializer(boolean includeClass) throws IllegalArgumentException
  {
    Params.isTrue(includeClass, "Include class flag should always be true.");
    this.includeClass = includeClass;
  }

  /**
   * Serialize primitive or aggregated value to given writer. Writer is flushed after serialization completes but is
   * left opened.
   * 
   * @param writer external created characters stream,
   * @param value primitive or aggregated value.
   * @throws IOException if IO write operation fails.
   */
  public void serialize(Writer writer, Object value) throws IOException
  {
    this.writer = writer instanceof BufferedWriter ? writer : new BufferedWriter(writer);
    serialize(value);
    this.writer.flush();
  }

  /**
   * Serialization worker implements core logic for serialization process. It handles both primitive and aggregated
   * values. For aggregated values delegates helpers implemented by this class: {@link #serializeArray(Object)},
   * {@link #serializeMap(Object)} and {@link #serializeObject(Object)}. Helpers method takes care of structural details
   * but for value serialization re-invoke this method. If value is null uses {@link #KEYWORD_NULL}.
   * <p>
   * Because for non primitive values this method is executed recursively it implements protection against circular
   * dependencies. When a circular dependency is discovered replace value with null and record to error log. For
   * circular dependencies protection uses {@link #circularDependenciesStack}. Value to serialize is pushed to stack
   * just before processing and extracted after. If value it is already in stack replace it with null and dump the stack
   * to error log.
   * 
   * @param value primitive or aggregated value.
   * @throws IOException if IO write operation fails.
   */
  private void serialize(Object value) throws IOException
  {
    if(value == null) {
      write(KEYWORD_NULL);
      return;
    }

    if(circularDependenciesStack.contains(value)) {
      write(KEYWORD_NULL);
      return;
    }
    circularDependenciesStack.push(value);

    try {
      if(Types.isBoolean(value)) {
        write(Boolean.toString((boolean)value));
        return;
      }

      if(Types.isNumber(value)) {
        write(value.toString());
        return;
      }

      if(value instanceof String) {
        writeString((String)value);
        return;
      }

      if(value instanceof Date) {
        writeDate((Date)value);
        return;
      }

      if(Types.isEnum(value.getClass())) {
        writeEnum(value);
        return;
      }

      if(Types.isArrayLike(value)) {
        serializeArray(value);
        return;
      }

      if(Types.isMap(value)) {
        serializeMap(value);
        return;
      }

      serializeObject(value);
    }
    finally {
      circularDependenciesStack.pop();
    }
  }

  @SuppressWarnings(
  {
      "unchecked", "rawtypes"
  })
  private void writeEnum(Object value) throws IOException
  {
    writeString(Enum.valueOf((Class)value.getClass(), value.toString()).name());
  }

  /**
   * Serialize opening square brace, array items separated by comma and closing square brace. This method invoked
   * recursively {@link #serialize(Object)} for every array item.
   * 
   * @param value
   * @throws IOException
   */
  private void serializeArray(Object value) throws IOException
  {
    write('[');
    int index = 0;

    for(Object item : Types.asIterable(value)) {
      if(index++ > 0) {
        write(',');
      }
      serialize(item);
    }

    write(']');
  }

  /**
   * Serialize value map entries separated by comma, key/value pair being separated by colon, all in curly braces. A map
   * is serialized like an object in opening and closing curly braces. For key and value serialization this method
   * recursively invoke {@link #serialize(Object)}.
   * 
   * @param value value map.
   * @throws IOException if IO write operation fails.
   */
  private void serializeMap(Object value) throws IOException
  {
    assert value != null;
    assert value instanceof Map;

    write('{');

    @SuppressWarnings("unchecked")
    Map<Object, Object> map = (Map<Object, Object>)value;
    int index = 0;

    for(Map.Entry<Object, Object> entry : map.entrySet()) {
      if(index++ > 0) {
        write(',');
      }
      serialize(entry.getKey());
      write(':');
      serialize(entry.getValue());
    }

    write('}');
  }

  /**
   * Serialize opening curly brace, all fields separated by comma and closing curly brace. All fields are serialized, in
   * no particular order, except static and transient. If {@link #includeClass} flag is true fields serialization is
   * preceded by value object class name. Also this method process superclass hierarchy as long as superclass is part of
   * the same package as given value object.
   * <p>
   * Note that this method hides a recursive call to {@link #serialize(Object)} via
   * {@link #serializeField(Object, Field)}.
   * 
   * @param value value object to serialize.
   * @throws IOException if IO write operation fails.
   */
  private void serializeObject(Object value) throws IOException
  {
    assert value != null;

    write('{');
    int index = 0;

    Class<?> clazz = value.getClass();

    if(includeClass) {
      // do not use inbound class for nested class
      includeClass = false;
      writeString("class");
      write(":");
      writeString(clazz.getName());
      ++index;
    }

    for(Field field : clazz.getDeclaredFields()) {
      if(field.isSynthetic()) {
        // do not include synthetic fields like outer 'this' for anonymous inner classes
        continue;
      }
      int m = field.getModifiers();
      if(Modifier.isStatic(m) || Modifier.isTransient(m)) {
        continue;
      }
      if(index++ > 0) {
        write(',');
      }
      serializeField(value, field);
    }

    // include super classes fields as long as they are in the same package
    Package classPackage = clazz.getPackage();
    Class<?> superclass = clazz.getSuperclass();
    while(superclass != null && classPackage.equals(superclass.getPackage())) {
      for(Field field : superclass.getDeclaredFields()) {
        int m = field.getModifiers();
        if(Modifier.isStatic(m) || Modifier.isTransient(m)) {
          continue;
        }
        if(index++ > 0) {
          write(',');
        }
        serializeField(value, field);
      }
      superclass = superclass.getSuperclass();
    }

    write('}');
  }

  /**
   * Serialize field name, colon as value separator and field value. Note that field value is processed by invoking
   * recursively {@link #serialize(Object)}.
   * 
   * @param value value object field belongs to,
   * @param field field reflective descriptor.
   * @throws IOException if IO write operation fails.
   */
  private void serializeField(Object value, Field field) throws IOException
  {
    field.setAccessible(true);
    writeString(field.getName());
    write(':');
    serialize(getFieldValue(value, field));
  }

  /**
   * Get field value from given instance.
   * 
   * @param instance object instance to get field value from,
   * @param field field reflective descriptor.
   * @return instance field value.
   */
  private static Object getFieldValue(Object instance, Field field)
  {
    try {
      return field.get(instance);
    }
    catch(IllegalArgumentException e) {
      throw new BugError("Value object |%s| is not an instance.", instance.getClass());
    }
    catch(IllegalAccessException ignore) {
      throw new RuntimeException("Field with accesibily set to true throws illegal access.");
    }
    catch(RuntimeException e) {
      // best effort: on any error returns null
    }
    return null;
  }

  private void writeDate(Date value) throws IOException
  {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    calendar.setTime(value);

    StringBuilder builder = new StringBuilder();

    int year = calendar.get(Calendar.YEAR);
    if(calendar.get(Calendar.ERA) == 0) {
      // https://en.wikipedia.org/wiki/ISO_8601
      // by convention 1 BC is labeled +0000, 2 BC is labeled −0001, ...

      if(year > 1) {
        builder.append('-');
      }
      --year;
    }

    builder.append(String.format("%04d", year));
    builder.append('-');
    builder.append(String.format("%02d", calendar.get(Calendar.MONTH) + 1));
    builder.append('-');
    builder.append(String.format("%02d", calendar.get(Calendar.DATE)));
    builder.append('T');
    builder.append(String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)));
    builder.append(':');
    builder.append(String.format("%02d", calendar.get(Calendar.MINUTE)));
    builder.append(':');
    builder.append(String.format("%02d", calendar.get(Calendar.SECOND)));
    builder.append('Z');

    writeString(builder.toString());
  }

  /**
   * Write string and takes care to escape accordingly RFC 4627.
   * <p>
   * Excerpt: ... string begins and ends with quotation marks. All Unicode characters may be placed within the quotation
   * marks except for the characters that must be escaped: quotation mark, reverse solidus, and the control characters
   * (U+0000 through U+001F).
   * 
   * @param string string to escape and write.
   * @throws IOException if IO write operation fails.
   */
  private void writeString(String string) throws IOException
  {
    write('"');

    for(int i = 0; i < string.length(); i++) {
      char c = string.charAt(i);
      switch(c) {
      case '\\':
      case '"':
        write('\\');
        write(c);
        break;

      case '/':
        write(c);
        break;

      case '\b':
        write("\\b");
        break;

      case '\t':
        write("\\t");
        break;

      case '\n':
        write("\\n");
        break;

      case '\f':
        write("\\f");
        break;

      case '\r':
        write("\\r");
        break;

      default:
        if(c < '\u0020') {
          String unicode = "000" + Integer.toHexString(c);
          write("\\u");
          write(unicode.substring(unicode.length() - 4));
        }
        else {
          write(c);
        }
      }
    }

    write('"');
  }

  /**
   * Write a string to internal JSON stream. This method is not only convenient but isolate JSON serializer logic from
   * underlying JSON stream.
   * 
   * @param s string to write.
   * @throws IOException if IO write operation fails.
   */
  private void write(String s) throws IOException
  {
    writer.write(s);
  }

  /**
   * Write a single character to internal JSON stream. This method is not only convenient but isolate JSON serializer
   * logic from underlying JSON stream.
   * 
   * @param c character to write.
   * @throws IOException if IO write operation fails.
   */
  private void write(char c) throws IOException
  {
    writer.write(c);
  }
}
