package js.wood;

/**
 * Resource type, both variables and media files. Resources are external entities referenced from source files. There
 * are two major kinds: variables and media files. Variables are text replaced with their value, media file references
 * are replaced by URL path, absolute or relative.
 * 
 * @author Iulian Rotaru
 */
public enum ResourceType
{
  /** Plain string values mainly for multi-languages support. */
  STRING,

  /**
   * Same as {@link #STRING} but allows for HTML format. Text variable value is a HTML fragment that is imported in
   * place of variable reference. For this reason it is valid only in places where child elements are legal.
   */
  TEXT,

  /** A reusable color, mainly for styles. Color format is that defined by CSS standard. */
  COLOR,

  /** A style dimension or position. It is a numeric value and units with format defined by CSS standard. */
  DIMEN,

  /** Predefined style rules to include into style files. Also known as mixin. */
  STYLE,

  /** Links references to local or third part resources, mainly for <code>href</code> attribute. */
  LINK,

  /** Image file stored on server and URL linked from source files, be it layout, style or script. */
  IMAGE,

  /** The same as {@link #IMAGE} but with audio content. */
  AUDIO,

  /** The same as {@link #IMAGE} but with video content. */
  VIDEO,

  /** Unknown type makes reference using this type as invalid. */
  UNKNOWN;

  /**
   * Test if resource type is a variable. Current implementation consider as variable next types: {@link #STRING},
   * {@link #TEXT}, {@link #COLOR}, {@link #DIMEN}, {@link #STYLE} and {@link #LINK}.
   * 
   * @return true if resource is a variable.
   */
  public boolean isVariable()
  {
    return this == STRING || this == TEXT || this == COLOR || this == DIMEN || this == STYLE || this == LINK;
  }

  /**
   * Test if resource type is a media file. Current implementation consider as media file next types: {@link #IMAGE},
   * {@link #AUDIO} and {@link #VIDEO}.
   * 
   * @return true if resource is media file.
   */
  public boolean isMedia()
  {
    return this == IMAGE || this == AUDIO || this == VIDEO;
  }

  /**
   * Create resource type enumeration from type value, not case sensitive. Returns {@link #UNKNOWN} if given resource
   * type parameter does not denote an enumeration constant.
   * 
   * @param type value of resource type.
   * @return resource type enumeration, possible {@link #UNKNOWN}.
   */
  public static ResourceType getValueOf(String type)
  {
    try {
      return ResourceType.valueOf(type.toUpperCase());
    }
    catch(Exception unused) {}
    return UNKNOWN;
  }
}