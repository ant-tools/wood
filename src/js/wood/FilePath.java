package js.wood;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.util.Files;
import js.util.Strings;

/**
 * File path identifies component files, both source files and resources. Recognized source files are layout (HTM),
 * style (CSS) and script (JS); they can contain references to resources that are variables (XML) and media files. File
 * paths scope is limited to source directories, see {@link Project} class description.
 * <p>
 * A file path is a standard Java file with couple syntax constrains: it always uses slash (/) for file separator,
 * always starts with a valid source directory and file name supports variants. Variants qualify file path so that one
 * can create group of files with the same semantic content but differently presented, e.g. string variables for
 * multi-language support.
 * <p>
 * Currently supported variants are language, viewport maximum width and design. Language is two letter code standard -
 * see ISO 639-1 whereas viewport width is a numeric value in pixels. Language code is used for multi-language build and
 * viewport width for styles depending on display size for responsive design. Design variant is used by component import
 * logic to match against target project design.
 * <p>
 * Characters used by path segments and file base name are US-ASCII alphanumeric characters, dash and dot. Dot is
 * allowed for file names with version, for example <code>js-lib-.1.2.3.js</code>. Underscore is reserved for variants
 * separator and is not valid in names.
 * 
 * <pre>
 * file-path    = source-dir SEP *path-segment base-name *variant DOT extension 
 * source-dir   = RES / LIB / SCRIPT / GEN
 * path-segment = 1*CH F-SEP
 * base-name    = 1*CH
 * extension    = 2*4(ALPHA / DIGIT)
 * ; for variant non terminal definition see {@link Variants}
 * 
 * ; terminal symbols definition
 * RES    = "res"                       ; UI resources
 * LIB    = "lib"                       ; third-party Java archives and UI components
 * SCRIPT = "script"                    ; scripts source directory
 * GEN    = "gen"                       ; generated scripts, mostly HTTP-RMI stubs
 * SEP  = "/"                           ; file separator is always slash
 * DOT    = "."                         ; dot as file extension separator
 * CH     = ALPHA / DIGIT / "-" / "."   ; character is US-ASCII alphanumeric, dash and dot
 * 
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class FilePath extends Path
{
  // res/template/progress-bar/progress-bar_ro_desktop.htm
  // ---------- path ----------|-- name --|-|variants|-|ext

  /** Pattern for file path accordingly syntax described by class description. */
  private static final Pattern PATTERN = Pattern.compile("^" + //
      "((?:res|lib|script|gen)/(?:[a-z0-9-]+/)*)" + // directory path
      "([a-z0-9-\\.]+)" + // base name is file name without variants or extension
      "(?:_([a-z0-9][a-z0-9-_]*))?" + // variants
      "\\.([a-z0-9]{2,3})" + // file extension
      "$", //
      Pattern.CASE_INSENSITIVE);

  /** This file parent directory. */
  private DirPath dirPath;

  /** File base name is the file name without variants and extension. Leading file separator is not included. */
  private String basename;

  /** File name including extension but no trailing file separator nor variants. */
  private String name;

  /** Optional variants, empty if file path has none. */
  private Variants variants;

  /** File type. */
  private FileType fileType;

  /**
   * Create immutable path instance from a given path value.
   * 
   * @param project project reference,
   * @param filePath path value.
   */
  public FilePath(Project project, String filePath)
  {
    super(project, filePath);

    Matcher matcher = PATTERN.matcher(filePath);
    if(!matcher.find()) {
      throw new WoodException("Invalid file path |%s|.", filePath);
    }

    dirPath = new DirPath(project, matcher.group(1));
    basename = matcher.group(2);
    String extension = matcher.group(4);
    name = Strings.concat(basename, '.', extension);
    variants = new Variants(this, matcher.group(3));

    fileType = FileType.forExtension(extension);
  }

  /**
   * Get file type.
   * 
   * @return file type.
   * @see #fileType
   */
  public FileType getType()
  {
    return fileType;
  }

  /**
   * Get this file name with extension but not variants.
   * 
   * @return file name.
   * @see #name
   */
  public String getName()
  {
    return name;
  }

  public boolean hasVariants()
  {
    return !variants.isEmpty();
  }

  /**
   * Get this file variants, possible empty.
   * 
   * @return this file variants.
   * @see #variants
   */
  public Variants getVariants()
  {
    return variants;
  }

  /**
   * Get this file parent directory.
   * 
   * @return parent directory.
   * @see #dirPath
   */
  public DirPath getDirPath()
  {
    return dirPath;
  }

  /**
   * Get base name, that is, the file name without extension and variants.
   * 
   * @return file base name.
   * @see #basename
   */
  public String getBaseName()
  {
    return basename;
  }

  /**
   * Test if reference name equals this file path base name. Base name is file name without variants and extension.
   * 
   * @param reference reference.
   * @return true if reference name equals this file base name.
   * @see #basename
   */
  public boolean isBaseName(Reference reference)
  {
    return isBaseName(reference.getName());
  }

  /**
   * Test if this file base name equals given name. Base name is file name without variants and extension.
   * 
   * @param name name to compare with.
   * @return true if this file base name equals given name.
   * @see #basename
   */
  public boolean isBaseName(String name)
  {
    return basename.equals(name);
  }

  /**
   * Test if this file is a component descriptor. A component descriptor is a XML file that has base name the same as
   * component name; by convention component has the same name as its directory.
   * 
   * @return true if this file is a component descriptor.
   */
  public boolean isCompoDescriptor()
  {
    return fileType == FileType.XML && basename.equals(dirPath.getName());
  }

  /**
   * Test if this file is resource variables definition. A variables definition file has XML extension but not the same
   * base name as parent directory. Note that descriptors and variables have both XML extension and differ only by base
   * name.
   * 
   * @return true if this file is resource variables.
   */
  public boolean isVariables()
  {
    return fileType == FileType.XML && !basename.equals(dirPath.getName());
  }

  /**
   * Test if this file is a component layout file.
   * 
   * @return true if this file is a layout file.
   */
  public boolean isLayout()
  {
    return fileType == FileType.LAYOUT;
  }

  /**
   * Test if this file designates a style sheet, be it component or theme styles.
   * 
   * @return true if this file is style sheet.
   */
  public boolean isStyle()
  {
    return fileType == FileType.STYLE;
  }

  /**
   * Test if this file is a script file.
   * 
   * @return true if this file is a script file.
   */
  public boolean isScript()
  {
    return fileType == FileType.SCRIPT;
  }

  /**
   * Test if this file is script preview file. Preview script is part of component sources used solely for preview; it
   * is not part of component distribution.
   * 
   * @return true if this file is script preview.
   */
  public boolean isPreviewScript()
  {
    return name.equalsIgnoreCase(CT.PREVIEW_SCRIPT);
  }

  public boolean isSDKScript()
  {
    return fileType == FileType.SCRIPT && dirPath.isSDK();
  }

  /**
   * Test if this file is a media file.
   * 
   * @return true if this file is media file.
   */
  public boolean isMedia()
  {
    return fileType == FileType.MEDIA;
  }

  /**
   * Clone this file path but forces the type to style. Returned file differs only by its type.
   * 
   * @return style file.
   */
  public FilePath cloneToStyle()
  {
    return project.getFile(Files.replaceExtension(value(), CT.STYLE_EXT));
  }

  /**
   * Get a reader usable for this file content. Returns a new created reader instance; it is caller responsibility to
   * close it.
   * 
   * @return newly created reader.
   * @throws WoodException if this file does not exist on file system.
   */
  public Reader getReader() throws WoodException
  {
    try {
      return new FileReader(toFile());
    }
    catch(FileNotFoundException e) {
      throw new WoodException(e);
    }
  }

  public FilePath getVariant(String variant)
  {
    int i = value.lastIndexOf('.');
    if(i == -1) {
      i = value.length();
    }
    return new FilePath(project, Strings.concat(value.substring(0, i), '_', variant, value.substring(i)));
  }

  /**
   * Test if path value is acceptable for file path instance creation.
   * 
   * @param path path value.
   * @return true if path value match file pattern.
   */
  public static boolean accept(String path)
  {
    Matcher matcher = PATTERN.matcher(path);
    return matcher.find();
  }
}
