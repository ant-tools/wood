package js.wood;

import java.io.File;

/**
 * Path designates project entities like component directories, source and resource files. It is immutable and is always
 * relative to project root directory. Anyway, the Java {@link File} returned by {@link #toFile()} does contain project
 * root.
 * <p>
 * Paths are designed to address entities from {@link Project}. This tool library project can be part of a larger
 * project, perhaps an Eclipse one. Although paths are relative to project root they can access files only in source
 * directories. Master project files, if any, are not in this path class scope. See {@link Project} class description
 * for recognized source directories.
 * <p>
 * Path syntax depends on concrete implementation but resemble Java file: it has a sequence of parts, separated by slash
 * (/). Note that Path always uses slash (/) for separator, no matter JVM platform. Also names from path uses US-ASCII
 * alphanumeric characters and dash (-). Note that underscore (_) is not allowed since it is used as variants separator.
 * 
 * <pre>
 * path = part *(SEP part) 
 * part = 1*CH
 * 
 * ; terminal symbols definition
 * SEP = "/"                 ; file separator
 * CH  = ALPHA / DIGIT / "-" ; character is US-ASCII alphanumeric and dash
 * 
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public abstract class Path
{
  /** Path separator is always slash. Note that this path separator is also used by URL path related logic. */
  public static final String SEPARATOR = "/";

  /** Parent project reference. All paths are always relative to this parent project.*/
  protected Project project;

  /** Path value relative to project root. */
  protected String value;

  /** Wrapped Java file include project root. */
  protected File file;

  /** Cached value for instance hash code. */
  private int hashCode;

  /**
   * Create path instance, initialize value and wrapped file and compute instance hash code. Uses {@link #value} to
   * compute hash code.
   * 
   * @param value path value relative to project.
   */
  protected Path(Project project, String value)
  {
    assert value != null;
    assert !value.startsWith(SEPARATOR);

    this.project = project;
    this.value = value;
    this.file = new File(project.getProjectDir(), value);
    assert this.file.isAbsolute();
    this.hashCode = CT.PRIME + ((value == null) ? 0 : value.hashCode());
  }

  /**
   * Get parent project in which this path is declared.
   * 
   * @return parent project.
   * @see #project
   */
  public Project getProject()
  {
    return project;
  }

  /**
   * Get this path value. Note that returned value is always relative to project root and never starts with slash
   * separator.
   * 
   * @return path value.
   * @see #value
   */
  public String value()
  {
    return value;
  }

  /**
   * Test if this path denotes an existing entity. This predicate simply check that wrapped {@link #file} exists.
   * 
   * @return true if entity exists.
   */
  public boolean exists()
  {
    return file.exists();
  }

  /**
   * Convert this path instance to project Java file; returned file always contains project root. This method test that
   * file really exists and throw exception if not.
   * 
   * @return path file.
   * @throws WoodException if file to return does not exist.
   */
  public File toFile() throws WoodException
  {
    if(!exists()) {
      throw new WoodException("Attempt to use not existing file path |%s|.", value);
    }
    return file;
  }

  /**
   * Test if project entity designated by this path is excluded from build process.
   * 
   * @return true if this path is excluded from build.
   */
  public boolean isExcluded()
  {
    return project.isExcluded(this);
  }

  /**
   * Instance string representation returns the same value as {@link #value()}.
   * 
   * @return path string representation.
   */
  @Override
  public String toString()
  {
    return value;
  }

  /**
   * Get instance hash code.
   * 
   * @return instance hash code.
   * @see #hashCode
   */
  @Override
  public int hashCode()
  {
    return hashCode;
  }

  /**
   * Two path instances are considered equal if they have the same path values. Note that object class is not
   * considered; for example, instances of {@link CompoPath} and {@link DirPath} are equals if have the same value.
   * 
   * @param other path instance to test for equality.
   * @return true if given object is equal with this one.
   */
  @Override
  public boolean equals(Object other)
  {
    if(this == other) return true;
    if(other == null) return false;
    // compare only hash code, which is based on value, no matter the actual class
    // a CompoPath is equals a DirPath is have the same value
    return hashCode() == other.hashCode();
  }

  /**
   * Create path instance suitable to represent given path value. This factory method match path value against
   * {@link CompoPath} and {@link FilePath}, in this order. If none match returns null.
   * 
   * @param project project reference,
   * @param path path value.
   * @return instance for path value or null.
   */
  public static Path create(Project project, String path)
  {
    if(CompoPath.accept(path)) {
      return new CompoPath(project, path);
    }
    if(FilePath.accept(path)) {
      return project.getFile(path);
    }
    return null;
  }
}
