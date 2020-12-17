package js.tools.commons.dep;

/**
 * Specialized dependency for j(s)-script class.
 * 
 * @author Iulian Rotaru
 * @version final
 */
final class JsClassDependency extends Dependency
{
  /**
   * Create dependency with name set to j(s)-script class name and given dependency type.
   * 
   * @param jsClassName j(s)-script class name,
   * @param type dependency type, strong or weak.
   */
  public JsClassDependency(String jsClassName, Type type)
  {
    super(jsClassName, type);
  }
}
