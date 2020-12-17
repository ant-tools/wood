package js.wood;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.util.Files;
import js.util.Strings;

/**
 * Directory path relative to project root. Directory path is always relative to project root and provides methods to
 * retrieve and iterate child files.
 * <p>
 * A directory path has a source directory and optional path segments. File separator is always slash (/). If trailing
 * file separator is missing takes care to add it. Since directory path is relative to project root leading file
 * separator is not accepted. Characters for name and path segment are those used by this tool: US-ASCII alphanumeric
 * and dash (-). Note that underscore (_) is not supported since is used for variants separator.
 * 
 * <pre>
 * dir-path     = source-dir *path-segment [SEP] 
 * source-dir   = RES / LIB / SCRIPT / GEN
 * path-segment = SEP 1*CH
 * 
 * ; terminal symbols definition
 * RES    = "res"               ; UI resources
 * LIB    = "lib"               ; third-party Java archives and UI components
 * SCRIPT = "script"            ; scripts source directory
 * GEN    = "gen"               ; generated scripts, mostly HTTP-RMI stubs
 * SEP    = "/"                 ; file separator
 * CH     = ALPHA / DIGIT / "-" ; character is US-ASCII alphanumeric and dash
 * 
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class DirPath extends Path
{
  /** Pattern for directory path. */
  private static final Pattern PATTERN = Pattern.compile("^(res|lib|script|gen)((?:/[a-z0-9-]+)*)/?$", Pattern.CASE_INSENSITIVE);

  /** Source directory. */
  private String sourceDir;

  /** Directory path segments does not include source directory. */
  private ArrayList<String> pathSegments;

  /**
   * Create an immutable directory path instance. It is not legal to use absolute paths, that is, path to start with
   * file separator. See {@link DirPath} class description for directory path syntax.
   * 
   * @param project project reference,
   * @param dirPath directory path, relative to project root.
   * @throws WoodException if <code>path</code> parameter is absolute or uses not valid characters.
   */
  @SuppressWarnings("unchecked")
  public DirPath(Project project, String dirPath) throws WoodException
  {
    super(project, dir(dirPath));
    Matcher matcher = PATTERN.matcher(dirPath);
    if(!matcher.find()) {
      throw new WoodException("Directory path parameter |%s| is invalid.", dirPath);
    }
    sourceDir = matcher.group(1);
    pathSegments = new ArrayList<String>(matcher.group(2) != null ? Strings.split(matcher.group(2), Path.SEPARATOR) : Collections.EMPTY_LIST);
  }

  /**
   * Add trailing file separator, if missing.
   * 
   * @param dirPath directory path.
   * @return directory path with trailing file separator.
   */
  private static String dir(String dirPath)
  {
    return dirPath.endsWith(Path.SEPARATOR) ? dirPath : dirPath + Path.SEPARATOR;
  }

  /**
   * Get this directory path segments, that do not include source directory. Returned list is safe to modify.
   * 
   * @return directory path segments.
   * @see #pathSegments
   */
  @SuppressWarnings("unchecked")
  public List<String> getPathSegments()
  {
    // returns a clone because list can be changed by caller logic
    return (List<String>)pathSegments.clone();
  }

  /**
   * Get directory name. Returned value has not leading or trailing file separator.
   * 
   * @return this directory name.
   */
  public String getName()
  {
    return file.getName();
  }

  /**
   * Get the path of a child file with a given name.
   * 
   * @param fileName the name of child file.
   * @return path of child file.
   */
  public FilePath getFilePath(String fileName)
  {
    return project.getFile(value + fileName);
  }

  /**
   * Get the path of named child directory.
   * 
   * @param path sub-directory path.
   * @return path of child directory.
   */
  public DirPath getSubdirPath(String path)
  {
    return new DirPath(project, value + path);
  }

  /**
   * Get iterable instance for this directory files.
   * <p>
   * This method is designed to work with for-each Java loop as in snippet below.
   * 
   * <pre>
   * for(FilePath file : dir.files()) {
   *   // handle file path
   * }
   * </pre>
   * 
   * @return file iteratble instance.
   */
  public Iterable<FilePath> files()
  {
    if(!exists()) {
      return Collections.emptyList();
    }
    return new FilesIterable(toFile());
  }

  /**
   * Iterate over all this directory files and sub-directories. Please note that iteration order is not guaranteed.
   * <p>
   * Files handler instance does not need to override all handler methods as in sample below. In fact is common case to
   * override only {@link FilesHandler#onFile(FilePath)}.
   * 
   * <pre>
   * dir.files(new FilesHandler()
   * {
   *   &#064;Override
   *   public void onDirectory(DirPath dir) throws Exception
   *   {
   *     // recursive scanning of sub-directories
   *   }
   * 
   *   &#064;Override
   *   public boolean accept(FilePath file)
   *   {
   *     // test file attributes and decide if acceptable
   *   }
   * 
   *   &#064;Override
   *   public void onFile(FilePath file) throws Exception
   *   {
   *     // handle file
   *   }
   * });
   * </pre>
   * 
   * @param handler files handler.
   */
  public void files(FilesHandler handler)
  {
    files(null, handler);
  }

  /**
   * Iterate over sub-directories and files of specified type. This method behaves the same as
   * {@link #files(FilesHandler)} but list only files of specified type. If {@link FilesHandler#accept(FilePath)} is
   * implemented its results uses AND logic with file type filtering. Note that iteration order is not guaranteed.
   * <p>
   * Files handler anonymous instance does not need to override all handler methods as in sample below. For child files
   * processing only, one can override just {@link FilesHandler#onFile(FilePath)}.
   * 
   * <pre>
   * dir.files(FileType.LAYOUT, new FilesHandler()
   * {
   *   &#064;Override
   *   public void onDirectory(DirPath dir) throws Exception
   *   {
   *     // recursive scanning of sub-directories
   *   }
   * 
   *   &#064;Override
   *   public boolean accept(FilePath file)
   *   {
   *     // test file attributes and decide if acceptable
   *   }
   * 
   *   &#064;Override
   *   public void onFile(FilePath file) throws Exception
   *   {
   *     // handle file
   *   }
   * });
   * </pre>
   * 
   * @param fileType files type filter,
   * @param handler files handler.
   */
  public void files(FileType fileType, FilesHandler handler)
  {
    if(!exists()) {
      return;
    }

    try {
      for(File file : toFile().listFiles()) {
        if(file.isDirectory()) {
          // takes care to create DirPath relative to project
          handler.onDirectory(new DirPath(project, Files.getRelativePath(project.getProjectDir(), file, true)));
          continue;
        }
        FilePath filePath = project.getFile(Files.getRelativePath(project.getProjectDir(), file, true));
        if(!handler.accept(filePath)) {
          continue;
        }
        if(fileType == null || fileType.equals(file)) {
          handler.onFile(filePath);
        }
      }
    }
    catch(Exception e) {
      throw (e instanceof WoodException) ? (WoodException)e : new WoodException(e);
    }
  }

  /**
   * Test if this directory is the project UI resources directory. UI resources directory is the code base for
   * application components.
   * 
   * @return true if this directory is project UI resources.
   */
  public boolean isResources()
  {
    return CT.RESOURCE_DIR.equals(sourceDir);
  }

  /**
   * Test if this directory is the project assets directory. Project assets are global variables and media files.
   * 
   * @return true if this directory is project assets.
   */
  public boolean isAssets()
  {
    return CT.ASSETS_DIR.endsWith(getName());
  }

  /**
   * Test if this directory is UI resources theme directory. Theme directory stores style files for UI primitive
   * elements and theme variables.
   * 
   * @return true if this directory is theme directory.
   */
  public boolean isTheme()
  {
    return value.startsWith(CT.THEME_DIR);
  }

  /**
   * Test if path directory denotes project library. Library directory holds third party components and script
   * libraries.
   * 
   * @return true if this directory is project library.
   */
  public boolean isLibrary()
  {
    return CT.LIBRARY_DIR.equals(sourceDir);
  }

  public boolean isSDK()
  {
    return value.startsWith(CT.SDK_DIR);
  }

  /**
   * Test if this path designates project generated files directory. Generated files directory contains mostly scripts
   * for HTTP-RMI stubs.
   * 
   * @return true if directory is project generated.
   */
  public boolean isGenerated()
  {
    return CT.GENERATED_DIR.equalsIgnoreCase(sourceDir);
  }

  /**
   * Directory string representation.
   * 
   * @return directory string representation.
   */
  @Override
  public String toString()
  {
    return value;
  }

  /**
   * Test if path value is acceptable for directory path instance creation.
   * 
   * @param path path value.
   * @return true if path value match directory pattern.
   */
  public static boolean accept(String path)
  {
    Matcher matcher = PATTERN.matcher(path);
    return matcher.find();
  }

  // ------------------------------------------------------
  // Internal utility classes.

  /**
   * Iterable over files from a directory.
   * 
   * @author Iulian Rotaru
   * @since 1.0
   */
  private class FilesIterable implements Iterable<FilePath>
  {
    /** Files iterator. */
    private Iterator<FilePath> iterator;

    /**
     * Construct file iterable instance for files from requested directory.
     * 
     * @param dir directory to iterate.
     */
    public FilesIterable(File dir)
    {
      iterator = new FilesIterator(dir.listFiles());
    }

    /**
     * Retrieve files iterator instance.
     * 
     * @return files iterator instance.
     */
    @Override
    public Iterator<FilePath> iterator()
    {
      return iterator;
    }
  }

  /**
   * Iterator over array of files in natural, incremental order. Please note that this iterator is flat, that is, it
   * does not enter sub-directories. In fact directories are ignored.
   * <p>
   * Be aware that this implementation mandates standard iterator pattern, see sample code. Trying to call
   * {@link #next()} without {@link #hasNext()} will render not predictable behavior.
   * 
   * <pre>
   * FileIterator it = new FilesIterator(dir.listFiles());
   * while(it.hasNext()) {
   *   it.next();
   * }
   * </pre>
   * 
   * @author Iulian Rotaru
   * @since 1.0
   */
  private class FilesIterator implements Iterator<FilePath>
  {
    /** Directory files. */
    private File[] files;

    /** Current processing index updated by {@link #hasNext()}. */
    private int index;

    /**
     * Construct iterator instance for given files list, preparing {@link #index} for {@link #hasNext()} invocation.
     * 
     * @param files files list.
     */
    public FilesIterator(File[] files)
    {
      this.files = files;
      this.index = -1;
    }

    /**
     * Test if there are more files to iterate and update {@link #index} for current file handling via {@link #next()}.
     * 
     * @return true if there are more files to iterate.
     */
    @Override
    public boolean hasNext()
    {
      if(files == null) {
        return false;
      }
      for(++index; index < files.length; ++index) {
        if(files[index].isFile()) {
          return true;
        }
      }
      return false;
    }

    /**
     * Uses {@link #index} to retrieve current file and returns it wrapped by {@link FilePath} instance.
     * <p>
     * Implementation note: this method uses {@link #index} to retrieve current file but does not increment it. This
     * method relies on {@link #hasNext()} for index increment.
     * 
     * @return current file.
     */
    @Override
    public FilePath next()
    {
      return project.getFile(Files.getRelativePath(project.getProjectDir(), files[index], true));
    }

    /**
     * Remove operation is not supported.
     * 
     * @throws UnsupportedOperationException remove not supported.
     */
    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
