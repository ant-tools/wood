package js.wood;

import js.util.Strings;

/**
 * Thrown whenever building or preview process fails to complete.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class WoodException extends RuntimeException
{
  /** Java serialization version. */
  private static final long serialVersionUID = 339924983961782597L;

  /**
   * Create exception instance with formatted message.
   * 
   * @param message formatted message,
   * @param args optional arguments for formatted message.
   */
  public WoodException(String message, Object... args)
  {
    super(Strings.format(message, args));
  }

  /**
   * Create exception instance with given cause.
   * 
   * @param cause exception cause.
   */
  public WoodException(Throwable cause)
  {
    super(cause);
  }
}
