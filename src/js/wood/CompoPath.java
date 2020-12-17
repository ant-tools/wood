package js.wood;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.util.Strings;

/**
 * Component path is used by layout files to declare editable elements and widgets bounds. It is the mean to implement
 * inheritance and composition. Component path identifies a component under UI resources or library source directories.
 * Project directory segments are mapped to component path elements.
 * <p>
 * Here is a sample usage for component path. Template attribute declare a reference to and editable element from a
 * template whereas widget attribute is a reference to an widget. For full template reference description see
 * {@link EditablePath}.
 * 
 * <pre>
 * &lt;div wood:template="template/page#body"&gt;
 *   . . .
 *   &lt;div wood:widget="compo/three-no"&gt;&lt;/div&gt;
 * &lt;/div&gt;
 * </pre>
 * <p>
 * Below is component path syntax. Component path starts with an optional source directory followed by a file separator
 * then an optional number of path segments and mandatory component name. If trailing path separator is missing takes
 * care to add it. Both path segment and component names uses US-ASCII alphanumeric characters and dash (-). If source
 * directory is missing uses UI resources.
 * 
 * <pre>
 * compo-path   = [source-dir SEP] *path-segment compo-name [SEP]
 * source-dir   = RES / LIB ; RES is default value
 * path-segment = name SEP
 * compo-name   = name
 * name         = 1*CH
 * 
 * ; terminal symbols definition
 * RES = "res"               ; UI resources
 * LIB = "lib"               ; third-party Java archives and UI components
 * SEP  = "/"                ; file separator 
 * CH  = ALPHA / DIGIT / "-" ; character is US-ASCII alphanumeric and dash
 * 
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * <p>
 * As stated, component directory segments are mapped on component path elements:
 * <ul>
 * <li>first director is source directory, optional,
 * <li>last director is component name,
 * <li>the rest are component path segments, optional.
 * </ul>
 * <p>
 * In ASCII diagram we have a sample project file system with couple components and relation between project directories
 * structure and components path.
 * 
 * <pre>
 * project /                              
 *         ~                              
 *         / lib /                        
 *         |     / paging /          --> lib/paging
 *         ~ 
 *         / res /
 *         |     / template /
 *         |     |          / page / --> template/page
 *         ~     ~
 *         |     / page /
 *         |     |      / index /    --> page/index
 * </pre>
 * 
 * @author Iulian Rotaru
 * @since 1.0
 * @see js.wood WOOD description
 */
public class CompoPath extends DirPath
{
  /** Component path pattern. */
  protected static final Pattern PATTERN = Pattern.compile("^(" + //
      "(?:(res|lib)/)?" + // source directory
      "(?:[\\w-]+/)*" + // path segments
      "[\\w-]+" + // component name
      ")/?$", //
      Pattern.CASE_INSENSITIVE);

  /**
   * Create component path instance with safe path value. This constructor is designed to be called with verified path
   * value and does not attempt to test its validity or if directory actually exist. Attempting to use this constructor
   * with invalid path value will conclude in not predictable behavior.
   * 
   * @param project project reference,
   * @param compoPath component path value.
   */
  public CompoPath(Project project, String compoPath)
  {
    super(project, value(compoPath));
  }

  /**
   * Create component path instance with path value read from layout file. Since path value is not reliable this
   * constructor test its integrity and throws exception if invalid or not found.
   * 
   * @param project project reference,
   * @param layoutPath path to layout file declaring the component path,
   * @param compoPath component path value.
   * @throws WoodException if <code>compoPath</code> parameter does not match pattern or directory is missing.
   */
  public CompoPath(Project project, FilePath layoutPath, String compoPath) throws WoodException
  {
    this(project, value(compoPath));
    if(value == null) {
      throw new WoodException("Invalid component path |%s| declared in layout file |%s|.", compoPath, layoutPath);
    }
    if(!exists()) {
      throw new WoodException("Missing component path |%s| requested from layout file |%s|.", compoPath, layoutPath);
    }
  }

  /**
   * Return normalized path value with source directory forced to UI resources, if missing. Component syntax allows for
   * optional source directory with default to UI resources. This method takes care to insert it, if missing. Also check
   * path syntax against {@link #PATTERN} and throws exception if invalid.
   * 
   * @param compoPath component path value.
   * @return normalized path value.
   * @throws WoodException if component path parameter has bad syntax.
   */
  private static String value(String compoPath) throws WoodException
  {
    Matcher matcher = PATTERN.matcher(compoPath);
    if(!matcher.find()) {
      throw new WoodException("Invalid component path |%s|.", compoPath);
    }
    String path = matcher.group(1);
    String sourceDir = matcher.group(2);
    return sourceDir != null ? path : Strings.concat(CT.RESOURCE_DIR, Path.SEPARATOR, path);
  }

  /**
   * Get this component name.
   * 
   * @return component name.
   */
  public String getName()
  {
    return file.getName();
  }

  /**
   * Get file path for this component layout.
   * 
   * @return component layout.
   */
  public FilePath getLayoutPath()
  {
    return getFilePath(getName() + CT.DOT_LAYOUT_EXT);
  }

  /**
   * Test if path value is acceptable for component path instance creation.
   * 
   * @param path path value.
   * @return true if path value match component pattern.
   */
  public static boolean accept(String path)
  {
    Matcher matcher = PATTERN.matcher(path);
    return matcher.find();
  }
}
