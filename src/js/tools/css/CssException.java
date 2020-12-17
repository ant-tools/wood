package js.tools.css;

public class CssException extends RuntimeException
{
  private static final long serialVersionUID = -5636400234171812290L;

  public CssException(String message, Object... args)
  {
    super(String.format(message, args));
  }

  public CssException(Throwable cause)
  {
    super(cause);
  }
}
