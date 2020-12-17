package js.wood;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import js.util.Files;
import js.util.Params;

/**
 * Build file system is the directory structure for site files. Different build file systems are supported but a default
 * implementation exists, see {@link DefaultBuildFS}.
 * <p>
 * BuildFS is the target of the building process. It supplies specialized write methods whereas actual directory and
 * file names are solved by subclasses, concrete implementations. All write methods take care to avoid multiple
 * processing of the same file. Also append build number to target file name, if {@link #buildNumber} was set via
 * {@link #setBuildNumber(int)}.
 * <p>
 * If project is multi-locale, BuildFS is locale sensitive. There is optional {@link #setLocale(Locale)} that is used
 * for multi-locale build to store current processing locale. Both {@link #createAbsoluteUrlPath(String)} and
 * {@link #createDirectory(String)} insert locale language tag, of course for multi-locale build. Locale language tag is
 * BCP encoded: language is always lower case and country, if present, upper case separated by hyphen.
 * <p>
 * Build file system implementations are not thread safe. Do not use BuildFS instances into a concurrent context.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public abstract class BuildFS
{
  /** Project reference. */
  private final Project project;

  /** Build number or 0 if not set. Specialized write methods take care to append build number, of course if set. */
  private int buildNumber;

  /**
   * Current processing locale for multi-locale build. Locale language tag is inserted into directory paths and context
   * absolute URLs. Locale language tag is formatted BCP, the format supported by HTML <code>lang</code> attribute.
   */
  private Locale locale;

  /** Processed files cache to avoid multiple processing of the same file. */
  private List<File> processedFiles = new ArrayList<File>();

  /**
   * Create build file system instance for given project.
   * 
   * @param project project reference.
   */
  public BuildFS(Project project)
  {
    this.project = project;
  }

  /**
   * Set optional build number.
   * 
   * @param buildNumber build number.
   * @throws IllegalArgumentException if build number is not positive.
   * @see #buildNumber
   */
  public void setBuildNumber(int buildNumber)
  {
    Params.positive(buildNumber, "Build number");
    this.buildNumber = buildNumber;
  }

  /**
   * Set current processing locale for multi-locale build.
   * 
   * @param locale current processing locale.
   * @throws IllegalArgumentException if <code>locale</code> argument is null.
   * @see #locale
   */
  public void setLocale(Locale locale)
  {
    Params.notNull(locale, "Locale");
    this.locale = locale;
  }

  /**
   * Serialize page document to pages directory. Target file name is derived from page component name argument; uses
   * {@link #formatPageName(String)} to format it. Stores target file into {@link #processedFiles} in order to avoid
   * multiple processing. Also takes care to append {@link #buildNumber}, if set.
   * 
   * @param compo page component,
   * @param page page document.
   * @throws IOException if write fails.
   */
  public void writePage(Component compo, PageDocument page) throws IOException
  {
    File targetFile = new File(getPageDir(compo), insertBuildNumber(formatPageName(compo.getLayoutFileName())));
    if(!processedFiles.contains(targetFile)) {
      page.getDocument().serialize(new FileWriter(targetFile), true);
      processedFiles.add(targetFile);
    }
  }

  /**
   * Write favicon to media directory. Target file name is the source favicon file name. Stores target file into
   * {@link #processedFiles} in order to avoid multiple processing.
   * <p>
   * Returns favicon URL path, relative to site page location. Returned URL path is ready to be inserted into page
   * document.
   * 
   * @param favicon favicon file.
   * @return URL path of favicon file.
   * @throws IOException if favicon file write fails.
   */
  public String writeFavicon(Component page, FilePath favicon) throws IOException
  {
    File targetFile = new File(getMediaDir(), favicon.getName());
    if(favicon.exists() && !processedFiles.contains(targetFile)) {
      Files.copy(favicon.toFile(), targetFile);
      processedFiles.add(targetFile);
    }
    return Files.getRelativePath(getPageDir(page), targetFile, true);
  }

  // ------------------------------------------------------
  // Media files

  /**
   * Write media file referenced from site page. Target file name is the media file name formated by
   * {@link #formatMediaName(FilePath)}. Stores target file into {@link #processedFiles} in order to avoid multiple
   * processing. Also takes care to append {@link #buildNumber}, if set.
   * <p>
   * Returns media file URL path, relative to site page location. Returned URL path is ready to be inserted into page
   * document.
   * 
   * @param mediaFile media file.
   * @return media file URL path.
   * @throws IOException if media file write fails.
   */
  public String writePageMedia(Component page, FilePath mediaFile) throws IOException
  {
    File targetFile = new File(getMediaDir(), insertBuildNumber(formatMediaName(mediaFile)));
    if(!processedFiles.contains(targetFile)) {
      Files.copy(mediaFile.toFile(), targetFile);
      processedFiles.add(targetFile);
    }
    return Files.getRelativePath(getPageDir(page), targetFile, true);
  }

  /**
   * Write media file referenced from style. Target file name is the media file name formated by
   * {@link #formatMediaName(FilePath)}. Stores target file into {@link #processedFiles} in order to avoid multiple
   * processing. Also takes care to append {@link #buildNumber}, if set.
   * <p>
   * Returns media file URL path, relative to style file location. Returned URL path is ready to be inserted into page
   * document.
   * 
   * @param mediaFile media file.
   * @return media file URL path.
   * @throws IOException if media file write fails.
   */
  public String writeStyleMedia(FilePath mediaFile) throws IOException
  {
    File targetFile = new File(getMediaDir(), insertBuildNumber(formatMediaName(mediaFile)));
    if(!processedFiles.contains(targetFile)) {
      Files.copy(mediaFile.toFile(), targetFile);
      processedFiles.add(targetFile);
    }
    return Files.getRelativePath(getStyleDir(), targetFile, true);
  }

  /**
   * Write media file referenced from script. Target file name is the media file name formated by
   * {@link #formatMediaName(FilePath)}. Stores target file into {@link #processedFiles} in order to avoid multiple
   * processing. Also takes care to append {@link #buildNumber}, if set.
   * <p>
   * Returns context absolute URL path for written media file, see {@link #createAbsoluteUrlPath(String)}. Returned URL
   * path is ready to be inserted into page document.
   * 
   * @param mediaFile media file.
   * @return media file URL path.
   * @throws IOException if media file write fails.
   */
  public String writeScriptMedia(FilePath mediaFile) throws IOException
  {
    String mediaName = insertBuildNumber(formatMediaName(mediaFile));
    File targetFile = new File(getMediaDir(), mediaName);
    if(!processedFiles.contains(targetFile)) {
      Files.copy(mediaFile.toFile(), targetFile);
      processedFiles.add(targetFile);
    }
    return createAbsoluteUrlPath(mediaName);
  }

  /**
   * Create context absolute URL path for the given file. Context absolute path starts with path separator but does not
   * include protocol, host and port. Anyway, it includes context name, that is, application name. It is an absolute
   * path relative to server document root.
   * 
   * @param fileName file name.
   * @return context absolute URL path.
   */
  private String createAbsoluteUrlPath(String fileName)
  {
    StringBuilder builder = new StringBuilder();
    builder.append(Path.SEPARATOR);
    builder.append(project.getName());
    builder.append(Path.SEPARATOR);
    if(project.isMultiLocale()) {
      builder.append(locale.toLanguageTag());
      builder.append(Path.SEPARATOR);
    }
    builder.append(getMediaDir().getName());
    builder.append(Path.SEPARATOR);
    builder.append(fileName);
    return builder.toString();
  }

  // ------------------------------------------------------
  // Style files

  /**
   * Write style file using external references handler. References handler is used for resources processing. Returns
   * URL path of the written style file, relative to page location, ready to be inserted into page document.
   * <p>
   * Stores target file into {@link #processedFiles} in order to avoid multiple processing. Also takes care to append
   * {@link #buildNumber}, if set.
   * 
   * @param styleFile style file,
   * @param referenceHandler resource references handler.
   * @return URL path relative to page location.
   * @throws IOException if write operation fails.
   */
  public String writeStyle(Component page, FilePath styleFile, ReferenceHandler referenceHandler) throws IOException
  {
    String fileName = insertBuildNumber(formatStyleName(styleFile));
    File targetFile = new File(getStyleDir(), fileName);
    if(!processedFiles.contains(targetFile)) {
      Files.copy(new SourceReader(new StyleReader(styleFile), styleFile, referenceHandler), new FileWriter(targetFile));
      processedFiles.add(targetFile);
    }
    return Files.getRelativePath(getPageDir(page), targetFile, true);
  }

  // ------------------------------------------------------
  // Script files

  /**
   * Write script file using external references handler. References handler is used for resources processing. Returns
   * URL path of the written script file, relative to page location, ready to be inserted into page document. Stores
   * target file into {@link #processedFiles} in order to avoid multiple processing. Also takes care to append
   * {@link #buildNumber}, if set.
   * 
   * @param scriptFile script file,
   * @param referenceHandler resource references handler.
   * @return URL path relative to page location.
   * @throws IOException if write operation fails.
   */
  public String writeScript(Component page, FilePath scriptFile, ReferenceHandler referenceHandler) throws IOException
  {
    File targetFile = new File(getScriptDir(), insertBuildNumber(formatScriptName(scriptFile)));
    targetFile.getParentFile().mkdirs();
    if(!processedFiles.contains(targetFile)) {
      Files.copy(new SourceReader(scriptFile, referenceHandler), new FileWriter(targetFile));
      processedFiles.add(targetFile);
    }
    return Files.getRelativePath(getPageDir(page), targetFile, true);
  }

  // ------------------------------------------------------
  // Helper methods

  /**
   * Insert build number into file name. {@link #buildNumber Build number} should be already set before invoking this
   * method. If {@link #buildNumber} is not set, that is, its value is 0 this method returns original file name.
   * <p>
   * Build number is added to file base name separated by dash. Number is padded with 0 to three digits. Extension is
   * preserved.
   * 
   * @param fileName file name.
   * @return numbered file name.
   * @throws WoodException if file does not have extension.
   */
  private String insertBuildNumber(String fileName)
  {
    if(buildNumber == 0) {
      return fileName;
    }
    int extensionSeparatorIndex = fileName.lastIndexOf('.');
    if(extensionSeparatorIndex == -1) {
      throw new WoodException("Invalid file name |%s|. Missing extension.", fileName);
    }
    String baseName = fileName.substring(0, extensionSeparatorIndex);
    String extension = fileName.substring(extensionSeparatorIndex + 1);
    return String.format("%s-%03d.%s", baseName, buildNumber, extension);
  }

  // ------------------------------------------------------
  // Interface to be implemented by concrete build file system.

  /**
   * Get the directory that stores site pages.
   * 
   * @return pages directory.
   */
  protected abstract File getPageDir(Component component);

  /**
   * Get the directory that stores site styles.
   * 
   * @return styles directory.
   */
  protected abstract File getStyleDir();

  /**
   * Get the directory that stores site scripts.
   * 
   * @return scripts directory.
   */
  protected abstract File getScriptDir();

  /**
   * Get the directory that stores site media files, all types in the same place.
   * 
   * @return media files directory.
   */
  protected abstract File getMediaDir();

  /**
   * Format the page file name. Implementation is free to pre-process page name in every way, including returning
   * original name unchanged.
   * 
   * @param pageName page name.
   * @return formatted page name.
   */
  protected abstract String formatPageName(String pageName);

  /**
   * Format the style file name. Implementation is free to pre-process file name in every way, including returning
   * original name unchanged.
   * 
   * @param styleFile style file.
   * @return formatted style name.
   */
  protected abstract String formatStyleName(FilePath styleFile);

  /**
   * Format the script file name. Implementation is free to pre-process file name in every way, including returning
   * original name unchanged.
   * 
   * @param scriptFile script file.
   * @return formatted style name.
   */
  protected abstract String formatScriptName(FilePath scriptFile);

  /**
   * Format the media file name. Implementation is free to pre-process file name in every way, including returning
   * original name unchanged.
   * 
   * @param mediaFile media file.
   * @return formatted style name.
   */
  protected abstract String formatMediaName(FilePath mediaFile);

  // ------------------------------------------------------
  // Helper methods common to all build file system implementations

  /**
   * Create named directory into site build. This factory method takes into account current processing locale, see
   * {@link #locale}. If project is multi-locale, this method prefixes directory name with locale language tag; if
   * project is single locale, locale sub-directory is not created.
   * <p>
   * Value returned by {@link Locale#toLanguageTag()} is encoded into BCP as requested by HTML <code>lang</code>
   * attribute. Both locale directory name and <code>lang</code> attribute has the same format; hence locale encoded
   * into request path has also the same format.
   * <p>
   * Create directory, if does not already exist.
   * 
   * @param dirName name of directory to create.
   * @return created directory.
   */
  protected File createDirectory(String dirName)
  {
    File dir = project.getSiteDir();

    // if project is multi-locale create a sub-directory with name equal with locale language tag
    if(project.isMultiLocale()) {
      dir = new File(dir, locale.toLanguageTag());
    }

    if(!CT.CURRENT_DIR.equals(dirName)) {
      dir = new File(dir, dirName);
    }
    if(!dir.exists()) {
      dir.mkdirs();
    }
    return dir;
  }
}
