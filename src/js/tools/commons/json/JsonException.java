package js.tools.commons.json;

public class JsonException extends RuntimeException
{
  /** Java serialization version. */
  private static final long serialVersionUID = -2893927318478574250L;

  public JsonException(String message, Object... args)
  {
    super(String.format(message, args));
  }

  public JsonException(Throwable cause)
  {
    super(cause);
  }
}
