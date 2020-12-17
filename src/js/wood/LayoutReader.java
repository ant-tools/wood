package js.wood;

import java.io.IOException;
import java.io.Reader;

import js.util.Strings;

/**
 * Component layout file reader. Layout is described using HTML and is not mandated to have unique root. This reader
 * decorator surrounds layout stream with XML declaration and root element; resulting stream is a valid XML stream.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public final class LayoutReader extends Reader
{
  /** Tag name for injected root element. */
  private static final String ROOT = "layout";

  /** XML stream header contains XML declaration and root opening tag. */
  private static final String HEADER = Strings.concat("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", CT.LN, "<", ROOT, ">", CT.LN);

  /** XML stream footer contains root closing tag. */
  private static final String FOOTER = Strings.concat(CT.LN, "</", ROOT, ">");

  /** External defined reader, decorated by this layout reader instance. */
  private Reader reader;

  /** Current state determine stream source: header, decorated reader or footer. */
  private State state;

  /**
   * Source string current position. Source string can be {@link #HEADER} or {@link #FOOTER} depending on current state.
   */
  private int sourceIndex;

  /**
   * Create layout reader decorator for external reader instance.
   * 
   * @param reader external reader instance.
   */
  public LayoutReader(Reader reader)
  {
    this.state = State.HEADER;
    this.reader = reader;
  }

  /**
   * Convenient layout reader constructor for a given layout file. Create reader for given file and delegates
   * {@link LayoutReader#LayoutReader(Reader)}.
   * 
   * @param file layout file path.
   * @throws WoodException if source file not found.
   */
  public LayoutReader(FilePath file) throws WoodException
  {
    this(file.getReader());
  }

  /**
   * Read characters from source and store them to target buffer at specified offset. Returns the number of characters
   * processed or EOF. Characters source depends on current state but first is {@link #HEADER} followed by decorated
   * reader content and ending with {@link #FOOTER}.
   * 
   * @param buffer target buffer,
   * @param offset target buffer offset,
   * @param length target buffer space.
   * @return the number of characters processed or EOF when all sources are completely read.
   * @throws IOException if source read operation fails.
   */
  @Override
  public int read(char[] buffer, int offset, int length) throws IOException
  {
    int readCount = CT.EOF;

    switch(this.state) {
    case HEADER:
      readCount = copy(HEADER, buffer, offset, length);
      if(readCount != CT.EOF) {
        // keep the state till header end
        break;
      }
      // header is completely read; change state and fall through
      state = State.BODY;

    case BODY:
      readCount = reader.read(buffer, offset, length);
      if(readCount != CT.EOF) {
        // keep the state till decorated reader end
        break;
      }
      // decorated reader is completely read; move state to footer and fall through
      sourceIndex = 0; // prepare source index for footer
      state = State.FOOTER;

    case FOOTER:
      readCount = copy(FOOTER, buffer, offset, length);
      // footer EOF is also end of this reader instance
    }
    return readCount;
  }

  /**
   * Copy source string characters to target buffer at given offset. Is legal that given buffer space to be smaller that
   * source string. This implies is possible this method to be invoked multiple times, for a given source. In order to
   * keep track of source characters position uses {@link #sourceIndex} that is properly initialized before first
   * invocation.
   * <p>
   * Returns the number of characters actually copied or {@link CT#EOF} on source end.
   * 
   * @param source source of characters,
   * @param buffer target buffer,
   * @param offset target buffer offset,
   * @param length target buffer space.
   * @return the number of characters processed or EOF on source end.
   */
  private int copy(String source, char[] buffer, int offset, int length)
  {
    if(sourceIndex == source.length()) {
      return CT.EOF;
    }
    int readCount = 0;
    for(int i = offset; sourceIndex < source.length() && i < length; sourceIndex++, i++) {
      buffer[i] = source.charAt(sourceIndex);
      readCount++;
    }
    return readCount;
  }

  /**
   * Close this layout reader instance. Note that decorated reader is closed too.
   */
  @Override
  public void close() throws IOException
  {
    reader.close();
  }

  /**
   * State machine for layout file reader.
   * 
   * @author Iulian Rotaru
   * @since 1.0
   */
  private static enum State
  {
    /** XML declaration and root open tag is processing. */
    HEADER,

    /** Decorated reader content is the body of resulting XML stream. */
    BODY,

    /** Processing root closing tag. */
    FOOTER
  }
}