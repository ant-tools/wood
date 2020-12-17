package js.wood;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
import js.dom.Element;
import js.dom.w3c.DocumentBuilderImpl;
import js.io.VariablesWriter;
import js.util.Files;

/**
 * HTML page document created on build process and serialized on build directory. This class just supplies specialized
 * setters for page DOM document. Is the caller responsibility to ensure proper order when invoke setters.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class PageDocument
{
  /** X(HT)ML document. */
  private Document doc;

  /** HTML root element. */
  private Element html;

  /** HTML head element. */
  private Element head;

  /** HTML body element. */
  private Element body;

  /** Create empty page document without body content. Testing constructor. */
  @SuppressWarnings("unused")
  private PageDocument()
  {
    init();
    this.body = doc.createElement("body");
    this.html.addChild(this.body);
    this.html.addText("\r\n");
  }

  /**
   * Create X(HT)ML document instance, add head element and copy layout from component into body.
   * 
   * @param component component instance containing body layout.
   */
  public PageDocument(Component component)
  {
    init();
    this.body = this.doc.importElement(component.getLayout());
    this.html.addChild(this.body);
    this.html.addText("\r\n");
  }

  /**
   * Instance initialization creates empty X(HT)ML document and head element.
   */
  private void init()
  {
    DocumentBuilder builder = new DocumentBuilderImpl();
    doc = builder.createHTML();
    html = doc.getRoot();
    html.addText("\r\n");
    head = doc.createElement("head");
    html.addChild(head);
    html.addText("\r\n");
    head.addText("\r\n");
  }

  /**
   * Set <code>lang</code> attribute value for HTML root element.
   * 
   * @param language langauge value.
   */
  public void setLanguage(String language)
  {
    html.setAttr("lang", language);
  }

  /**
   * Set document content type. This tool library always uses <code>text/html; charset=UTF-8</code> but this policy is
   * enforced by {@link Builder}.
   * 
   * @param contentType content type.
   */
  public void setContentType(String contentType)
  {
    head.addChild(doc.createElement("meta", "http-equiv", "Content-Type", "content", contentType));
    head.addText("\r\n");
  }

  /**
   * Set page author. If given <code>author</code> is null this method does nothing. Delegates
   * {@link #addMetaData(String, String, String, String)} with name <code>Author</code>.
   * 
   * @param author page author, possible null.
   */
  public void setAuthor(String author)
  {
    if(author != null) {
      addMetaData("name", "Author", "content", author);
    }
  }

  /**
   * Set page header title. If given <code>title</code> is null this method does nothing.
   * 
   * @param title page title, possible null.
   */
  public void setTitle(String title)
  {
    if(title != null) {
      head.addChild(doc.createElement("title").setText(title));
      head.addText("\r\n");
    }
  }

  /**
   * Add page description meta element. Delegates {@link #addMetaData(String, String, String, String)} with name
   * <code>Description</code>. If <code>description</code> parameter is null this setter does nothing.
   * 
   * @param description page description.
   */
  public void setDescription(String description)
  {
    if(description != null) {
      addMetaData("name", "Description", "content", description);
    }
  }

  /**
   * Add link for page favorite icon.
   * 
   * @param path path to favorite icon.
   */
  public void addFavicon(String path)
  {
    head.addChild(doc.createElement("link", "href", path, "rel", "shortcut icon", "type", "image/x-icon"));
    head.addText("\r\n");
  }

  /**
   * Add meta element to header. A meta element is in fact a name/value pair but attribute names differ for certain meta
   * types. After meta element add new line.
   * 
   * @param metaName name of attribute storing the <code>name</code>,
   * @param metaValue value of attribute storing the <code>name</code>,
   * @param dataName name of attribute storing the <code>value</code>,
   * @param dataValue value of attribute storing the <code>value</code>.
   */
  private void addMetaData(String metaName, String metaValue, String dataName, String dataValue)
  {
    head.addChild(doc.createElement("meta", metaName, metaValue, dataName, dataValue));
    head.addText("\r\n");
  }

  /**
   * Add meta elements to document header. This method import and append meta elements, as they are, to this document
   * header in the order from given list. Is legal for <code>metas</code> list parameter to be empty.
   * 
   * @param metas meta elements.
   */
  public void setMetas(EList metas)
  {
    for(Element meta : metas) {
      head.addChild(meta);
      head.addText("\r\n");
    }
  }

  /**
   * Add style file links to this document header. Styles are appended to header in the order from given list.
   * 
   * @param paths URL paths list to style files.
   */
  public void addStyles(List<String> paths)
  {
    for(String path : paths) {
      addStyle(path);
    }
  }

  /**
   * Append style link element to this document head.
   * 
   * @param path style file URL path.
   */
  public void addStyle(String path)
  {
    head.addChild(doc.createElement("link", "href", path, "rel", "stylesheet", "type", "text/css"));
    head.addText("\r\n");
  }

  /**
   * Add script links on this document body end, in the order from given list.
   * 
   * @param paths URL paths list to script files.
   */
  public void addScripts(Set<String> paths)
  {
    for(String path : paths) {
      addScript(path, false);
    }
  }

  /**
   * Add script link to body end or on head if <code>appendToHead</code> parameter is true. Path parameter can be both
   * relative to site or absolute URL for scripts stored on foreign servers.
   * 
   * @param path script source path, relative to site or absolute URL,
   * @param appendToHead append to document head.
   */
  public void addScript(String path, boolean appendToHead)
  {
    Element parent = appendToHead ? head : body;
    parent.addChild(doc.createElement("script", "src", path, "type", "text/javascript"));
    parent.addText("\r\n");
  }

  public void addSDKScript(FilePath scriptPath, String sdkid)
  {
    Map<String, String> variables = new HashMap<String, String>();
    variables.put("sdk-id", sdkid);

    VariablesWriter writer = new VariablesWriter(variables);
    try {
      Files.copy(scriptPath.getReader(), writer);
    }
    catch(IOException e) {
      throw new WoodException(e);
    }

    Element script = doc.createElement("script", "type", "text/javascript");
    script.setRichText("\r\n" + writer.toString());

    head.addChild(script);
    head.addText("\r\n");
  }

  /**
   * Return this page document.
   * 
   * @return this page document.
   * @see #doc
   */
  public Document getDocument()
  {
    return doc;
  }
}
