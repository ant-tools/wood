package js.wood;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
import js.dom.Element;
import js.dom.w3c.DocumentBuilderImpl;
import js.util.Strings;

/**
 * Project configuration file. This file is located into project root directory with the name <code>project.xml</code>.
 * It is mandatory and constructor throws exception if not found it. It contains customizable values for project.
 * <p>
 * Here are current implemented configuration values:
 * <table>
 * <tr>
 * <td><b>Element
 * <td><b>Description
 * <td><b>Sample Value
 * <td><b>Default Value
 * <tr>
 * <td>name
 * <td>project name used for internal representation
 * <td>test-project
 * <td>project directory name
 * <tr>
 * <td>display
 * <td>project name for user interface
 * <td>Test Project
 * <td>project name to title case
 * <tr>
 * <td>description
 * <td>project description for user interface
 * <td>Project used as fixture for unit testing.
 * <td>project display
 * <tr>
 * <td>language
 * <td>project language or comma separated languages list if project is multi-languages
 * <td>en, ro, de, jp
 * <td>
 * <tr>
 * <td>site-dir
 * <td>the path of directory used for site build, relative to project root
 * <td>build/site
 * <td>build/site
 * <tr>
 * <td>analytics
 * <td>Google Analytics account ID used to include analytics script in all pages
 * <td>UA-12345678-1
 * <td>
 * <tr>
 * <td>facebook
 * <td>Facebook SDK application ID used to include Facebook SDK initialization script in all pages
 * <td>123456789012345
 * <td>
 * <tr>
 * <td>meta
 * <td>page header meta elements ready to be inserted into generated site pages
 * <td>&lt;meta http-equiv="X-UA-Compatible" content="IE=9; IE=8; IE=7; IE=EDGE" /&gt;
 * <td>
 * <tr>
 * <td>font
 * <td>third party font URL usable for href attribute in page header link element
 * <td>http://fonts.googleapis.com/css?family=Open+Sans
 * <td>
 * <tr>
 * <td>excludes
 * <td>comma or space separated component paths excluded from build process; preview is still usable
 * <td>page/about, page/license
 * <td>
 * </table>
 * <p>
 * For convenience here is a sample configuration file.
 * 
 * <pre>
 *  &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 *  &lt;project&gt;
 *      &lt;name&gt;project&lt;/name&gt;
 *      &lt;display&gt;Test Project&lt;/display&gt;
 *      &lt;description&gt;Project used as fixture for unit testing.&lt;/description&gt;
 *      &lt;language&gt;en, ro&lt;/language&gt;
 *      
 *      &lt;site-dir&gt;build/site&lt;/site-dir&gt;
 *  
 *      &lt;analytics&gt;UA-12345678-1&lt;/analytics&gt;
 *      &lt;facebook&gt;123456789012345&lt;/facebook&gt;
 *   
 *      &lt;metas&gt;
 *          &lt;meta http-equiv="X-UA-Compatible" content="IE=9; IE=8; IE=7; IE=EDGE" /&gt;
 *          &lt;meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" /&gt;
 *      &lt;/metas&gt;
 *  
 *      &lt;fonts&gt;
 *          &lt;font&gt;http://fonts.googleapis.com/css?family=Roboto&lt;/font&gt;
 *          &lt;font&gt;http://fonts.googleapis.com/css?family=Great+Vibes&lt;/font&gt;
 *      &lt;/fonts&gt;
 *      
 *      &lt;excludes&gt;page/about, compo/video-player&lt;/excludes&gt;
 *  &lt;/project&gt;
 * </pre>
 * 
 * @author Iulian Rotaru
 * @since 1.0
 * @see ComponentDescriptor
 */
public final class ProjectConfig
{
  /** Project reference. */
  private Project project;

  /** XML DOM document. */
  private Document doc;

  /**
   * Project configured locales. If project is not locale sensitive, locales list has a single null record. Null record
   * denotes default locale, that is, files without locale variant. For locale sensitive project first record is still
   * null, still representing the default locale.
   * <p>
   * TODO: is not acurate: If <code>locale</code> element is missing from project configuration file this list is
   * initialized as for default locale - single null record.
   */
  private List<Locale> locales = new ArrayList<Locale>();

  /**
   * Default language is the first language from configured languages. Into {@link #languages} list default language is
   * marked with first null record. This default language value is that from configuration file and is needed to set
   * correct HTML <code>lang</code> attribute.
   * 
   * @todo update
   */
  private Locale defaultLocale;

  /**
   * Create project configuration instance. Searches for configuration file into given project directory. Throws
   * exception if project configuration file or language section is missing.
   * 
   * @param project parent project.
   * @throws WoodException if configuration file not found or language section is missing.
   */
  public ProjectConfig(Project project) throws WoodException
  {
    this.project = project;

    File configFile = new File(project.getProjectDir(), CT.PROJECT_CONFIG);
    try {
      DocumentBuilder builder = new DocumentBuilderImpl();
      this.doc = builder.loadXML(configFile);
    }
    catch(FileNotFoundException unused) {
      throw new WoodException("Missing project configuration file |%s|.", configFile);
    }

    Element localeElement = this.doc.getByTag("locale");
    if(localeElement == null) {
      throw new WoodException("Invalid project configuration file. Missing <locale> element.");
    }

    for(String languageTag : Strings.split(localeElement.getText(), ',', ' ')) {
      Locale locale = getLocale(languageTag);
      if(defaultLocale == null) {
        // first locale is always the default one
        defaultLocale = locale;
      }
      locales.add(locale);
    }
  }

  private static final Locale getLocale(String languageTag)
  {
    // TODO: study if to replace this logic with Locale.Builder.setLanguageTag(java.lang.String)
    // wood locale is a subset of BCP language tag

    // Locale.Builder builder = new Locale.Builder();
    // builder.setLanguageTag(languageTag);
    // return builder.build();

    Matcher matcher = Variants.LOCALE.matcher(languageTag);
    if(!matcher.find()) {
      throw new WoodException("Invalid language tag format |%s| into project config.", languageTag);
    }
    String country = matcher.group(2);
    if(country == null) {
      return new Locale(matcher.group(1));
    }
    return new Locale(matcher.group(1), country);
  }

  /**
   * Get project name or default value if <code>name</code> element is missing.
   * 
   * @param defaultValue default value.
   * @return project name or default value.
   */
  public String getName(String defaultValue)
  {
    return text("name", defaultValue);
  }

  /**
   * Get project display or default value if <code>display</code> element is missing.
   * 
   * @param defaultValue default value.
   * @return project display or default value.
   */
  public String getDisplay(String defaultValue)
  {
    return text("display", defaultValue);
  }

  /**
   * Get project description or default value if <code>description</code> element is missing.
   * 
   * @param defaultValue default value.
   * @return project description or default value.
   */
  public String getDescription(String defaultValue)
  {
    return text("description", defaultValue);
  }

  /**
   * Get project author or null if not set.
   * 
   * @return project author or null.
   */
  public String getAuthor()
  {
    return text("author", null);
  }

  /**
   * Get site build directory path, relative to project root. Return default value if <code>site-dir</code> element is
   * missing.
   * 
   * @param defaultValue default value.
   * @return site build directory path or default value.
   */
  public String getSiteDir(String defaultValue)
  {
    return text("site-dir", defaultValue);
  }

  /**
   * Get SDK specific service ID for requested named service. Returned ID have meaning in requested service scope, e.g.
   * for Google Analytics returned value is track ID whereas for Facebook is application ID.
   * <p>
   * Service name is under developer control; it is the name of SDK initialization script from library SDK directory.
   * This library sources layout convention is to use the same name for script file and the element from config file.
   * This very name is by definition the <em>service name</code>.
   * 
   * @return service specific ID.
   */
  public String getSDKID(String serviceName)
  {
    return text(serviceName, null);
  }

  /**
   * Get meta elements list declared into <code>meta</code> section. Returned elements list contains meta elements as
   * they are into configuration file. If <code>meta</code> section is missing returned elements list is empty.
   * 
   * @return meta elements list, possible empty.
   */
  public EList getMetas()
  {
    return doc.findByTag("meta");
  }

  /**
   * Get third party fonts declared into <code>font</code> section. Order from returned list is that from configuration
   * file. If <code>font</code> section is missing returned list is empty.
   * 
   * @return third party fonts list possible empty.
   */
  public List<String> getFonts()
  {
    List<String> fonts = new ArrayList<String>();
    for(Element fontEl : doc.findByXPath("//font")) {
      fonts.add(fontEl.getText());
    }
    return fonts;
  }

  /**
   * Return project configured locales. Returned list has at least one record, even if <code>locale</code> element is
   * missing from project configuration file. See {@link #locales} for details. Returned list is immutable.
   * 
   * @return project configured locales.
   * @see #locales
   */
  public List<Locale> getLocales()
  {
    if(locales.isEmpty()) {
      throw new IllegalStateException("Project locale not initialized.");
    }
    return Collections.unmodifiableList(locales);
  }

  /**
   * Get project configured default locale.
   * 
   * @return default locale.
   * @see #defaultLocale
   */
  public Locale getDefaultLocale()
  {
    return defaultLocale;
  }

  /**
   * Get the list of paths excluded from build process. Returned list is empty if <code>excludes</code> element is not
   * present.
   * 
   * @return excluded paths list, possible empty.
   */
  public List<Path> getExcludes()
  {
    Element el = doc.getByTag("excludes");
    if(el == null) {
      return Collections.emptyList();
    }
    List<Path> excludes = new ArrayList<Path>();
    for(String exclude : Strings.split(el.getText(), ',', ' ')) {
      excludes.add(Path.create(project, exclude.trim()));
    }
    return excludes;
  }

  /**
   * Return text value for element denoted by tag name or default value if element is missing.
   * 
   * @param tagName element tag name,
   * @param defaultValue default value to use when element is missing.
   * @return element text or default value.
   */
  private String text(String tagName, String defaultValue)
  {
    Element el = doc.getByTag(tagName);
    return el != null ? el.getText() : defaultValue;
  }
}
