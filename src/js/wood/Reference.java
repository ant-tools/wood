package js.wood;

import js.util.Params;

/**
 * Immutable resource reference. Simply put, a reference is a pointer from a source file to a resource, be it variable
 * or media file. It has a type and a name, uniquely identifying the resource in its scope. Resource reference scope is
 * the component to which source file defining reference belongs plus global assets scope.
 * <p>
 * Regarding media files, resource reference is a sort of abstract addressing. It does not identify precisely the file;
 * extension and variants are not included into media reference name. Also media file can be stored private into
 * component directory or global into assets. Anyway, media file references support sub-directories, for example
 * <code>@image/icon/logo</code>. There are methods to test and retrieve the path, see {@link #hasPath()} and
 * {@link #getPath()}.
 * <p>
 * Reference syntax is described below. This syntax is the same, no mater the source file type where reference is used:
 * layout, style or script.
 * 
 * <pre>
 * reference = MARK resource-type SEP ?(path SEP) name
 * path      = 1*CH           ; optional path for media files only
 * name      = 1*CH  
 * ; resource-type is defined by {@link ResourceType}, to lower case
 * 
 * ; terminal symbols definition
 * MARK = "@"                 ; reference mark
 * SEP  = "/"                 ; reference name separator
 * CH   = ALPHA / DIGIT / "-" ; character is US-ASCII alphanumeric and dash
 * 
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * <p>
 * As stated variables and media files can be referenced from any source file: layout, style or script. Here are sample
 * usage for all three cases. Note that references are text replaced and where source file syntax requires quotes (")
 * they should be explicitly used.
 * 
 * <pre>
 *  &lt;body&gt;
 *      &lt;h1&gt;@string/title&lt;/h1&gt;
 *      &lt;img src="@image/logo" /&gt;
 *      &lt;p&gt;@text/message&lt;/p&gt;
 *      . . .
 *  &lt;/body&gt;
 *  
 *  body {
 *      {@literal @}style/page
 *      width: {@literal @}dimen/page-width;
 *      background-image: url("@image/page-bg");
 *      background-color: {@literal @}color/page-color;
 *      . . .
 *  }
 * 
 *  js.ua.System.alert("@string/exception");
 *  this.setRichText("@text/message");
 *  this.logo.setSrc("@image/logo");
 *  this.audioPlayer.play("@audio/beep");
 * </pre>
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class Reference
{
  /** Mark for reference start. */
  public static final char MARK = '@';

  /** Reference name separator. */
  public static final char SEPARATOR = '/';

  /** Referenced resource type. If type is {@link ResourceType#UNKNOWN} this resource reference is invalid. */
  private ResourceType resourceType;

  /** Optional path, for media files only. */
  private String path;

  /**
   * Resource name, unique in scope. Resource reference name scope is the component directory in which it is used plus
   * global project assets.
   */
  private String name;

  /** Cached value for reference hash code. */
  private int hashCode;

  /** Cached value for instance string representation. */
  private String string;

  /** Keep values source file for error tracking. */
  private String sourceFile;

  /**
   * Create immutable reference instance from given type and name.
   * 
   * @param sourceFile source file, for exception handling and debugging,
   * @param resourceType referenced resource type,
   * @param name name uniquely identifying resource in its scope.
   * @throws IllegalArgumentException if a parameter is null.
   */
  public Reference(FilePath sourceFile, ResourceType resourceType, String name) throws IllegalArgumentException
  {
    Params.notNull(resourceType, "Resource type");
    Params.notNull(name, "Name");
    this.sourceFile = sourceFile.toFile().getPath();
    ctor(resourceType, name);
  }

  /**
   * Test contructor.
   * 
   * @param resourceType referenced resource type,
   * @param name name uniquely identifying resource in its scope.
   */
  public Reference(ResourceType resourceType, String name) throws IllegalArgumentException
  {
    this.sourceFile = "test";
    ctor(resourceType, name);
  }

  /**
   * Test contructor. Create immutable reference instance and parse type and name from reference string value.
   * 
   * @param reference reference string value.
   * @throws WoodException if <code>reference</code> parameter is not valid.
   */
  public Reference(String reference) throws WoodException
  {
    if(reference.charAt(0) != MARK) {
      throw new WoodException("Invalid reference |%s| syntax. Missing start mark.", reference);
    }
    int separator = reference.indexOf(SEPARATOR);
    if(separator == -1) {
      throw new WoodException("Invalid reference |%s| syntax. Missing name separator.", reference);
    }
    this.sourceFile = "test";
    ctor(ResourceType.getValueOf(reference.substring(1, separator)), reference.substring(separator + 1));
  }

  /**
   * Pseudo-constructor for instance initialization.
   * 
   * @param resourceType referenced resource type,
   * @param name name scope uniquely identifying resource.
   * @throws WoodException if resource type is variable and <code>name</code> contains path.
   */
  private void ctor(ResourceType resourceType, String name) throws WoodException
  {
    this.resourceType = resourceType;

    int pathSeparator = name.lastIndexOf(SEPARATOR);
    if(pathSeparator == -1) {
      this.name = name;
    }
    else {
      if(!isMedia()) {
        throw new WoodException("Invalid reference |%s| syntax on file |%s|. Variable reference with path.", name, sourceFile);
      }
      this.path = name.substring(0, pathSeparator);
      this.name = name.substring(pathSeparator + 1);
    }

    hashCode = 1;
    hashCode = CT.PRIME * hashCode + resourceType.hashCode();
    hashCode = CT.PRIME * hashCode + name.hashCode();
  }

  /**
   * Get resource type.
   * 
   * @return resource type.
   * @see #resourceType
   */
  public ResourceType getResourceType()
  {
    return resourceType;
  }

  /**
   * Get resource name.
   * 
   * @return resource name.
   * @see #name
   */
  public String getName()
  {
    return name;
  }

  /**
   * Test if this reference contains path.
   * 
   * @return true if reference contains path.
   * @see #path
   */
  public boolean hasPath()
  {
    return path != null;
  }

  /**
   * Return this reference path, possible null.
   * 
   * @return reference path.
   * @see #path
   */
  public String getPath()
  {
    return path;
  }

  /**
   * Test if referenced resource is a variable. This predicate delegates {@link ResourceType#isVariable()}.
   * 
   * @return true if resource is a variable.
   * @see ResourceType#isVariable()
   */
  public boolean isVariable()
  {
    return resourceType.isVariable();
  }

  /**
   * Test if referenced resource is a media file. This predicate delegates {@link ResourceType#isMedia()}.
   * 
   * @return true if resource is media file.
   * @see ResourceType#isMedia()
   */
  public boolean isMedia()
  {
    return resourceType.isMedia();
  }

  /**
   * Test if this reference is valid, that is, referenced resource type is known.
   * 
   * @return true if this reference is valid.
   * @see #resourceType
   */
  public boolean isValid()
  {
    return resourceType != ResourceType.UNKNOWN;
  }

  /**
   * Test if character is valid for resource reference. Current reference implementation accept only US-ASCII
   * alphanumeric characters and dash (-), reference mark and separator - see {@link #MARK}, {@link #SEPARATOR}.
   * 
   * @param c character to test.
   * @return true if requested character is valid.
   */
  public static boolean isChar(int c)
  {
    return Character.isLetterOrDigit(c) || c == '-' || c == MARK || c == SEPARATOR;
  }

  /**
   * Get instance hash code.
   * 
   * @return instance hash code.
   * @see #hashCode
   */
  @Override
  public int hashCode()
  {
    return hashCode;
  }

  /**
   * Two resource references are equal if they have the same type and name.
   * 
   * @param obj another reference instance.
   * @return true if this instance equals the given one.
   */
  @Override
  public boolean equals(Object obj)
  {
    if(this == obj) return true;
    if(obj == null) return false;
    if(getClass() != obj.getClass()) return false;
    Reference other = (Reference)obj;
    return this.hashCode == other.hashCode;
  }

  /**
   * Get reference instance string representation.
   * 
   * @return string representation.
   * @see #string
   */
  @Override
  public String toString()
  {
    if(string == null) {
      StringBuilder builder = new StringBuilder();
      builder.append('@');
      builder.append(resourceType.name().toLowerCase());
      builder.append(SEPARATOR);
      if(path != null) {
        builder.append(path);
        builder.append(SEPARATOR);
      }
      builder.append(name);
      string = builder.toString();
    }
    return string;
  }
}
