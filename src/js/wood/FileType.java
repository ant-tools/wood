package js.wood;

import java.io.File;

import js.util.Files;

/**
 * Project recognized file types, based on file extension. By convention a component has next file types: layout (HTM),
 * style (CSS), script (JS) and variables and descriptor (XML). All other are considered media files, mostly support
 * images.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public enum FileType
{
  /** Layout is HTM file that describe UI structure. */
  LAYOUT,

  /** CSS styles describe UI elements structure and aspect. */
  STYLE,

  /** Define component behavior. */
  SCRIPT,

  /** XML files are used for variables definition and descriptors. */
  XML,

  /** Media files, mostly support images. */
  MEDIA;

  /**
   * Test if file extension denotes the same type as this one. Uses {@link #forExtension(String)} to convert file
   * extension to file type constant then compare with this one.
   * 
   * @param file file to compare.
   * @return true if <code>file</code> equals this file type.
   */
  public boolean equals(File file)
  {
    return this == forExtension(Files.getExtension(file));
  }

  /**
   * Get file type related to given extension. If extension is not recognized file type is considered media; this is
   * indeed true for null or empty extension.
   * 
   * @param extension file extension.
   * @return file type for extension.
   */
  public static FileType forExtension(String extension)
  {
    if(CT.LAYOUT_EXT.equalsIgnoreCase(extension)) {
      return LAYOUT;
    }
    if(CT.STYLE_EXT.equalsIgnoreCase(extension)) {
      return STYLE;
    }
    if(CT.SCRIPT_EXT.equalsIgnoreCase(extension)) {
      return SCRIPT;
    }
    if(CT.XML_EXT.equalsIgnoreCase(extension)) {
      return XML;
    }
    return MEDIA;
  }
}