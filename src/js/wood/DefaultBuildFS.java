package js.wood;

import java.io.File;
import java.util.List;
import java.util.Locale;

import js.util.Strings;

/**
 * Default build file system implementation. This build file system uses separated directories for styles, scripts and
 * media files; all media files are stored in the same place. Pages are placed in build directory root. For multi-locale
 * projects, replicates single locale directories layout for every locale.
 * <p>
 * For multi-locale build uses locale language tag for locale directory name, see {@link Locale#toLanguageTag()}. Locale
 * language tag is BCP encoded: language encoded ISO 639 alpha-2 and is always lower case and country, if present, is
 * encoded ISO 3166 alpha-2 and is upper case, separated by hyphen.
 * <p>
 * Here is a build directory layout for both single and multi locale projects.
 * 
 * <pre>
 *  /                              /                  
 *  /media/                        /en-US/
 *  /script/                       |  /media/
 *  /style/                        |  /script/
 *  +-page.htm                     |  /style/    
 *                                 |  +-page.htm  
 *                                 |
 *                                 /ro/
 *                                 |  /media/
 *                                 |  /script/
 *                                 |  /style/
 *                                 |  +-page.htm
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class DefaultBuildFS extends BuildFS
{
  /**
   * Create default build file system instance for given project.
   * 
   * @param project project reference.
   */
  public DefaultBuildFS(Project project)
  {
    super(project);
  }

  /**
   * Usually page layout files are stored on build root. Anyway, if component descriptor define <code>path</code>
   * element uses that directories path instead. Takes care to create directories path. For <code>path</code>
   * description see {@link ComponentDescriptor}.
   */
  @Override
  protected File getPageDir(Component page)
  {
    String path = page != null ? page.getDescriptor().getPath(null) : null;
    return path != null ? createDirectory(path) : createDirectory(CT.CURRENT_DIR);
  }

  /**
   * Style files are stored in a single directory named <code>style</code>, relative to build root.
   */
  @Override
  protected File getStyleDir()
  {
    return createDirectory("style");
  }

  /**
   * Script files are stored in a single directory named <code>script</code>, relative to build root.
   */
  @Override
  protected File getScriptDir()
  {
    return createDirectory("script");
  }

  /**
   * All media files are stored in a single directory named <code>media</code>, relative to build root.
   */
  @Override
  protected File getMediaDir()
  {
    return createDirectory("media");
  }

  /**
   * Returns original page name, unchanged.
   * 
   * @param pageName page name.
   * @return page name.
   */
  @Override
  protected String formatPageName(String pageName)
  {
    return pageName;
  }

  /**
   * Return qualified style file name separated by dash. Resources directory is not included. Also do not include last
   * directory if has the same name as style file.
   * <p>
   * For your convenience here are couple examples.
   * <table border="1" style="border-collapse:collapse;">
   * <tr>
   * <td><b>Original Style Name
   * <td><b>Formatted Style Name
   * <tr>
   * <td>res/page/index/index.css
   * <td>page-index.css
   * <tr>
   * <td>res/theme/style.css
   * <td>theme-style.css
   * <tr>
   * <td>lib/video-player/style.css
   * <td>lib-video-player-style.css
   * </table>
   * 
   * @param styleFile style file.
   * @return formatted style file name.
   */
  @Override
  protected String formatStyleName(FilePath styleFile)
  {
    DirPath dir = styleFile.getDirPath();
    List<String> segments = dir.getPathSegments();
    if(dir.isLibrary()) {
      segments.add(0, CT.LIBRARY_DIR);
    }
    if(segments.isEmpty() || !segments.get(segments.size() - 1).equals(styleFile.getBaseName())) {
      segments.add(styleFile.getBaseName());
    }
    return Strings.join(segments, '-') + CT.DOT_STYLE_EXT;
  }

  /**
   * Return qualified file name separated by dot. Source directory is not included, except for generated scripts. For
   * library scripts do not include last directory if has the same name as script file.
   * <p>
   * For your convenience here are couple examples.
   * <table border="1" style="border-collapse:collapse;">
   * <tr>
   * <td><b>Original Script Name
   * <td><b>Formatted Script Name
   * <tr>
   * <td>script/hc/page/Index.js
   * <td>hc.page.Index.js
   * <tr>
   * <td>gen/js/wood/Controller.js
   * <td>gen.js.wood.Controller.js
   * <tr>
   * <td>lib/video-player/media.js
   * <td>video-player.media.js
   * <tr>
   * <td>lib/paging.js
   * <td>paging.js
   * <tr>
   * <td>lib/js-lib/js-lib.js
   * <td>js-lib.js
   * </table>
   * 
   * @param scriptFile script file.
   * @return formatted script file name.
   */
  @Override
  protected String formatScriptName(FilePath scriptFile)
  {
    DirPath dir = scriptFile.getDirPath();

    if(dir.isLibrary()) {
      List<String> segments = dir.getPathSegments();
      if(segments.isEmpty()) {
        return scriptFile.getName();
      }
      if(dir.getName().equals(segments.get(segments.size() - 1))) {
        segments.remove(segments.size() - 1);
      }
      segments.add(scriptFile.getName());
      return Strings.join(segments, '.');
    }

    List<String> segments = dir.getPathSegments();
    if(dir.isGenerated()) {
      segments.add(0, "gen");
    }
    segments.add(scriptFile.getName());
    return Strings.join(segments, '.');
  }

  /**
   * Return qualified media file guaranteed to be unique in order to avoid name collision. Returned file name contains
   * both path directories and file name separated by underscore (_). Directory names are separated by dash (-). Source
   * directory is not included, except for project library.
   * <p>
   * For your convenience here are couple examples.
   * <table border="1" style="border-collapse:collapse;">
   * <tr>
   * <td><b>Original Media Name
   * <td><b>Formatted Media Name
   * <tr>
   * <td>res/page/index/background.png
   * <td>page-index_background.png
   * <tr>
   * <td>res/theme/background.png
   * <td>theme_background.png
   * <tr>
   * <td>res/asset/background.png
   * <td>asset_background.png
   * <tr>
   * <td>script/js/wood/player/background.png
   * <td>js-wood-player_background.png
   * <tr>
   * <td>lib/paging/next-page.png
   * <td>lib-paging_next-page.png
   * </table>
   * 
   * @param mediaFile media file.
   * @return formatted media file name.
   */
  @Override
  protected String formatMediaName(FilePath mediaFile)
  {
    DirPath dir = mediaFile.getDirPath();
    List<String> segments = dir.getPathSegments();
    if(dir.isLibrary()) {
      segments.add(0, CT.LIBRARY_DIR);
    }
    // uses underscore to separated directories path from file name in order to avoid name collision on sub-directories:
    // template/page/icon-logo.png -> template-page_icon-logo.png
    // template/page/icon/logo.png -> template-page-icon_logo.png
    return Strings.concat(Strings.join(segments, '-'), '_', mediaFile.getName());
  }
}
