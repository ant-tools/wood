package js.wood;

import java.io.IOException;

/**
 * Resource reference handler invoked by {@link SourceReader} and {@link ReferencesResolver} when discover a reference.
 * It is designed for the benefit of building and preview processes that uses it for resources processing - variables
 * injection and media files handling.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public interface ReferenceHandler
{
  /**
   * Handler invoked when a resource reference is discovered. A resource reference is private to a scope defined by
   * source file where reference is declared. This handler process the reference and returns a value meant to replace
   * reference into declaring source file. Note that <code>reference</code> parameter is guaranteed to be valid.
   * <p>
   * Returned value is mandatory to be not null and not empty. If this handler is not able to process the reference it
   * should rise exception.
   * <p>
   * It is expected that handler implementation to perform IO operation; for this reason handler is allowed to throw
   * {@link IOException}.
   * 
   * @param reference resource reference, guaranteed to be valid,
   * @param sourceFile source file path acting as reference scope.
   * @return not null and not empty reference value.
   * @throws IOException if handler IO operations fail.
   * @throws WoodException if handler is not able to process the reference.
   */
  String onResourceReference(Reference reference, FilePath sourceFile) throws IOException, WoodException;
}