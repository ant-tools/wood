package js.wood;


/**
 * Globals constants.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public final class CT
{
  // ------------------------------------------------------
  // Global constants

  /** Project configuration file. */
  public static final String PROJECT_CONFIG = "project.xml";

  /** Favicon file name stored into project assets directory. */
  public static final String FAVICON = "favicon.ico";

  /** End of file mark. */
  public static final int EOF = -1;

  /** System dependent line separator. */
  public static final String LN = System.getProperty("line.separator");

  /** Name convention for current directory. */
  public static final String CURRENT_DIR = ".";

  /** Seed for hash code computation. */
  public static final int PRIME = 31;

  // ------------------------------------------------------
  // Files types recognized by this build tool

  /** Layout file describe UI elements. */
  public static final String LAYOUT_EXT = "htm";
  /** The same as {@link #LAYOUT_EXT} but with leading dot. */
  public static final String DOT_LAYOUT_EXT = ".htm";

  /** Style file for layout structure and theme styles. */
  public static final String STYLE_EXT = "css";
  /** The same as {@link #STYLE_EXT} but with leading dot. */
  public static final String DOT_STYLE_EXT = ".css";

  /** Script for component behavior and application logic. */
  public static final String SCRIPT_EXT = "js";
  /** The same as {@link #SCRIPT_EXT} but with leading dot. */
  public static final String DOT_SCRIPT_EXT = ".js";

  /** XML files are used to define variables and descriptors. */
  public static final String XML_EXT = "xml";
  /** Same as {@link #XML_EXT} but with leading dot. */
  public static final String DOT_XML_EXT = ".xml";

  // ------------------------------------------------------
  // Project directories relative to project root

  /** UI resources stores application components. */
  public static final String RESOURCE_DIR = "res";

  /** Project library for third party components and script libraries. */
  public static final String LIBRARY_DIR = "lib";

  /** Script code base. */
  public static final String SCRIPT_DIR = "script";

  /** Generated files, mainly scripts for HTTP-RMI stubs. */
  public static final String GENERATED_DIR = "gen";

  /** Default site build directory used when project configuration file does not include it. */
  public static final String DEF_SITE_DIR = "build/site";

  /** Project assets directory stores global variables and media files. */
  public static final String ASSETS_DIR = RESOURCE_DIR + "/asset";

  /** Site theme directory for styles related to UI primitive elements design and theme variables. */
  public static final String THEME_DIR = RESOURCE_DIR + "/theme";

  // ------------------------------------------------------
  // Dynamic generated files

  /** File name for styles reset. */
  public static final String RESET_CSS = "reset.css";

  /** File name for key frames used for CSS animations. */
  public static final String FX_CSS = "fx.css";

  // ------------------------------------------------------
  // Preview files

  /** The name of style file used for preview. */
  public static final String PREVIEW_STYLE = "preview.css";

  /** The name of script file used for preview. */
  public static final String PREVIEW_SCRIPT = "preview.js";

  public static final String SDK_DIR = "lib/sdk";

  /** Prevent default constructor synthesis. */
  private CT()
  {
  }
}
