package js.wood;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import js.log.Log;
import js.log.LogFactory;
import js.util.Strings;

/**
 * Project builder for user interface resources. Builder acts as a bridge between {@link Project} and {@link BuildFS}.
 * It reads component source and resource files from project, consolidates them into pages then write to build site
 * directory. Build class implements {@link #onResourceReference(Reference, FilePath)} used to process resource
 * references declared into source files.
 * <p>
 * Using this class is pretty straightforward: create instance providing project directory then just invoke
 * {@link #build()}. Optionally, one may set the build number. Build site directory is controlled by build process and
 * is project configurable; it can be obtaining via {@link #getSitePath()}.
 * 
 * <pre>
 * Builder builder = new Builder(projectDir);
 * builder.setBuildNumber(buildNumber);
 * builder.build();
 * ...
 * String sitePath = builder.getSitePath();
 * </pre>
 * 
 * Builder interface is designed for integration with external tools. There are extensions for Ant tasks and Eclipse
 * plug-in using this Builder class, but not part of this library distribution.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public class Builder implements ReferenceHandler
{
  /** Class logger. */
  private static final Log log = LogFactory.getLog(Builder.class);

  /** Project instance. */
  private Project project;

  /** Project discovered pages. */
  private Collection<CompoPath> pages = new ArrayList<CompoPath>();

  /** Cache for resource variables. */
  private Map<DirPath, Variables> variables;

  /** Current processing build file system, that is, build site directory. */
  private BuildFS buildFS;

  /** Current processing locale variant. */
  private Locale locale;

  /**
   * Construct builder instance. Create {@link Project} instance with given project root directory. Scan for project
   * layout and script files and initialize project pages and variables map. Create build FS instance.
   * 
   * @param projectPath path to project root directory.
   */
  public Builder(String projectPath)
  {
    log.trace("Builder(String)");
    this.project = new Project(projectPath);
    this.project.scanBuildFiles();

    // scan project layout files then initialize pages list and global variables map
    for(LayoutFile layoutFile : this.project.getLayouts()) {
      if(layoutFile.isPage()) {
        this.pages.add(layoutFile.getCompoPath());
      }
    }

    this.variables = this.project.getVariables();
    this.buildFS = new DefaultBuildFS(project);
  }

  /**
   * Set build number.
   * 
   * @param buildNumber build number.
   */
  public void setBuildNumber(int buildNumber)
  {
    buildFS.setBuildNumber(buildNumber);
  }

  /**
   * Set site build directory.
   * 
   * @param siteDir site build directory, relative to project root.
   */
  public void setSiteDir(File siteDir)
  {
    project.setSiteDir(siteDir);
  }

  public void setNamingStrategy(NamingStrategy namingStrategy)
  {
    project.setNamingStrategy(namingStrategy);
  }

  /**
   * Get the site build directory path, relative to project. Returned value is that from project configuration - see
   * {@link ProjectConfig#getSiteDir(String)}, or default value {@link CT#DEF_SITE_DIR}. Path is guaranteed to have
   * trailing file separator.
   * 
   * @return site build path.
   */
  public String getSitePath()
  {
    return project.getSitePath();
  }

  /**
   * Run project building process. Uses project detected locale(s) to initialize current processing {@link #locale} then
   * delegates {@link #buildPage(CompoPath)} for every discovered page, see {@link #pages}.
   * 
   * @throws IOException for error related to underlying file system operations.
   */
  public void build() throws IOException
  {
    for(Locale locale : project.getLocales()) {
      this.locale = locale;
      buildFS.setLocale(locale);

      for(CompoPath page : pages) {
        buildPage(page);
      }
    }
  }

  private ThreadLocal<Component> currentCompo = new ThreadLocal<Component>();

  /**
   * Build page identified by given component path and copy to build directory. Component should be designed for page
   * generation so that it should have body root or to use a template that has body.
   * <p>
   * This method create a page document with the component instance and initialize it as follow:
   * <ul>
   * <li>create X(HT)ML document with header and body loaded from component layout,
   * <li>set content type, metas from project configuration and page title,
   * <li>add header styles in order: third party fonts, reset and key-frames, component styles and themes,
   * <li>include script files at the body end,
   * <li>update default values for missing attributes,
   * <li>serialize document to build page directory.
   * </ul>
   * <p>
   * Be aware that {@link #locale} should be properly initialized before calling this method, if project has
   * multi-locale support.
   * 
   * @param compoPath page component path.
   * @throws IOException if files operation fails.
   */
  private void buildPage(CompoPath compoPath) throws IOException
  {
    log.debug("Build page |%s|.", compoPath);

    Component compo = new Component(compoPath, this);
    currentCompo.set(compo);
    compo.scan(false);
    PageDocument page = new PageDocument(compo);

    ProjectConfig config = project.getConfig();
    ComponentDescriptor descriptor = compo.getDescriptor();

    page.setLanguage((locale != null ? locale : config.getDefaultLocale()).toLanguageTag());
    page.setContentType("text/html; charset=UTF-8");
    page.setMetas(config.getMetas());
    page.setAuthor(config.getAuthor());

    // descriptor is page specific and may contain <title> and <description>
    // if <title> is missing uses project / compo display and if <description> is missing use <title>
    String title = descriptor.getTitle(Strings.concat(project.getDisplay(), " / ", compo.getDisplay()));
    page.setTitle(title);
    page.setDescription(descriptor.getDescription(title));

    if(project.getFavicon().exists()) {
      page.addFavicon(buildFS.writeFavicon(compo, project.getFavicon()));
    }

    page.addStyles(config.getFonts());
    for(FilePath style : project.getThemeStyles()) {
      page.addStyle(buildFS.writeStyle(compo, style, this));
    }
    for(FilePath style : compo.getStyleFiles()) {
      page.addStyle(buildFS.writeStyle(compo, style, this));
    }

    // include SDK initialization scripts into header in order to have early initialization
    // anyway, well behaving SDK scripts are loading asynchronously
    for(FilePath script : project.getSDKScripts()) {
      // by convention SDK script base name is the same as configuration element
      page.addSDKScript(script, config.getSDKID(script.getBaseName()));
    }

    // component descriptor third party scripts accept both project file path and absolute URL
    // if file path is used copy the script to build FS, otherwise script is stored on foreign server
    for(ComponentDescriptor.Script script : descriptor.getThirdPartyScripts()) {
      String scriptPath = script.getSource();
      if(FilePath.accept(scriptPath)) {
        scriptPath = buildFS.writeScript(compo, project.getFile(scriptPath), this);
      }
      page.addScript(scriptPath, script.isAppendToHead());
    }

    // component third party script are served from foreign servers and need not to be copied into build FS
    page.addScripts(compo.getThirdPartyScripts());

    for(ScriptFile script : compo.getScriptFiles()) {
      page.addScript(buildFS.writeScript(compo, script.getSourceFile(), this), false);
    }

    DefaultAttributes.update(page.getDocument());

    buildFS.writePage(compo, page);
  }

  /**
   * Resource reference handler invoked by {@link SourceReader} when discover a resource reference into source file.
   * Returned value is used by source reader to replace the reference, into source file. If reference points to a
   * variable, returns its value or null if not found. If media file, copy it to build media directory then return build
   * media file path.
   * 
   * @param reference resource reference,
   * @param source source file where <code>reference</code> is used.
   * @return value to replace reference on source file.
   * @throws IOException if media file write operation fails.
   * @throws WoodException if directory variables or media file is missing.
   */
  @Override
  public String onResourceReference(Reference reference, FilePath source) throws IOException, WoodException
  {
    if(reference.isVariable()) {
      Variables dirVariables = variables.get(source.getDirPath());
      if(dirVariables == null) {
        throw new WoodException("Missing variable value for reference |%s:%s|.", source, reference);
      }
      if(locale == null) {
        throw new IllegalStateException("Builder locale not initialized.");
      }
      return dirVariables.get(locale, reference, source, this);
    }

    FilePath mediaFile = project.getMediaFile(locale, reference, source);
    if(mediaFile == null) {
      throw new WoodException("Missing media file for reference |%s:%s|.", source, reference);
    }

    switch(source.getType()) {
    case LAYOUT:
      return buildFS.writePageMedia(currentCompo.get(), mediaFile);

    case STYLE:
      return buildFS.writeStyleMedia(mediaFile);

    case SCRIPT:
      return buildFS.writeScriptMedia(mediaFile);

    default:
    }
    return null;
  }
}
