package js.tools.commons.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Package internal utility functions.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public final class Utils
{
  /** Forbid default constructor synthesis. */
  private Utils()
  {
  }

  /**
   * Pattern for qualified class name. This pattern matches class names but without member; inner classes are accepted.
   * 
   * <pre>
   *    qualifiedClassName := packageName ("." className)+
   *    packageName := packageName "." packagePart
   *    packagePart := lowerChar+
   *    className := upperChar char*
   * </pre>
   */
  private static final Pattern QUALIFIED_CLASS_NAME = Pattern.compile("^([a-z][a-z0-9]*(?:\\.[a-z][a-z0-9]*)*(?:\\.[A-Z][a-zA-Z0-9]*)+)$");

  /**
   * Predicate to test if name is qualified name. Note that class name without packages does not qualifies.
   * 
   * @param name name to test.
   * @return true if given name is a qualified class name.
   */
  public static boolean isQualifiedClassName(String name)
  {
    return QUALIFIED_CLASS_NAME.matcher(name).matches();
  }

  /**
   * Pattern for qualified name.
   * <p>
   * A qualified name has a single mandatory simple class name that can be prefixed by optional package name. Qualified
   * names also support optional inner classes and member names. Note that member can be also nested.
   * 
   * <pre>
   *    qualified-name = package DOT class *(DOT class) *(DOT member)
   *    package        = package-part *(DOT package-part)
   *    package-part   = LOWER-CH *CH
   *    class          = UPPER-CH *CH
   *    member         = field / constant
   *    field          = (LOWER-CH / UNDERSCORE) *CH
   *    constant       = UPPER-CH 1*(UPPER-CH / UNDERSCORE)  
   *    
   *    ; terminal symbols definition
   *    DOT        = "."          ; separator for qualified name parts
   *    UNDERSCORE = "_"          ; underscore
   *    CH         = [_a-zA-Z0-9] ; underscore, case insensitive letter or digit
   *    UPPER-CH   = [A-Z]        ; upper case letter
   *    LOWER-CH   = [a-z]        ; lower case  letter
   * </pre>
   * 
   */
  private static final Pattern DEPENDENCY_NAME = Pattern.compile("^" + //
      "((?:[a-z][_a-zA-Z0-9]*)(?:\\.[a-z][_a-zA-Z0-9]*)*\\." + // package
      "(?:[A-Z][_a-zA-Z0-9]*))" + // class; ending capture group is for qualified class name
      "(?:\\.[A-Z][_a-zA-Z0-9]*)*" + // optional inner classes
      "(?:\\.(?:(?:[_a-z][_a-zA-Z0-9]*)|(?:[A-Z][_A-Z]+)))*" + // optional members
      "$");

  /**
   * Predicate to test if name is qualified name.
   * 
   * @param name name to test.
   * @return true if given name is a qualified class name.
   */
  public static boolean isDependencyName(String name)
  {
    return DEPENDENCY_NAME.matcher(name).matches();
  }

  public static String getDependencyQualifiedClassName(String qualifiedMemeberName)
  {
    Matcher m = DEPENDENCY_NAME.matcher(qualifiedMemeberName);
    return m.find() ? m.group(1) : "";
  }
}
