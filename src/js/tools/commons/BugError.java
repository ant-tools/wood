package js.tools.commons;

public class BugError extends Error
{
  /** Java serialization version. */
  private static final long serialVersionUID = 419015620226969981L;

  public BugError(String format, Object... args)
  {
    super(String.format(format, args));
  }

  public BugError(Throwable throwable)
  {
    super(throwable);
  }
}
