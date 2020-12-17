package js.tools.css;

import js.tools.css.impl.ParserImpl;

public final class Builder
{
  public static Parser getParser()
  {
    return new ParserImpl();
  }

  private Builder()
  {
  }
}
