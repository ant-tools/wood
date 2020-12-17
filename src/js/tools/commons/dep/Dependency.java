package js.tools.commons.dep;

import js.tools.commons.util.Strings;

/**
 * A script dependency has a name and a type, basically strong or weak. Dependency type affects scripts processing
 * order: both should be loaded but strong dependency should be loaded before hosting script.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public abstract class Dependency
{
  private final String name;
  private Type type;

  protected Dependency(String name, Type type)
  {
    this.name = name;
    this.type = type;
  }

  public String getName()
  {
    return name;
  }

  public void setType(Type type)
  {
    this.type = type;
  }

  public Type getType()
  {
    return type;
  }

  public boolean isStrong()
  {
    return type == Type.STRONG;
  }

  public boolean isThirdParty()
  {
    return type == Type.THIRD_PARTY;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if(this == obj) return true;
    if(obj == null) return false;
    if(getClass() != obj.getClass()) return false;
    Dependency other = (Dependency)obj;
    if(name == null) {
      if(other.name != null) return false;
    }
    else if(!name.equals(other.name)) return false;
    if(type != other.type) return false;
    return true;
  }

  @Override
  public String toString()
  {
    return Strings.concat(name, ":", type);
  }

  /**
   * Dependency types change script loading order. There are basically two dependency kinds: strong and weak. Both
   * should be loaded but strong dependency must be loaded before hosting script.
   * 
   * @author Iulian Rotaru
   * @version final
   */
  public static enum Type
  {
    /**
     * Weak dependency is in contrast with strong dependency. A weak dependency takes effect at run-time, after script
     * loading. For example, an instance created in constructor depends on its class but is weak dependency since is not
     * mandatory to be resolved at script loading. As a consequence a weak dependency can be loaded after host script
     * loading; is mandatory to be loaded but does not matter loading order.
     */
    WEAK,
    /**
     * A strong dependency is discovered when j(s)-script is parsed at script loading. For example, a super class
     * declared with $super j(s)-script operator is a strong dependency since it should be resolved at script loading. A
     * strong dependency should be loaded before hosting script loading.
     */
    STRONG,
    /** Third party dependencies are introduced with $declare and $include j(s)-script operators. */
    THIRD_PARTY
  }
}
