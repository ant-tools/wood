package js.tools.commons.json;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import js.tools.commons.BugError;
import js.tools.commons.util.Types;

public class Converter
{
  public static Object toObject(Object object, Type type)
  {
    if(object == null || !(object instanceof String)) {
      return object;
    }
    String value = (String)object;

    if(Types.isNumber(type)) {
      return parseNumber(value, type);
    }
    if(Types.isBoolean(type)) {
      return parseBoolean(value, type);
    }
    if(Types.isEnum(type)) {
      return parseEnum(value, type);
    }
    if(Types.isCharacter(type)) {
      return parseCharacter(value, type);
    }
    if(Types.isDate(type)) {
      return parseDate(value, type);
    }
    return value;
  }

  private static Object parseDate(String string, Type type)
  {
    if(string.isEmpty()) {
      return null;
    }

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));

    ParsePosition parsePosition = new ParsePosition(0);
    Date date = df.parse(string, parsePosition);
    if(date == null) {
      throw new JsonException("Cannot parse ISO8601 date from |%s| at position |%d|.", string, parsePosition.getErrorIndex());
    }

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    calendar.set(Calendar.MILLISECOND, 0);
    long time = calendar.getTimeInMillis();

    if(type == Date.class) {
      return new Date(time);
    }
    if(type == java.sql.Date.class) {
      return new java.sql.Date(time);
    }
    if(type == Time.class) {
      return new Time(time);
    }
    if(type == Timestamp.class) {
      return new Timestamp(time);
    }

    throw new BugError("Unsupported date type |%s|.", type);
  }

  private static Object parseCharacter(String string, Type type)
  {
    // at this point value type is guaranteed to be char or Character
    if(string.length() > 1) {
      throw new JsonException("Trying to convert a larger string into a single character.");
    }
    return string.charAt(0);
  }

  @SuppressWarnings(
  {
      "unchecked", "rawtypes"
  })
  private static Object parseEnum(String string, Type type)
  {
    if(string.isEmpty()) {
      return null;
    }
    // at this point value type is guaranteed to be enumeration
    return Enum.valueOf((Class)type, string);
  }

  private static Object parseBoolean(String string, Type type)
  {
    // at this point value type is a boolean or a boxing boolean
    string = string.toLowerCase();
    if(string.equals("true")) {
      return true;
    }
    if(string.equals("yes")) {
      return true;
    }
    if(string.equals("1")) {
      return true;
    }
    if(string.equals("on")) {
      return true;
    }
    return false;
  }

  private static Object parseNumber(String string, Type type)
  {
    Number number = string.isEmpty() ? 0 : parseNumber(string);

    // hopefully int and double are most used numeric types and test them first
    if(Types.equalsAny(type, int.class, Integer.class)) {
      return number.intValue();
    }
    if(Types.equalsAny(type, double.class, Double.class)) {
      return number.doubleValue();
    }
    if(Types.equalsAny(type, byte.class, Byte.class)) {
      return number.byteValue();
    }
    if(Types.equalsAny(type, short.class, Short.class)) {
      return number.shortValue();
    }
    if(Types.equalsAny(type, long.class, Long.class)) {
      // because converting between doubles and longs may result in loss of precision we need
      // special treatment for longs. @see ConverterUnitTest.testConversionPrecision
      if(string.length() > 0 && string.indexOf('.') == -1) {
        // handle hexadecimal notation
        if(string.length() > 1 && string.charAt(0) == '0' && string.charAt(1) == 'x') {
          return Long.parseLong(string.substring(2), 16);
        }
        return Long.parseLong(string);
      }
      return number.longValue();
    }
    if(Types.equalsAny(type, float.class, Float.class)) {
      return number.floatValue();
    }
    if(Types.equalsAny(type, BigDecimal.class)) {
      return new BigDecimal(number.doubleValue());
    }
    throw new BugError("Unsupported numeric value |%s|.", type);
  }

  /**
   * Parse numeric string value to a number.
   * 
   * @param string numeric string value.
   * @return number instance.
   */
  private static Number parseNumber(String string)
  {
    if(string.length() > 2 && string.charAt(0) == '0' && string.charAt(1) == 'x') {
      return Long.parseLong(string.substring(2), 16);
    }
    return Double.parseDouble(string);
  }
}
