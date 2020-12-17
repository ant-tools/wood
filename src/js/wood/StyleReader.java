package js.wood;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import js.util.Params;
import js.util.Strings;

/**
 * Style file reader adds style variants, as media queries sections, to given base style file. This class is used in
 * conjunction with {@link SourceReader}, see sample code below. Style reader append media sections to style file and
 * source reader resolve resources from aggregated stream. Both tasks are processed on the fly, while style file content
 * is reading.
 * <p>
 * Be it a component <code>page</code> with a base style file <code>page.css</code>. Also, component has two style file
 * variants, namely <code>page_w1200.css</code> and <code>page_w800.css</code>.
 * 
 * <pre>
 * FilePath styleFile = project.getFile("page.css");
 * Files.copy(new SourceReader(new StyleReader(styleFile)), ...
 * </pre>
 * <p>
 * Resulting style file would be something like snippet below. First <code>body</code> rule set is from base style file
 * <code>page.css</code> whereas the other two are from style variants, <code>page_w1200.css</code> respective
 * <code>page_w800.css</code>. Please notice relation between file path variant and <code>max-width</code> expression
 * from media query.
 * 
 * <pre>
 * body {
 *     width: 1000px;
 * }
 * 
 * {@literal @}media screen and (max-width:1200px) {
 * body {
 *     width: 600px;
 * }
 * }
 * 
 * {@literal @}media screen and (max-width:800px) {
 * body {
 *     width: 400px;
 * }
 * }
 * </pre>
 * 
 * @author Iulian Rotaru
 */
public class StyleReader extends Reader
{
  /** Media section header. */
  private static final String HEADER = Strings.concat(CT.LN, "@media %s {", CT.LN, CT.LN);

  /** Media section footer. */
  private static final String FOOTER = Strings.concat(CT.LN, "}", CT.LN);

  /** Current processed style file reader, base style file or style variants. */
  private Reader reader;

  /** Reader automaton current state. */
  private State state;

  /** Media expressions list mapped to style files, possible empty. */
  private SortedMap<FilePath, String> variants;

  /** Index for current processed style variant. */
  private Iterator<Map.Entry<FilePath, String>> variantsIterator;

  /** Current processed source, media section header or footer. */
  private String source;

  /** Currently processed source index. */
  private int sourceIndex;

  /**
   * Create reader instance for given style file. Style file parameter should be a base style file, that is, it should
   * have no variants.
   * 
   * @param styleFile base style file.
   */
  public StyleReader(FilePath styleFile)
  {
    Params.isFalse(styleFile.hasVariants(), "Style reader decorates a base style file and supplied file is a variant.");
    this.reader = new StyleExtensionReader(styleFile.getReader());
    this.state = State.BASE_CONTENT;

    // TODO: scanning for variants in this reader constructor is not the best option
    // but style variants is still experimental solution and needs to be certified by production code

    variants = new TreeMap<FilePath, String>(new Comparator<FilePath>()
    {
      @Override
      public int compare(FilePath f1, FilePath f2)
      {
        // media selector for variants with greater weight should be inserted first
        // so sort variants in descending order, that is, compare f2 with f1 not vice versa
        return getVariantWeight(f2.getVariants()).compareTo(getVariantWeight(f1.getVariants()));
      }
    });

    final String styleFileBasename = styleFile.getBaseName();
    styleFile.getDirPath().files(FileType.STYLE, new FilesHandler()
    {
      @Override
      public boolean accept(FilePath file)
      {
        return file.getBaseName().equals(styleFileBasename);
      }

      @Override
      public void onFile(FilePath file) throws Exception
      {
        String mediaExpressions = MediaQueryFactory.getMediaExpressions(file.getVariants());
        if(mediaExpressions != null) {
          variants.put(file, mediaExpressions);
        }
      }
    });

    // at this point variants map is sorted in proper order for media selection insertion
    // see above comparator from sorted map creation

    variantsIterator = variants.entrySet().iterator();
  }

  private static Integer getVariantWeight(Variants variants)
  {
    // TODO replace magic numbers

    int weight = 0;
    if(variants.hasViewportHeight()) {
      weight += variants.getViewportHeight();
    }
    if(variants.hasViewportWidth()) {
      weight += (4000 + variants.getViewportWidth());
    }
    if(variants.hasScreen()) {
      weight += (8000 + variants.getScreen().getResolution());
    }
    if(variants.hasOrientation()) {
      weight += (12000 + variants.getOrientation().ordinal());
    }
    return weight != 0 ? weight : Integer.MAX_VALUE;
  }

  /**
   * Implementation for abstract {@link Reader#read(char[], int, int)} is also this style reader automaton
   * implementation. The main concern of this method is to read base style file content. After read completes switch
   * state to variants processing, of course if there are any. Read every variant content surrounded by media section
   * header and footer.
   * 
   * @param buffer target buffer,
   * @param offset target buffer offset,
   * @param length target buffer length.
   * @throws IOException if read operation fails.
   */
  @Override
  public int read(char[] buffer, int offset, int length) throws IOException
  {
    if(state == State.BASE_CONTENT) {
      int readCount = reader.read(buffer, offset, length);
      if(readCount != CT.EOF) {
        return readCount;
      }
      state = State.NEXT_VARIANT;
    }

    int readCount = CT.EOF;
    VARIANTS_LOOP: for(;;) {
      switch(state) {
      case NEXT_VARIANT:
        reader.close();
        if(!variantsIterator.hasNext()) {
          // if no more variants break for loop with readCount set to EOF
          break VARIANTS_LOOP;
        }
        Entry<FilePath, String> variantsEntry = variantsIterator.next();
        // prepare next reader; current one was already closed so is safe to replace it wit a new one
        reader = new StyleExtensionReader(variantsEntry.getKey().getReader());

        // prepare source and source index for variant header copy
        // variants entry contains prepared media expressions ready to be inserted into media block header
        source = String.format(HEADER, variantsEntry.getValue());
        sourceIndex = 0;

        // prepare next state and fall through
        state = State.VARIANT_HEADER;

      case VARIANT_HEADER:
        readCount = copy(buffer, offset, length);
        if(readCount != CT.EOF) {
          // keep the state till header end
          break VARIANTS_LOOP;
        }
        // header is completely read; change state and fall to variant content processing
        state = State.VARIANT_CONTENT;

      case VARIANT_CONTENT:
        readCount = reader.read(buffer, offset, length);
        if(readCount != CT.EOF) {
          // keep the state till current style file reader end
          break VARIANTS_LOOP;
        }
        // prepare source and source index for footer copy
        source = FOOTER;
        sourceIndex = 0;
        // reader is completely read; move state to footer and fall through
        state = State.VARIANT_FOOTER;

      case VARIANT_FOOTER:
        readCount = copy(buffer, offset, length);
        if(readCount != CT.EOF) {
          // keep copying footer till its end
          break VARIANTS_LOOP;
        }
        state = State.NEXT_VARIANT;
        // goto NEXT_VARIANT state via for loop continue
        continue;

      default:
        throw new IllegalStateException();
      }
    }

    return readCount;
  }

  /**
   * Copy {@link #source} string characters to target buffer at given offset. Is legal that given buffer space to be
   * smaller that source string. This implies is possible this method to be invoked multiple times, for a given source.
   * In order to keep track of source characters position uses {@link #sourceIndex} that is properly initialized before
   * first invocation.
   * <p>
   * Returns the number of characters actually copied or {@link CT#EOF} on source end.
   * 
   * @param buffer target buffer,
   * @param offset target buffer offset,
   * @param length target buffer space.
   * @return the number of characters processed or EOF on source end.
   */
  private int copy(char[] buffer, int offset, int length)
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
   * Close style reader.
   */
  @Override
  public void close() throws IOException
  {
    // reading loop takes care to close the style file reader
  }

  /**
   * States list for style reader finite automaton.
   * 
   * @author Iulian Rotaru
   * @since 1.0
   */
  private static enum State
  {
    /** Base style file content is processed. */
    BASE_CONTENT,

    /** Select next variant, if any. */
    NEXT_VARIANT,

    /** Copy header for current variant, selected on {@link #NEXT_VARIANT}. */
    VARIANT_HEADER,

    /** Variant style file content is processed. */
    VARIANT_CONTENT,

    /** Copy footer for current variant. On completion go back to {@link #NEXT_VARIANT}. */
    VARIANT_FOOTER;
  }

  /**
   * Media query factory. This factory creates expressions lists from given file variants. Returned expressions list
   * already contains AND operator and are ready to insert into media query block header.
   * 
   * @author Iulian Rotaru
   * @since 1.1
   */
  private static class MediaQueryFactory
  {
    /**
     * Create media query expressions list for given file variants. Returned media expressions contains both media type
     * and <code>media feature</code> / <code>value</code> pairs, joined by AND operator. This method applies next
     * heuristic:
     * <ul>
     * <li>if variants contains view port maximum width create <code>max-width</code> expression with width measured in
     * pixels,
     * <li>if variants contains device class uses maximum or minimum device width in conjunction with
     * {@link #MOBILE_MAX_WIDTH} to resolve mobile and desktop; for television set just use standard media type,
     * <li>if variants contains device orientation uses standard media expressions related to landscape and portrait.
     * </ul>
     * If more variants are present used <code>and</code> operator.
     * 
     * @param variants file variants to consider for media query expressions.
     * @return media query expressions.
     */
    public static String getMediaExpressions(Variants variants)
    {
      List<String> mediaExpressions = new ArrayList<String>();
      String mediaType = null;

      final Variants.Screen screen = variants.getScreen();
      switch(screen) {
      case LARGE:
        mediaType = "screen";
        // media selected only if screen width strictly greater than 1200px
        mediaExpressions.add(String.format("(min-width : %dpx)", screen.getResolution() + 1));
        break;

      case NORMAL:
        mediaType = "screen";
        // no media query since normal is default, screen width values (992, 1200]
        break;

      case MEDIUM:
        mediaType = "screen";
        // media selected only if screen width is less or equal to 992
        mediaExpressions.add(String.format("(max-width : %dpx)", screen.getResolution()));
        break;

      case SMALL:
        mediaType = "screen";
        // media selected only if screen width is less or equal to 768
        mediaExpressions.add(String.format("(max-width : %dpx)", screen.getResolution()));
        break;

      case EXTRA_SMALL:
        mediaType = "screen";
        // media selected only if screen width is less or equal to 560
        mediaExpressions.add(String.format("(max-width : %dpx)", screen.getResolution()));
        break;

      default:
        break;
      }

      if(variants.hasViewportWidth()) {
        mediaType = "screen";
        mediaExpressions.add(String.format("(max-width : %dpx)", variants.getViewportWidth()));
      }

      if(variants.hasViewportHeight()) {
        mediaType = "screen";
        mediaExpressions.add(String.format("(max-height : %dpx)", variants.getViewportHeight()));
      }

      switch(variants.getOrientation()) {
      case LANDSCAPE:
        mediaExpressions.add("(orientation: landscape)");
        break;

      case PORTRAIT:
        mediaExpressions.add("(orientation: portrait)");
        break;

      default:
        break;
      }

      if(mediaType != null) {
        mediaExpressions.add(0, mediaType);
      }
      return mediaExpressions.isEmpty() ? null : Strings.join(mediaExpressions, " and ");
    }
  }
}
