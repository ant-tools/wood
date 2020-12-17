package js.tools.commons.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import js.tools.commons.BugError;

/**
 * Functions for files, byte and character streams copy and file path manipulation. This class supplies methods for byte
 * and character streams transfer and files copy. If not otherwise specified, in order to simplify caller logic, streams
 * methods close streams before returning, including on error. Also for files copy, target file is created if does not
 * exist; if target file creation fails, perhaps because of missing rights or target exists and is a directory, file
 * methods throw {@link FileNotFoundException}. Please note that in all cases target content is overwritten.
 * <p>
 * Finally, there are method working with temporary files as target. These methods return newly created temporary file
 * and is caller responsibility to remove it when is not longer necessary. This library does not keep record of created
 * temporary file and there is no attempt to remove then, not even at virtual machine exit.
 * <p>
 * This utility class allows for sub-classing. See {@link js.util} for utility sub-classing description.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public final class Files
{
  /** Disable default constructor synthesis. */
  private Files()
  {
  }

  /**
   * Return base name for given file path. Returns null if given path parameter is null.
   * 
   * @param path file path string.
   * @return file name without extension or null.
   */
  public static String basename(String path)
  {
    return path != null ? basename(new File(path)) : null;
  }

  /**
   * Return file name without extension. Returns null if given <code>file</code> parameter is null.
   * 
   * @param file file to return base name for.
   * @return file name, without extension, or null.
   */
  public static String basename(File file)
  {
    if(file == null) {
      return null;
    }
    String fileName = file.getName();
    int i = fileName.lastIndexOf('.');
    return i != -1 ? fileName.substring(0, i) : fileName;
  }

  /**
   * Replace all dots from given qualified name with platform specific path separator.
   * 
   * <pre>
   *    js.net.Transaction -&gt; js/net/Transaction or js\net\Transaction
   * </pre>
   * 
   * Returns null if <code>qualifiedName</code> parameter is null.
   * 
   * @param qualifiedName qualified name.
   * @return resulting path or null.
   */
  public static String dot2path(String qualifiedName)
  {
    return qualifiedName != null ? qualifiedName.replace('.', File.separatorChar) : null;
  }

  /**
   * Convert qualified name to platform specific path and add given extension. Uses {@link #dot2path(String)} to convert
   * <code>qualifiedName</code> to file path then add give <code>fileExtension</code>. Is legal for
   * <code>fileExtension</code> to start with dot.
   * 
   * <pre>
   *    js.net.Transaction java -&gt; js/net/Transaction.java or js\net\Transaction.java
   *    js.net.Transaction .java -&gt; js/net/Transaction.java or js\net\Transaction.java
   * </pre>
   * 
   * Returns null if <code>qualifiedName</code> parameter is null. If <code>fileExtension</code> parameter is null
   * resulting path has no extension.
   * 
   * @param qualifiedName qualified name,
   * @param fileExtension requested file extension, leading dot accepted.
   * @return resulting file path or null.
   */
  public static String dot2path(String qualifiedName, String fileExtension)
  {
    if(qualifiedName == null) {
      return null;
    }
    StringBuilder path = new StringBuilder();
    path.append(dot2path(qualifiedName));
    if(fileExtension != null) {
      if(fileExtension.charAt(0) != '.') {
        path.append('.');
      }
      path.append(fileExtension);
    }
    return path.toString();
  }

  /**
   * Same as {@link #dot2path(String)} but always uses forward slash as path separator, as used by URLs. Returns null if
   * <code>qualifiedName</code> parameter is null.
   * 
   * @param qualifiedName qualified name.
   * @return resulting URL path or null.
   */
  public static String dot2urlpath(String qualifiedName)
  {
    return qualifiedName != null ? qualifiedName.replace('.', '/') : null;
  }

  /**
   * Convert file path to an Unix like path string. If given <code>path</code> is Windows like replaces drive letter
   * with Unix path root and all Windows path separators to Unix counterpart.
   * 
   * <pre>
   *    D:\\temp\file.txt -&gt; /temp/file.txt
   * </pre>
   * 
   * If <code>path</code> is already Unix like this method leave it as it is but remove trailing path separator, if any.
   * Returns null if <code>path</code> parameter is null.
   * 
   * @param path path to convert, trailing path separator ignored.
   * @return Unix like path string or null.
   */
  public static String path2unix(String path)
  {
    if(path == null) {
      return null;
    }
    if(path.endsWith("/") || path.endsWith("\\")) {
      path = path.substring(0, path.length() - 1);
    }
    return path.replaceAll("(^[a-zA-Z]\\:\\\\?)?\\\\", "/");
  }

  /**
   * Copy characters from a reader to a given writer then close both character streams.
   * 
   * @param reader character stream to read from,
   * @param writer character stream to write to.
   * @throws IOException if read or write operation fails.
   * @throws IllegalArgumentException if reader or writer is null.
   */
  public static void copy(Reader reader, Writer writer) throws IOException
  {
    Params.notNull(reader, "Reader");
    Params.notNull(writer, "Writer");

    if(!(reader instanceof BufferedReader)) {
      reader = new BufferedReader(reader);
    }
    if(!(writer instanceof BufferedWriter)) {
      writer = new BufferedWriter(writer);
    }

    try {
      char[] buffer = new char[4096];
      for(;;) {
        int readChars = reader.read(buffer);
        if(readChars == -1) {
          break;
        }
        writer.write(buffer, 0, readChars);
      }
    }
    finally {
      close(reader);
      close(writer);
    }
  }

  /**
   * Copy source file bytes to target file.
   * 
   * @param inputFile source file,
   * @param outputFile destination file.
   * @throws IllegalArgumentException if input or output file is null.
   * @throws FileNotFoundException if <code>inputFile</code> does not exist.
   * @throws IOException bytes processing fails.
   */
  public static void copy(File inputFile, File outputFile) throws IOException
  {
    Params.notNull(inputFile, "Input file");
    Params.notNull(outputFile, "Output stream");
    copy(new FileInputStream(inputFile), new FileOutputStream(outputFile));
  }

  /**
   * Copy source file bytes to requested output stream. Note that output stream is closed after transfer completes,
   * including on error.
   * 
   * @param inputFile source file,
   * @param outputStream destination output stream.
   * @throws FileNotFoundException if <code>file</code> does not exist.
   * @throws IOException bytes processing fails.
   * @throws IllegalArgumentException if input file or output stream is null.
   */
  public static void copy(File inputFile, OutputStream outputStream) throws IOException
  {
    Params.notNull(inputFile, "Input file");
    Params.notNull(outputStream, "Output stream");
    copy(new FileInputStream(inputFile), outputStream);
  }

  /**
   * Copy bytes from input to given output stream then close both byte streams. Please be aware this method closes both
   * input and output streams. This is especially important if work with ZIP streams; trying to get/put next ZIP entry
   * after this method completes will fail with <em>stream closed</em> exception.
   * 
   * @param inputStream bytes input stream,
   * @param outputStream bytes output stream.
   * @throws IOException if reading or writing fails.
   * @throws IllegalArgumentException if input or output stream is ZIP stream.
   */
  private static void copy(InputStream inputStream, OutputStream outputStream) throws IOException
  {
    if(!(inputStream instanceof BufferedInputStream)) {
      inputStream = new BufferedInputStream(inputStream);
    }
    if(!(outputStream instanceof BufferedOutputStream)) {
      outputStream = new BufferedOutputStream(outputStream);
    }

    try {
      byte[] buffer = new byte[4096];
      int length;
      while((length = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, length);
      }
    }
    finally {
      close(inputStream);
      close(outputStream);
    }
  }

  /**
   * Close given <code>closeable</code> if not null but ignoring IO exception generated by failing close operation.
   * Please note that this helper method does not throw {@link IOException} if close operation fails but still print the
   * event to error stream.
   * 
   * @param closeable closeable to close.
   */
  public static void close(Closeable closeable)
  {
    if(closeable == null) {
      return;
    }
    try {
      closeable.close();
    }
    catch(IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Get file extension as lower case or empty string. Returned extension does not contain dot separator, that is,
   * <code>htm</code> not <code>.htm</code>. Returns null if <code>file</code> parameter is null.
   * 
   * @param file file to return extension of.
   * @return file extension or empty string or null if <code>file</code> parameter is null.
   */
  public static String getExtension(File file)
  {
    return file != null ? getExtension(file.getAbsolutePath()) : null;
  }

  /**
   * Get the lower case extension of the file denoted by given path or empty string if not extension. Returned extension
   * does not contain dot separator, that is, <code>htm</code> not <code>.htm</code>. Returns null if given
   * <code>path</code> parameter is null.
   * 
   * @param path the path of the file to return extension of.
   * @return file extension, as lower case, or empty string if no extension.
   */
  public static String getExtension(String path)
  {
    if(path == null) {
      return null;
    }

    // search for both Unix and Windows path separators because this logic is common for files and URLs

    int extensionPos = path.lastIndexOf('.');
    int lastUnixPos = path.lastIndexOf('/');
    int lastWindowsPos = path.lastIndexOf('\\');
    int lastSeparatorPos = Math.max(lastUnixPos, lastWindowsPos);

    // do not consider extension separator before last path separator, e.g. /etc/rc.d/file
    int i = (lastSeparatorPos > extensionPos ? -1 : extensionPos);
    return i == -1 ? "" : path.substring(i + 1).toLowerCase();
  }

  /**
   * Create MD5 message digest for requested file content. This method returns a 16-bytes array with computed MD5
   * message digest value.
   * 
   * @param file file to create message digest for.
   * @return 16-bytes array of message digest.
   * @throws FileNotFoundException if <code>file</code> does not exist.
   * @throws IOException if file read operation fails.
   */
  public static byte[] getFileDigest(File file) throws IOException
  {
    InputStream inputStream = new BufferedInputStream(new FileInputStream(file));

    MessageDigest messageDigest = null;
    try {
      byte[] buffer = new byte[1024];
      messageDigest = MessageDigest.getInstance("MD5");
      for(;;) {
        int bytesRead = inputStream.read(buffer);
        if(bytesRead <= 0) {
          break;
        }
        messageDigest.update(buffer, 0, bytesRead);
      }
    }
    catch(NoSuchAlgorithmException e) {
      throw new BugError("JVM with missing MD5 algorithm for message digest.");
    }
    finally {
      inputStream.close();
    }

    return messageDigest.digest();
  }
}
