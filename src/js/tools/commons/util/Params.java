package js.tools.commons.util;

import java.io.File;

/**
 * Invocation parameters (in)sanity tests. This utility class supplies convenient methods for invocation parameters
 * validation, see sample usage. All throws {@link IllegalArgumentException} if validation fails. Also all have a
 * <code>name</code> parameter used to format exception message; it is injected at message beginning exactly as
 * supplied.
 * <p>
 * In sample code throws illegal argument exception if <code>file</code> argument is null. Exception message is 'File
 * parameter is null.'
 * 
 * <pre>
 *  void method(File file . . . {
 *      Params.notNull(file, "File");
 *      . . .
 *  }
 * </pre>
 * <p>
 * This utility class allows for sub-classing. See {@link js.util} for utility sub-classing description.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class Params
{
  /** Disable default constructor synthesis. */
  private Params()
  {
  }

  /**
   * Check if file parameter is an existing directory. This validator throws illegal argument if given file does not
   * exist or is not a directory.
   * 
   * @param parameter invocation file parameter,
   * @param name parameter name.
   * @throws IllegalArgumentException if <code>parameter</code> file is null or does not designate an existing
   *           directory.
   */
  public static void isDirectory(File parameter, String name)
  {
    if(parameter == null || !parameter.isDirectory()) {
      throw new IllegalArgumentException(String.format("%s |%s| is missing or is not a directory.", name, parameter.getAbsolutePath()));
    }
  }

  /**
   * Throw exception if parameter is null. Name parameter can be formatted as accepted by
   * {@link String#format(String, Object...)}.
   * 
   * @param parameter invocation parameter to test,
   * @param name parameter name used on exception message,
   * @param args optional arguments if name is formatted.
   * @throws IllegalArgumentException if <code>parameter</code> is null.
   */
  public static void notNull(Object parameter, String name)
  {
    if(parameter == null) {
      throw new IllegalArgumentException(name + " parameter is null.");
    }
  }

  /**
   * Test if string parameter is not null or empty.
   * 
   * @param parameter invocation string parameter,
   * @param name the name of invocation parameter.
   * @throws IllegalArgumentException if <code>parameter</code> is null or empty.
   */
  public static void notNullOrEmpty(String parameter, String name)
  {
    if(parameter == null || parameter.isEmpty()) {
      throw new IllegalArgumentException(name + " is null or empty.");
    }
  }

  /**
   * Test if array parameter is not null or empty.
   * 
   * @param parameter invocation array parameter,
   * @param name the name of invocation parameter.
   * @throws IllegalArgumentException if <code>parameter</code> array is null or empty.
   */
  public static void notNullOrEmpty(Object[] parameter, String name) throws IllegalArgumentException
  {
    if(parameter == null || parameter.length == 0) {
      throw new IllegalArgumentException(name + " parameter is empty.");
    }
  }

  /**
   * Test if given boolean condition is true and throw exception if not. Exception message is that supplied by
   * <code>message</code> parameter.
   * 
   * @param condition boolean condition to test,
   * @param message exception message.
   * @throws IllegalArgumentException if given condition is false.
   */
  public static void isTrue(boolean condition, String message) throws IllegalArgumentException
  {
    if(!condition) {
      throw new IllegalArgumentException(message);
    }
  }
}
