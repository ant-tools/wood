package js.wood;

import java.util.regex.Matcher;

/**
 * The path to an editable element has component path and editable name. An editable path is used by components to
 * declare OOP inheritance. It is the path to editable element from template, for which component define content.
 * <p>
 * In sample code, <code>wood:template</code> attribute declares a bound to an editable element named <code>body</code>
 * from <code>template/page</code> component.
 * 
 * <pre>
 * &lt;div wood:template="template/page#body"&gt;
 *   // content for template editable element
 * &lt;/div&gt;
 * </pre>
 * <p>
 * An editable element is part of a template layout. Is legal to have more editable elements in a template; for this
 * reason editable path should identify both template component and named editable.
 * 
 * <pre>
 * editable-path = compo-path SEP editable-name
 * editable-name = 1*CH
 * 
 * ; terminal symbols definition
 * SEP = "#"                 ; editable separator
 * CH  = ALPHA / DIGIT / "-" ; character is US-ASCII alphanumeric and dash
 * 
 * ; compo-path nonterminal symbol is described in {@link CompoPath component path}
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * 
 * @author Iulian Rotaru
 * @since 1.0
 * @see js.wood WOOD description
 */
public class EditablePath extends CompoPath
{
  /** Separator for editable name. */
  private static final char EDITABLE_SEPARATOR = '#';

  /** Editable name as defined into template. */
  private String editableName;

  /** Cached value for object string representation. */
  private String string;

  /**
   * Construct editable path instance for value loaded from layout file. This constructor extracts editable name from
   * given path and store it; with what is left delegates {@link CompoPath#CompoPath(Project, FilePath, String) super
   * constructor}. Since editable path is loaded from layout files it is not reliable and uses component constructor
   * that check component path integrity.
   * 
   * @param project project reference,
   * @param layoutPath layout file declaring editable path,
   * @param editablePath editable path value.
   * @throws WoodException if <code>editablePath</code> parameter is invalid or its component path missing.
   */
  public EditablePath(Project project, FilePath layoutPath, String editablePath) throws WoodException
  {
    super(project, layoutPath, compoPath(layoutPath, editablePath));
    editableName = editablePath.substring(editablePath.lastIndexOf(EDITABLE_SEPARATOR) + 1);
  }

  /**
   * Get the component part from an editable path.
   * 
   * @param editablePath editable path.
   * @return component path.
   * @throws WoodException if editable name is missing.
   */
  private static String compoPath(FilePath layoutPath, String editablePath) throws WoodException
  {
    int index = editablePath.lastIndexOf(EDITABLE_SEPARATOR);
    if(index == -1) {
      throw new WoodException("Missing editable name from editable path |%s| declared in layout file |%s|.", layoutPath, editablePath);
    }
    return editablePath.substring(0, index);
  }

  /**
   * Get this editable name.
   * 
   * @return editable name.
   * @see #editableName
   */
  public String getEditableName()
  {
    return editableName;
  }

  /**
   * Get editable path string representation.
   * 
   * @return editable string representation.
   */
  @Override
  public String toString()
  {
    if(string == null) {
      StringBuilder builder = new StringBuilder(value.substring(0, value.length() - 1));
      builder.append(EDITABLE_SEPARATOR);
      builder.append(editableName);
      string = builder.toString();
    }
    return string;
  }

  /**
   * Test if path value is acceptable for editable path instance creation.
   * 
   * @param path path value.
   * @return true if path value match editable pattern.
   */
  public static boolean accept(String path)
  {
    int index = path.lastIndexOf(EDITABLE_SEPARATOR);
    if(index == -1) {
      return false;
    }
    Matcher matcher = PATTERN.matcher(path.substring(0, index));
    return matcher.find();
  }
}
