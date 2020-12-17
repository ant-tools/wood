package js.wood;

import java.io.IOException;
import java.io.Writer;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.dom.w3c.DocumentBuilderImpl;
import js.util.Strings;

/**
 * Component preview wraps component in standard HTML document and serialize it to given writer. This component preview
 * class is companion for {@link PreviewServlet}; it hides details regarding component layout dynamic generation.
 * <p>
 * When create preview instance a fully aggregated component instance is supplied. This component instance already have
 * variables injected and media file path resolved. Component layout supplies the body for generated preview document.
 * Header is inserted by {@link #serialize(Writer)} method logic. Note that for style and script links preview always
 * uses absolute URL path.
 * 
 * @author Iulian Rotaru
 */
public final class Preview
{
  /** Project reference. */
  private Project project;

  /** Wrapped component. */
  private Component compo;

  /**
   * Create component preview instance.
   * 
   * @param compo component.
   */
  public Preview(Component compo)
  {
    this.project = compo.getProject();
    this.compo = compo;
  }

  /**
   * Create HTML document wrapping this instance component, insert header meta elements, style and script links and
   * serialize to given writer.
   * 
   * @param writer character writer.
   * @throws IOException if document serialization fails.
   */
  public void serialize(Writer writer) throws IOException
  {
    ProjectConfig config = project.getConfig();
    ComponentDescriptor descriptor = compo.getDescriptor();

    DocumentBuilder builder = new DocumentBuilderImpl();
    Document doc = builder.createHTML();

    Element html = doc.getRoot();
    html.setAttr("lang", config.getDefaultLocale().toLanguageTag());
    Element head = doc.createElement("head");
    Element body = doc.createElement("body");
    html.addChild(head).addChild(body);
    head.addText("\r\n");

    head.addChild(doc.createElement("meta", "http-equiv", "Content-Type", "content", "text/html; charset=UTF-8"));
    head.addText("\r\n");

    for(Element meta : config.getMetas()) {
      head.addChild(meta);
      head.addText("\r\n");
    }

    String defaultTitle = Strings.concat(project.getDisplay(), " / ", compo.getDisplay());
    String title = descriptor.getTitle(defaultTitle);
    head.addChild(doc.createElement("title").setText(title));
    head.addText("\r\n");

    String description = descriptor.getDescription(title);
    head.addChild(doc.createElement("meta", "name", "Description", "content", description));
    head.addText("\r\n");

    Element layout = compo.getLayout();
    if(layout.getTag().equals("body")) {
      body.replace(layout);
    }
    else {
      body.addChild(layout);
    }

    // styles link inclusion order is important:
    // 1. third party fonts
    // 2. reset.css
    // 3. fx.css
    // 4. theme styles - theme styles are in no particular order since they are independent of each other
    // 5. component styles - first used template and widgets styles then component

    for(String font : config.getFonts()) {
      addStyle(doc, font);
    }

    for(FilePath stylePath : project.previewThemeStyles()) {
      addStyle(doc, absoluteUrlPath(stylePath));
    }

    for(FilePath stylePath : compo.getStyleFiles()) {
      addStyle(doc, absoluteUrlPath(stylePath));
    }

    // component descriptor third party scripts accept both project file path and absolute URL
    // if file path is used convert to absolute URL path, otherwise leave it as it is since points to foreign server
    for(ComponentDescriptor.Script script : descriptor.getThirdPartyScripts()) {
      String scriptPath = script.getSource();
      if(FilePath.accept(scriptPath)) {
        scriptPath = absoluteUrlPath(scriptPath);
      }
      addScript(doc, scriptPath);
    }

    for(String script : compo.getThirdPartyScripts()) {
      // do not convert to absolute URL path since third party scripts are already absolute URL
      addScript(doc, script);
    }

    // component instance for preview includes preview script and its dependencies, it any
    for(ScriptFile scriptFile : compo.getScriptFiles()) {
      addScript(doc, absoluteUrlPath(scriptFile.getSourceFile()));
    }

    DefaultAttributes.update(doc);
    doc.serialize(writer, true);
  }

  /**
   * Add style link element to HTML document head.
   * 
   * @param doc HTML document,
   * @param href style sheet hyper-reference.
   */
  private static void addStyle(Document doc, String href)
  {
    Element head = doc.getByTag("head");
    head.addChild(doc.createElement("link", "href", href, "rel", "stylesheet", "type", "text/css"));
    head.addText("\r\n");
  }

  /**
   * Add script element to HTML document head.
   * 
   * @param doc HTML document,
   * @param src the source of script.
   */
  private static void addScript(Document doc, String src)
  {
    Element head = doc.getByTag("head");
    head.addChild(doc.createElement("script", "src", src, "type", "text/javascript"));
    head.addText("\r\n");
  }

  /**
   * Build absolute URL path for given file path. Returned path contains project context but not protocol or host name.
   * 
   * @param filePath file path.
   * @return file absolute URL path.
   */
  private String absoluteUrlPath(FilePath filePath)
  {
    return absoluteUrlPath(filePath.value());
  }

  /**
   * Build absolute URL path for given file path value. Returned path contains project context but not protocol or host
   * name.
   * 
   * @param filePath file path value.
   * @return file absolute URL path.
   */
  private String absoluteUrlPath(String filePath)
  {
    return Strings.concat(Path.SEPARATOR, project.getName(), Path.SEPARATOR, filePath);
  }
}
