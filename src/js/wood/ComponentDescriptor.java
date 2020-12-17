package js.wood;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.dom.w3c.DocumentBuilderImpl;
import js.util.Classes;

/**
 * Component descriptor contains properties customizable at component level. This is in contrast with
 * {@link ProjectConfig} that affects all components from project. Current implementation is actually used for pages;
 * support for ordinary components is expected.
 * <p>
 * Here are current implemented values:
 * <table border="1" style="border-collapse:collapse;">
 * <tr>
 * <td><b>Element
 * <td><b>Description
 * <td><b>Usage
 * <td><b>Sample Value
 * <tr>
 * <td>version
 * <td>component version especially useful for library components
 * <td>library logic does not use it but developer may want to know it
 * <td>1.2.3
 * <tr>
 * <td>title
 * <td>component title used to identify component on user interfaces
 * <td>current implementation uses title for page head <code>title</code> element
 * <td>Index Page
 * <tr>
 * <td>description
 * <td>component description is a concise explanation of the component content
 * <td>current implementation insert description into page head using <code>meta</code> element
 * <td>Index page description.
 * <tr>
 * <td>path
 * <td>directories path to store page layout into; build file system insert this directories path just before layout
 * file
 * <td>useable for role based security supplied by servlet container
 * <td>/admin/
 * <tr>
 * <td>scripts
 * <td>contains path to third party scripts specific to component; both project file path and absolute URL are accepted
 * <td>scripts are included into page document in the defined order
 * <td>https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js
 * </table>
 * <p>
 * For convenience below is a sample configuration file. Element values can be replaced with string variables. For
 * example <code>title</code> value can be something like <code>@string/page-title</code>. This class uses
 * {@link ReferencesResolver} to replace variables with their defined values.
 * <p>
 * Third party scripts are usually linked at the page bottom, just before closing page body. Anyway, <code>script</code>
 * element has an optional boolean attribute, <code>append-to-head</code> to force including script link at the end of
 * the page header.
 * 
 * <pre>
 *  &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 *  &lt;page&gt;
 *      &lt;version&gt;1.2.3&lt;/version&gt;
 *      &lt;title&gt;Index Page&lt;/title&gt;
 *      &lt;description&gt;Index page description.&lt;/description&gt;
 *      &lt;path&gt;/admin/&lt;/path&gt;
 *      &lt;scripts&gt;
 *          &lt;script append-to-head="true"&gt;https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js&lt;/script&gt;
 *          &lt;script&gt;http://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js&lt;/script&gt;
 *      &lt;/scripts&gt;
 *  &lt;/page&gt;
 * </pre>
 * 
 * <p>
 * Directories path for page layout files is described by <code>path</code> element. Usually page layout files are
 * stored in a directory configured by build file system implementation, see {@link BuildFS#getPageDir()}. For example,
 * in default file system pages are stored into build root. If <code>path</code> element is specified, given path is
 * used to stored page layout file. In example below, <code>user-manager.htm</code> page is stored under
 * <code>admin</code> directory. This facility is useable for role based security; it allows to group pages under given
 * path and configure security constrains base on that path.
 *
 * <pre>
 *  /
 *  /admin/
 *        +-user-manager.htm
 *        ~
 *  /info/
 *        +-log.viewer.htm
 *        ~                          
 *  /media/
 *  /script/
 *  /style/
 *  +-login.htm
 *  ~
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 * @see ProjectConfig
 */
public class ComponentDescriptor
{
  /** Empty XML document used when component descriptor file is missing. */
  private static final Document EMPTY_DOC;
  static {
    DocumentBuilder builder = new DocumentBuilderImpl();
    EMPTY_DOC = builder.createXML("component");
  }

  /** XML DOM document. */
  private final Document doc;

  /** File path for component descriptor. It has owning component name and XML extension. */
  private final FilePath filePath;

  /** References handler used to actually process referenced resources; defined externally. */
  private final ReferenceHandler referenceHandler;

  /**
   * Resolver parses element values and invoke {@link #referenceHandler} for discovered references, if any. It is
   * necessary because is legal for element value to contain resource references.
   */
  private final ReferencesResolver resolver;

  /**
   * Create component descriptor instance and initialize it from given file. Values defined by descriptor may contain
   * resource references that need to be resolved. This descriptor uses external defined references handler just for
   * that.
   * 
   * @param filePath descriptor file path,
   * @param referenceHandler resource references handler.
   */
  public ComponentDescriptor(FilePath filePath, ReferenceHandler referenceHandler)
  {
    this.filePath = filePath;
    this.referenceHandler = referenceHandler;
    this.resolver = new ReferencesResolver();
    try {
      DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
      this.doc = this.filePath.exists() ? builder.loadXML(this.filePath.toFile()) : EMPTY_DOC;
    }
    catch(FileNotFoundException unused) {
      throw new WoodException("Missing project configuration file |%s| although it exists.", this.filePath);
    }
  }

  /**
   * Get component version or null if missing or not set. This property is loaded from <code>version</code> element.
   * Note that version property is especially useful for library components.
   * 
   * @return component version or null.
   */
  public String getVersion()
  {
    return value("version", null);
  }

  /**
   * Get component title or given default value, if title is missing or not set. This property is loaded from
   * <code>title</code> element.
   * 
   * @param defaultValue default title value.
   * @return component title or supplied default value.
   */
  public String getTitle(String defaultValue)
  {
    return value("title", defaultValue);
  }

  /**
   * Get component description or given default value, if description is missing or not set. This property is loaded
   * from <code>description</code> element.
   * 
   * @param defaultValue default description value.
   * @return component description or supplied default value.
   */
  public String getDescription(String defaultValue)
  {
    return value("description", defaultValue);
  }

  /**
   * Get directory path to store page layout file or supplied default value if path is missing. This property is loaded
   * from <code>path</code> element.
   * 
   * @param defaultValue default path value.
   * @return page layout directory path or supplied default value.
   */
  public String getPath(String defaultValue)
  {
    return value("path", defaultValue);
  }

  /**
   * Get third party scripts defined by this component descriptor. Returns a list of paths in the order and in format
   * defined into descriptor. There is no attempt to check path validity; it is developer responsibility to ensure paths
   * are correct and inclusion order is proper.
   * <p>
   * Here is expected scripts descriptor format.
   * 
   * <pre>
   * &lt;scripts&gt;
   *    &lt;script append-to-head="true"&gt;https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js&lt;/script&gt;
   *    &lt;script&gt;http://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js&lt;/script&gt;
   * &lt;/scripts&gt;
   * </pre>
   * <p>
   * If optional attribute <code>append-to-head</code> is present into <code>script</code> element enable
   * {@link Script#appendToHead}.
   * 
   * @return third party script defined by descriptor.
   */
  public List<Script> getThirdPartyScripts()
  {
    // do not attempt to cache script paths since this method is expected to be used only once

    Element scriptsEl = doc.getByTag("scripts");
    if(scriptsEl == null) {
      return Collections.emptyList();
    }

    List<Script> scripts = new ArrayList<Script>();
    for(Element scriptEl : scriptsEl.getChildren()) {
      boolean appendToHead = Boolean.parseBoolean(scriptEl.getAttr("append-to-head"));
      scripts.add(new Script(scriptEl.getText(), appendToHead));
    }
    return scripts;
  }

  /**
   * Return text value from element or null if element not found or value not set. Uses {@link ReferencesResolver} to
   * resolve value references, if any.
   * 
   * @param tag tag name identifying desired element.
   * @return element text value or null.
   */
  private String value(String tag, String defaultValue)
  {
    Element el = doc.getByTag(tag);
    if(el == null) {
      return defaultValue;
    }
    String value = el.getText();
    if(value.isEmpty()) {
      return defaultValue;
    }
    return resolver.parse(value, filePath, referenceHandler);
  }

  /**
   * Third party script descriptor contains script source and flag to append to document head. This class is loaded from
   * <code>scripts</code> section of the component descriptor, see snippet below. It is used to declare third party
   * scripts.
   * <p>
   * 
   * <pre>
   * &lt;scripts&gt;
   *    &lt;script&gt;http://code.jquery.com/jquery-1.7.min.js&lt;/script&gt;
   *    &lt;script&gt;http://sixqs.com/site/js/lib/qtip.js&lt;/script&gt;
   * &lt;/scripts&gt;
   * </pre>
   * 
   * @author Iulian Rotaru
   */
  public static class Script
  {
    /** Script source is the URL from where third script is to be loaded. */
    private String source;
    /**
     * Usually scripts are inserted into page document at the bottom, after body content. This flag is used to force
     * script loading on document header.
     */
    private boolean appendToHead;

    public Script(String source, boolean appendToHead)
    {
      this.source = source;
      this.appendToHead = appendToHead;
    }

    public String getSource()
    {
      return source;
    }

    public boolean isAppendToHead()
    {
      return appendToHead;
    }
  }
}
