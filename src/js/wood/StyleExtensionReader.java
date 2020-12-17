package js.wood;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Style file reader with browser specific declarations injection. This decorator, beside the actual source read, scan
 * for base properties that have browser specific forms and inject them into read stream.
 * <p>
 * For example, when this reader found <code>column-width</code> it takes care to inject browser specific forms, see
 * below sample code and supported declarations. Browser specific declaration value is that from base declaration.
 * 
 * <pre>
 * article {
 *     column-width : 300px;
 * }
 * 
 * article {
 *     column-width : 300px;
 *     -moz-column-width : 300px;
 *     -webkit-column-width : 300px;
 * }
 * </pre>
 * <p>
 * Current implemented browser specific declarations.
 * <table>
 * <tr>
 * <td><b>Base Declaration
 * <td><b>Mozilla
 * <td><b>Chrome / Safari / Opera
 * <tr>
 * <td>column-count
 * <td>-moz-column-count
 * <td>-webkit-column-count
 * <tr>
 * <td>column-width
 * <td>-moz-column-width
 * <td>-webkit-column-width
 * <tr>
 * <td>column-gap
 * <td>-moz-column-gap
 * <td>-webkit-column-gap
 * <tr>
 * <td>column-fill
 * <td>-moz-column-fill
 * <td>
 * </table>
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
public class StyleExtensionReader extends Reader
{
  /** External defined reader, decorated by this style extension reader. */
  private Reader reader;

  /** Current state for style extension reader automaton. */
  private State state;

  /** Declaration builder collects chars after CSS declaration is discovered into source file. */
  private DeclarationBuilder declarationBuilder;

  /** Contains supported browser specific CSS declarations. */
  private Extensions extensions;

  /** Iterator for current injecting browser specific declarations. */
  private Iterator<Character> extensionsIterator;

  /**
   * Create style extension decorator.
   * 
   * @param reader external defined file reader.
   */
  public StyleExtensionReader(Reader reader)
  {
    super();
    this.reader = reader;
    this.state = State.WAIT_DECLARATIONS_BLOCK;
    this.declarationBuilder = new DeclarationBuilder();
    this.extensions = new Extensions();
  }

  /**
   * This method is not needed for style extension reader logic but is required by reader interface. It just fill the
   * buffer, reading char by char.
   * 
   * @param buffer target characters buffer,
   * @param offset buffer offset,
   * @param length buffer length.
   * @return the number of read characters.
   */
  @Override
  public int read(char[] buffer, int offset, int length) throws IOException
  {
    int n = 0;
    for(int i = offset; i < length; ++i, ++n) {
      int c = read();
      if(c == -1) {
        return n > 0 ? n : c;
      }
      buffer[i] = (char)c;
    }
    return n;
  }

  /**
   * Read a char from source style file or from iterator over browser specific declaration. This method reads and parse
   * CSS declarations. If a declaration property is one declared by {@link Extensions} as having browser specific
   * variants initialize {@link #extensionsIterator} and switch the state to {@link State#INSERT_EXTENSION}. At this
   * point this method returns characters from browser specific declarations then continue with base style source file.
   * 
   * @return read character or -1 on EOF.
   */
  @Override
  public int read() throws IOException
  {
    if(state == State.INSERT_EXTENSION) {
      if(extensionsIterator.hasNext()) {
        return extensionsIterator.next();
      }
      state = State.WAIT_DECLARATION;
    }

    int c = reader.read();
    if(c == -1) {
      return c;
    }

    switch(state) {
    case WAIT_DECLARATIONS_BLOCK:
      if(c == '{') {
        state = State.WAIT_DECLARATION;
      }
      break;

    case WAIT_DECLARATION:
      if(c == '}') {
        state = State.WAIT_DECLARATIONS_BLOCK;
        break;
      }
      if(!Character.isWhitespace(c)) {
        declarationBuilder.reset();
        declarationBuilder.add(c);
        state = State.DECLARATION;
      }
      break;

    case DECLARATION:
      declarationBuilder.add(c);
      if(c == ';') {
        extensionsIterator = extensions.getIterator(declarationBuilder);
        if(extensionsIterator != null) {
          state = State.INSERT_EXTENSION;
        }
        else {
          state = State.WAIT_DECLARATION;
        }
      }
      break;

    default:
      throw new IllegalStateException();
    }

    return c;
  }

  /**
   * Close this reader instance.
   */
  @Override
  public void close() throws IOException
  {
    reader.close();
  }

  /**
   * States list for reader automaton.
   * 
   * @author Iulian Rotaru
   * @since 1.1
   */
  private static enum State
  {
    /** Wait for declarations block opening. */
    WAIT_DECLARATIONS_BLOCK,

    /** Waiting for new declaration. */
    WAIT_DECLARATION,

    /** Inside a declaration wait for its end mark while collection property and value. */
    DECLARATION,

    /** Insert browser specific declarations. */
    INSERT_EXTENSION
  }

  /**
   * Builder for source CSS declaration. Declaration builder instance is enacted when {@link #read()} method discover a
   * new declaration start. It collects declaration characters till end mark or EOF.
   * 
   * @author Iulian Rotaru
   * @since 1.1
   */
  private static class DeclarationBuilder
  {
    /** Internal string builder used to collect declaration characters. */
    private StringBuilder builder = new StringBuilder();

    /** Index for declaration property end, relative to declaration start. */
    private int propertyEndIndex;

    /** Index for declaration value start, relative to declaration start. */
    private int valueStartIndex;

    /**
     * Prepare internal builder and indices for a new CSS declaration collect session.
     */
    public void reset()
    {
      builder.setLength(0);
      propertyEndIndex = 0;
      valueStartIndex = 0;
    }

    /**
     * Add CSS declaration character to internal builder. On the fly detect indices for property end and value start,
     * relative to builder start.
     * 
     * @param c character to add.
     */
    public void add(int c)
    {
      builder.append((char)c);

      if(propertyEndIndex == 0 && (c == ':' || Character.isWhitespace(c))) {
        propertyEndIndex = builder.length() - 1;
        return;
      }

      if(propertyEndIndex > 0 && valueStartIndex == 0 && c != ':' && !Character.isWhitespace(c)) {
        valueStartIndex = builder.length() - 1;
      }
    }

    /**
     * Get current loaded CSS declaration property.
     * 
     * @return CSS declaration property.
     */
    public String getProperty()
    {
      return builder.substring(0, propertyEndIndex);
    }

    /**
     * Get current loaded CSS declaration value.
     * 
     * @return CSS declaration value.
     */
    public String getValue()
    {
      return builder.substring(valueStartIndex, builder.length() - 1);
    }
  }

  /**
   * Current supported browser specific declarations.
   * 
   * @author Iulian Rotaru
   * @since 1.1
   */
  private static class Extensions
  {
    /** Browser specific rules. */
    private static final Map<String, String[]> EXTENSIONS = new HashMap<String, String[]>();
    static {
      EXTENSIONS.put("column-count", new String[]
      {
          "-moz-column-count", "-webkit-column-count"
      });

      EXTENSIONS.put("column-width", new String[]
      {
          "-moz-column-width", "-webkit-column-width"
      });

      EXTENSIONS.put("column-gap", new String[]
      {
          "-moz-column-gap", "-webkit-column-gap"
      });

      EXTENSIONS.put("column-fill", new String[]
      {
        "-moz-column-fill"
      });
    }

    /** Iterator over browser specific declarations. */
    private ExtensionsIterator iterator = new ExtensionsIterator();

    /**
     * Get iterator instance initialized with browser specific extension for given CSS declaration, or null if no
     * extension defined.
     * 
     * @param declaration base CSS declaration.
     * @return extension iterator or null.
     */
    public Iterator<Character> getIterator(DeclarationBuilder declaration)
    {
      String[] extensions = EXTENSIONS.get(declaration.getProperty());
      if(extensions == null) {
        return null;
      }
      iterator.init(extensions, declaration.getValue());
      return iterator;
    }
  }

  /**
   * Iterator over current injecting browser specific declarations. Instance of this iterator is returned by
   * {@link Extensions} instance that takes care to call {@link #init(String[], String)} with discovered browser
   * specific properties.
   * 
   * @author Iulian Rotaru
   * @since 1.1
   */
  private static class ExtensionsIterator implements Iterator<Character>
  {
    /**
     * Format for the injected browser specific declaration. It uses extension property and value from base declaration.
     */
    private static final String FORMAT = "\r\n\t%s: %s;";

    /** Properties for browser specific declarations. */
    private String[] properties;

    /** Properties index. */
    private int propertiesIndex;

    /** Value of base declaration that need to be replicated on browser specific rules. */
    private String value;

    /** Current formatted browser specific declaration from which this iterator returns characters in sequence. */
    private String currentDeclaration;

    /** Index for current browser specific declaration. */
    private int currentDeclarationIndex;

    /**
     * Initialize this iterator instance with browser specific properties and value from base CSS declaration.
     * 
     * @param properties browser specific properties,
     * @param value base declaration value.
     */
    public void init(String[] properties, String value)
    {
      assert properties.length > 0;
      this.properties = properties;
      this.value = value;

      propertiesIndex = 0;
      setCurrentDeclaration();
    }

    /**
     * Detect if there are more characters to process. This predicate prepare on the fly current processing browser
     * specific declaration till all {@link #properties} are processed.
     * 
     * @return true if there is at least one not processed character left.
     */
    @Override
    public boolean hasNext()
    {
      if(currentDeclarationIndex < currentDeclaration.length()) {
        return true;
      }
      if(++propertiesIndex < properties.length) {
        setCurrentDeclaration();
        return true;
      }
      return false;
    }

    /**
     * Initialize currently processing browser specific declaration. This method used {@link #FORMAT}. Resulting
     * declaration is stored into {@link #currentDeclaration} and {@link #currentDeclarationIndex} reset.
     * <p>
     * Declaration format is like that: <code>column-count: 4;</code>.
     */
    private void setCurrentDeclaration()
    {
      currentDeclarationIndex = 0;
      currentDeclaration = String.format(FORMAT, properties[propertiesIndex], value);
    }

    /**
     * Get next character from currently processing browser specific declaration. This getter updates
     * {@link #currentDeclarationIndex}; do not invoke it repeatedly without {@link #next()}.
     * 
     * @return next character from current declaration.
     */
    @Override
    public Character next()
    {
      return currentDeclaration.charAt(currentDeclarationIndex++);
    }

    /**
     * Remove operation is not supported by this iterator.
     * 
     * @throws UnsupportedOperationException remove is not supported.
     */
    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
