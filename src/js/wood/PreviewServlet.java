package js.wood;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.security.Principal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import js.container.ContainerSPI;
import js.core.Factory;
import js.http.ContentType;
import js.json.Json;
import js.lang.Callback;
import js.rmi.BusinessException;
import js.servlet.RequestContext;
import js.servlet.TinyContainer;
import js.util.Classes;
import js.util.Files;
import js.util.Strings;

/**
 * Preview servlet allows access from browser to project components and related files. This allows to use browser for
 * components preview by simple refreshing loaded page. A file is returned as it is but component layout is aggregated
 * and preview HTML code is generated on the fly; uses {@link Preview} for that.
 * <p>
 * In order to work preview servlet should be properly declared into web.xml file:
 * 
 * <pre>
 *  &lt;context-param&gt;
 *      &lt;param-name&gt;PROJECT_DIR&lt;/param-name&gt;
 *      &lt;param-value&gt;path/to/project&lt;/param-value&gt;
 *  &lt;/context-param&gt;
 *  
 *  &lt;listener&gt;
 *      &lt;listener-class&gt;js.wood.PreviewServlet&lt;/listener-class&gt;
 *  &lt;/listener&gt;
 *  
 *  &lt;servlet&gt;
 *      &lt;servlet-name&gt;development&lt;/servlet-name&gt;
 *      &lt;servlet-class&gt;js.wood.PreviewServlet&lt;/servlet-class&gt;
 *      &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 *  &lt;/servlet&gt;
 *  
 *  &lt;servlet-mapping&gt;
 *      &lt;servlet-name&gt;development&lt;/servlet-name&gt;
 *      &lt;url-pattern&gt;/&lt;/url-pattern&gt;
 *  &lt;/servlet-mapping&gt;
 * </pre>
 * 
 * Preview WAR archive is actually empty; it contains only <code>web.xml</code> descriptor, that is, no Java classes nor
 * static content.
 * 
 * @author Iulian Rotaru
 */
public final class PreviewServlet extends HttpServlet implements ReferenceHandler
{
  /** Java serialization ID, */
  private static final long serialVersionUID = -2971057399517075118L;

  /** Servlet context init parameter for project directory. */
  private static final String PROJECT_DIR_PARAM = "PROJECT_DIR";
  /** Context parameter for naming strategy. */
  private static final String NAMING_STRATEGY_PARAM = "NAMING_STRATEGY";

  private static final String PREVIEW_CALLBACK_PARAM = "PREVIEW_CALLBACK";

  /** Parent container. */
  private ContainerSPI container;

  /** Project instance initialized from Servlet context parameter on Servlet initialization. */
  private Project project;

  /** Preview callback defined by user code. */
  private Callback<ContainerSPI> previewCallback;

  /**
   * Preview layout is a special layout used for component unit test. It is returned instead of component; preview
   * layout uses component layout as widget.
   */
  private static final String LAYOUT_PREVIEW = "preview.htm";

  /** Variables cache initialized before every component preview processing. */
  private static VariablesCache variablesCache = new VariablesCache();

  private Json json;

  /**
   * Servlet instance initialization. This hook is invoked by Servlet container when first create preview Servlet
   * instance. Since this Servlet is declared <code>load-on-startup</code> this initialization occurs at application
   * deployment.
   * <p>
   * For every deployed preview context/application, Servlet container creates exactly one instance of this preview
   * Servlet and invoke this method once. This method takes care to create {@link #project} instance with proper root
   * directory loaded from initialization parameters.
   */
  @Override
  public void init(ServletConfig config) throws ServletException
  {
    ServletContext context = config.getServletContext();
    container = (ContainerSPI)context.getAttribute(TinyContainer.ATTR_INSTANCE);
    project = new Project(context.getInitParameter(PROJECT_DIR_PARAM));

    String namingStrategyValue = context.getInitParameter(NAMING_STRATEGY_PARAM);
    if(namingStrategyValue == null) {
      project.setNamingStrategy(NamingStrategy.XMLNS);
    }
    else {
      project.setNamingStrategy(NamingStrategy.valueOf(namingStrategyValue));
    }

    String previewCallbackValue = context.getInitParameter(PREVIEW_CALLBACK_PARAM);
    if(previewCallbackValue != null) {
      previewCallback = Classes.newInstance(previewCallbackValue);
    }

    json = Classes.loadService(Json.class);
  }

  /**
   * Servlet service routine just delegates {@link #doService(HttpServletRequest, HttpServletResponse)} and print
   * exception stack.
   * 
   * @param httpRequest HTTP request,
   * @param httpResponse HTTP response.
   */
  @Override
  protected void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException
  {
    try {
      Factory.bind(container);

      RequestContext context = Factory.getInstance(RequestContext.class);
      context.attach(httpRequest, httpResponse);

      if(!container.isAuthenticated()) {
        container.login(new PreviewUser());
      }

      if(previewCallback != null) {
        previewCallback.handle(container);
      }

      doService(httpRequest, httpResponse);
    }
    catch(BusinessException e) {}
    catch(Throwable e) {
      if(e.getCause() instanceof BusinessException) {
        sendThrowable(httpResponse, HttpServletResponse.SC_BAD_REQUEST, e.getCause());
        return;
      }
      throw new ServletException(e);
    }
  }

  /**
   * Process requests from browser for component layout and all dependent files.
   * 
   * @param httpRequest HTTP request,
   * @param httpResponse HTTP response.
   * @throws Exception exceptions from preview process are thrown to caller.
   */
  private void doService(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception
  {
    httpResponse.setCharacterEncoding("UTF-8");

    // request path is request URI without context; it does not starts with a path separator
    String requestPath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length() + 1);

    if(RmiRequestHandler.accept(requestPath)) {
      RmiRequestHandler handler = new RmiRequestHandler(project, requestPath);
      handler.service(httpRequest, httpResponse);
      return;
    }

    if(RestRequestHandler.accept(requestPath)) {
      RestRequestHandler handler = new RestRequestHandler(project, requestPath);
      handler.service(httpRequest, httpResponse);
      return;
    }

    if(ResourceRequestHandler.accept(requestPath)) {
      ResourceRequestHandler handler = new ResourceRequestHandler(project, httpRequest);
      handler.service(httpRequest, httpResponse);
      return;
    }

    if(CompoPath.accept(requestPath)) {
      CompoPath compoPath = new CompoPath(project, requestPath);
      FilePath layoutPath = compoPath.getLayoutPath();

      // if component has preview layout uses it instead of component layout
      // preview layout should use component as widget
      FilePath layoutPreview = compoPath.getFilePath(LAYOUT_PREVIEW);
      if(layoutPreview.exists()) {
        layoutPath = layoutPreview;
      }

      // rescan project script files and update variables cache every time a component is requested
      project.previewScriptFiles();
      variablesCache.update(project);

      // create component with support for preview script
      Component component = new Component(layoutPath, this);
      component.scan(true);
      Preview preview = new Preview(component);
      preview.serialize(httpResponse.getWriter());
      return;
    }

    FilePath filePath = project.getFile(requestPath);
    if(filePath.isStyle()) {
      if(!filePath.hasVariants()) {
        Reader reader = new SourceReader(new StyleReader(filePath), filePath, this);
        Files.copy(reader, httpResponse.getWriter());
      }
      return;
    }

    if(filePath.isScript()) {
      Reader reader = new SourceReader(filePath, this);
      Files.copy(reader, httpResponse.getWriter());
      return;
    }

    // all other files are just sent back to browser
    if(!filePath.exists() && filePath.isMedia()) {
      filePath = filePath.getVariant("en");
      if(!filePath.exists()) {

      }
    }
    Files.copy(filePath.toFile(), httpResponse.getOutputStream());
  }

  /**
   * Handler for resource references takes care of variables injection and media file processing.
   * 
   * @param reference resource references,
   * @param source source containing the resource references.
   * @return resource variable values or media file source.
   */
  @Override
  public String onResourceReference(Reference reference, FilePath source)
  {
    Locale previewLocale = new Locale("en");
    if(reference.isVariable()) {
      String value = variablesCache.getVariables(project, source).get(previewLocale, reference, source, this);
      if(value == null) {
        value = reference.toString();
      }
      return value;
    }

    // discover media file and returns its absolute URL path
    FilePath mediaFile = project.getMediaFile(previewLocale, reference, source);
    if(mediaFile == null) {
      throw new WoodException("Missing media file for reference |%s| from source |%s|.", reference, source);
    }
    return Strings.concat(Path.SEPARATOR, project.getName(), Path.SEPARATOR, mediaFile.getDirPath().value(), mediaFile.getName());
  }

  /**
   * Send throwable back to preview browser.
   * 
   * @param httpResponse HTTP response,
   * @param responseCode HTTP response code,
   * @param throwable throwable instance.
   * @throws IOException if sending throwable fails.
   */
  private void sendThrowable(HttpServletResponse httpResponse, int responseCode, Throwable throwable) throws IOException
  {
    httpResponse.setStatus(responseCode);
    httpResponse.setContentType(ContentType.APPLICATION_JSON.getValue());

    StringWriter buffer = new StringWriter();
    json.stringify(buffer, throwable);

    byte[] bytes = buffer.toString().getBytes("UTF-8");
    httpResponse.setContentLength(bytes.length);
    httpResponse.getOutputStream().write(bytes);
    httpResponse.getOutputStream().flush();
  }

  /**
   * Variable caches used to speed up preview process. Cache life span last for a component preview session.
   * 
   * @author Iulian Rotaru
   */
  private static class VariablesCache
  {
    /** Components variables. */
    private Map<Path, Variables> compoVariables = new HashMap<Path, Variables>();

    /** Project asset variables. */
    private Variables assetVariables;

    /** Theme variables. */
    private Variables themeVariables;

    /**
     * Initialize variables cache by cleaning component variables hash and rescanning assets and site styles
     * directories.
     * 
     * @param project parent project.
     */
    public synchronized void update(Project project)
    {
      compoVariables.clear();
      assetVariables = new Variables(project, project.getAssetsDir());
      themeVariables = new Variables(project, project.getThemeDir());
    }

    /**
     * Return cached component variables. If variables are not already on hash create new instance and store before
     * return it. Component to return variables for is identified by given source file.
     * 
     * @param sourcePath component source file.
     * @return component variables.
     */
    public synchronized Variables getVariables(Project project, FilePath sourcePath)
    {
      Variables variables = compoVariables.get(sourcePath.getDirPath());
      if(variables == null) {
        variables = new Variables(project, sourcePath.getDirPath());
        variables.setAssetVariables(assetVariables);
        variables.setThemeVariables(themeVariables);
        compoVariables.put(sourcePath.getDirPath(), variables);
      }
      return variables;
    }
  }

  private static class PreviewUser implements Principal
  {
    @Override
    public String getName()
    {
      return "preview-user";
    }
  }
}