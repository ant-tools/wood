package js.tools.css;

import java.io.Reader;

public interface Parser
{
  void setListener(ParserListener listener);

  StyleSheet parse(String string);

  StyleSheet parse(Reader reader);
}