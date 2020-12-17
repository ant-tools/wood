package js.wood;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import js.util.Files;

/**
 * Resolve resource references that may exist into a variable value. Resources resolving is a text replace process; it
 * replaces reference with variable value or media file URL path. The actual resource processing is delegated to
 * external reference handler.
 * <p>
 * This class provides a parsing method for variable value, see {@link #parse(String, FilePath, ReferenceHandler)}. It
 * is invoked by {@link Variables#get(String, Reference, FilePath, ReferenceHandler)} with found variable value.
 * Value reference may point to a new value that in its turn may have references, creating a tree of values linked by
 * references. This class parsing occurs in a recursive loop, in depth-first order, till entire values tree is resolved.
 * See {@link SourceReader} for a discussion on references tree iteration.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class ReferencesResolver
{
  /**
   * Parse variable value and invoke {@link ReferenceHandler reference handler} when encounter a resource reference.
   * Return the value with resource references resolved.
   * 
   * @param value variable value,
   * @param sourceFile source file that define the scope of the variable reference,
   * @param referenceHandler reference handler to invoke for reference processing.
   * @return value with references resolved.
   */
  public String parse(String value, FilePath sourceFile, ReferenceHandler referenceHandler)
  {
    Reader reader = new SourceReader(new StringReader(value), sourceFile, referenceHandler);
    StringWriter writer = new StringWriter();
    try {
      Files.copy(reader, writer);
    }
    catch(IOException e) {
      throw new WoodException(e);
    }
    return writer.toString();
  }
}
