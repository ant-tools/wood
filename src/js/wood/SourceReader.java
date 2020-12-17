package js.wood;

import java.io.IOException;
import java.io.Reader;

import js.util.Params;
import js.wood.eval.Interpreter;

/**
 * Source file reader with {@literal @}meta processing, also known as at-meta. This class is a decorator for a
 * characters stream reader. Beside standard reading it looks for resource references and expressions evaluation
 * described by at-meta syntax and invokes external {@link ReferenceHandler}, respective {@link Interpreter} when
 * discover them. Also inject layout parameters provided by {@link LayoutParameters} when encounter <code>@param</code>
 * reference.
 * <p>
 * As mentioned, at-meta annotations define both references and evaluations. General syntax is described here. For
 * specific syntax details see {@link Reference}, respective {@link js.wood.eval.Expression}.
 * 
 * <pre>
 * at-meta    = AT meta
 * meta       = reference | parameter | evaluation
 * reference  = resource-type SEP ?(path SEP) name
 * parameter  = PARAM SEP name 
 * evaluation = EVAL expression
 * ; for complete reference description see {@link Reference}
 * ; for parameters details see {@link LayoutParameters}
 * ; expression is defined by {@link js.wood.eval.Expression}
 * 
 * ; terminal symbols definition
 * AT    = "@"      ; at-meta mark
 * SEP   = "/"      ; path separator
 * PARAM = "param"  ; layout parameter reference
 * EVAL  = "eval"   ; expression evaluation identifier
 * </pre>
 * 
 * <h5>Resource Reference</h5>
 * Component source files can contain references to variables. Is legal for variable values to also contain references;
 * this creates a tree of references that is traversed recursively in depth-first order. There are a number of methods
 * invoked in a chain that creates this recursive reference scanning.
 * <ol>
 * <li>{@link SourceReader source file reader} discovers a variable reference and delegates reference handler,
 * <li>{@link ReferenceHandler reference handler} retrieves value from variables instance,
 * <li>{@link Variables#get(String, Reference, FilePath, ReferenceHandler) variables getter} invokes value references
 * resolver with found value,
 * <li>{@link ReferencesResolver references resolver} discovers a variable reference and delegates reference handler,
 * back to 2,
 * <li>repeat 2..4 till entire values tree is resolved.
 * </ol>
 * Note that variables value getter, at point 3, implements recursive loop level guard.
 * 
 * <h5>Layout Parameters</h5>
 * Widget and template layouts can contain layout parameters defined using <code>@param</code> parameter references.
 * When source reader discover a parameter reference uses {@link #layoutParameters} to retrieve named parameter value
 * and text replace parameter reference with its value. Layout parameters map is initialized beforehand and injected via
 * constructor {@link SourceReader#SourceReader(FilePath, LayoutParameters, ReferenceHandler)}.
 * <p>
 * Layout parameters map is initialized from <code>wood:param</code> operator at widget or template invocation.
 * 
 * <pre>
 * &lt;div wood:widget="compo/list-view" wood:param="caption:Users Info"&gt;&lt;/div&gt;
 * 
 * &lt;div data-template="template/dialog#body" data-param="name:user-edit;caption:Edit User;class:js.dialog.Dialog"&gt;&lt;/div&gt;
 * </pre>
 * 
 * As stated layout parameters are the operand for <code>wood:param</code> operator; see {@link LayoutParameters} for
 * layout parameters syntax.
 * 
 * <h5>Expression Evaluation</h5>
 * Expression evaluation syntax has two parts: evaluation and expression. Evaluation is described by at-meta syntax, see
 * above, whereas expression is defined by {@link js.wood.eval.Expression}. This source reader uses {@link MetaBuilder}
 * to detect evaluations and extract expression that is passed to {@link Interpreter}. At this stage expression is legal
 * to contain resource references; this class uses {@link ReferencesResolver} to resolve them before passing expression
 * to interpreter. Note that reference resolver is step 4. from recursive reference scanning described by resource
 * reference section.
 * <p>
 * Interpreter performs the actual expression evaluation and returns a not null and not empty value that is used by this
 * class to replace evaluation annotation from source file.
 * 
 * @author Iulian Rotaru
 */
public final class SourceReader extends Reader
{
  /** Source file define the scope of resource references. */
  private FilePath sourceFile;

  /** Layout parameters map or null if source file is not an widget or template layout. */
  private LayoutParameters layoutParameters;

  /** External defined reference handler in charge with resource processing. */
  private ReferenceHandler referenceHandler;

  /** Expression interpreter. */
  private Interpreter interpreter;

  /** Resource references resolver. */
  private ReferencesResolver resolver;

  /** External defined reader, decorated by this source reader instance. */
  private Reader reader;

  /** Resource reference builder. */
  private MetaBuilder metaBuilder;

  /** Current state determine how every character is processed. */
  private State state;

  /** Current variable value. */
  private String value;

  /** Current variable value index. */
  private int valueIndex;

  /** Store the character after at-meta, used to detect at-meta end. */
  private int charAfterMeta;

  /**
   * Create source reader decorator for external defined reader. Source may contain resource references and expression
   * evaluations that are declared into source file as at-meta annotations. For resources processing this constructor
   * takes external defined resource references handler. For expression evaluations creates interpreter and references
   * resolver instances.
   * 
   * @param reader external defined reader,
   * @param sourceFile source file used as scope for resource references,
   * @param referenceHandler external reference handler.
   * @throws IllegalArgumentException if any parameter is null or source file does not exist.
   */
  public SourceReader(Reader reader, FilePath sourceFile, ReferenceHandler referenceHandler)
  {
    super();
    Params.notNull(reader, "Reader");
    Params.notNull(sourceFile, "Source file");
    Params.isTrue(sourceFile.exists(), "Source file does not exist");
    Params.notNull(referenceHandler, "Reference handler");

    this.reader = reader;
    this.sourceFile = sourceFile;
    this.referenceHandler = referenceHandler;
    this.interpreter = new Interpreter();
    this.resolver = new ReferencesResolver();
    this.metaBuilder = new MetaBuilder(sourceFile);
    this.state = State.TEXT;
  }

  /**
   * Convenient source reader constructor for a given source file. Create file reader for source file and delegates
   * {@link SourceReader#SourceReader(Reader, FilePath, ReferenceHandler)}.
   * 
   * @param sourceFile source file to create source reader for,
   * @param referenceHandler external defined reference handler.
   * @throws IllegalArgumentException if any parameter is null or source file does not exist.
   */
  public SourceReader(FilePath sourceFile, ReferenceHandler referenceHandler)
  {
    this(reader(sourceFile), sourceFile, referenceHandler);
  }

  /**
   * Construct source file reader for widget or template layout with parameters. For layout files without parameters
   * <code>layoutParameters</code> argument is not null but its content is empty. Reference handler resolve values for
   * variables and media files references.
   * 
   * @param sourceFile source file for widget layout,
   * @param layoutParameters layout parameters, possible empty,
   * @param referenceHandler external reference handler.
   * @throws IllegalArgumentException if any parameter is null, source file does not exist or is not a layout.
   */
  public SourceReader(FilePath sourceFile, LayoutParameters layoutParameters, ReferenceHandler referenceHandler)
  {
    this(reader(sourceFile), sourceFile, referenceHandler);
    Params.isTrue(sourceFile.isLayout(), "Source file is not a widget or template layout");
    this.layoutParameters = layoutParameters;
  }

  /**
   * Get a reader for the given source file throwing illegal argument if source file is null.
   * 
   * @param sourceFile source file path.
   * @return source file reader.
   * @throws IllegalArgumentException if source file parameter is null.
   */
  private static Reader reader(FilePath sourceFile)
  {
    Params.notNull(sourceFile, "Source file");
    return sourceFile.getReader();
  }

  /**
   * This method is not needed for source reader logic but is required by reader interface. It just fill the buffer,
   * reading char by char.
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
   * Get character from decorated reader and process it accordingly current state value. On the way updates the state.
   * <p>
   * If current reader position is outside at-meta annotation this method just return the source text character. When
   * discover at-meta mark, this method blocks reading the at-meta content. Once at-meta annotation parsed delegates
   * {@link #referenceHandler} or {@link #interpreter} to process resource reference, respective expression evaluation,
   * that return a not null not empty value.
   * <p>
   * At this point, this method start returning the value, char by char, instead of reading from decorated reader. When
   * value is completely retrieved start processing again the source text characters.
   * 
   * @return source text or resource value character.
   */
  @Override
  public int read() throws IOException
  {
    int c = -1;

    switch(state) {
    case TEXT:
      c = reader.read();
      if(c != Reference.MARK) {
        break;
      }
      state = State.AT_META;
      metaBuilder.reset();
      // fall through next REFERENCE state

    case AT_META:
      while(metaBuilder.add(c)) {
        c = reader.read();
      }

      if(metaBuilder.isReference()) {
        Reference reference = metaBuilder.getReference();
        if(!reference.isValid()) {
          throw new WoodException("Invalid reference |%s| in source file |%s|. Unknown type.", metaBuilder, sourceFile);
        }
        value = referenceHandler.onResourceReference(reference, sourceFile);
      }
      else if(metaBuilder.isParameter()) {
        String parameter = metaBuilder.getParameter();
        // not all sources support parameters; for now only widget layout source
        if(layoutParameters != null) {
          value = layoutParameters.getValue(parameter);
          if(value == null) {
            throw new WoodException("Missing layout parameter |%s| in source file |%s|.", parameter, sourceFile);
          }
        }
      }
      else if(metaBuilder.isEvaluation()) {
        // is valid for expression to contain resource references that need to be resolved before evaluation
        String expression = resolver.parse(metaBuilder.getExpression(), sourceFile, referenceHandler);
        value = interpreter.evaluate(expression, sourceFile.toString());
      }
      else {
        // if built reference is not recognized sent it back to source stream unchanged since is valid to have reference
        // mark in source syntax, e.g. @media from CSS file
        value = metaBuilder.toString();
      }

      state = State.VALUE;
      charAfterMeta = c;
      valueIndex = 1;
      return value.charAt(0);

    case VALUE:
      if(valueIndex < value.length()) {
        return value.charAt(valueIndex++);
      }
      state = State.TEXT;
      c = charAfterMeta;
      break;
    }

    return c;
  }

  /**
   * Close this source reader instance.
   */
  @Override
  public void close() throws IOException
  {
    reader.close();
  }

  // ------------------------------------------------------
  // Internal classes

  /**
   * Source reader state machine.
   * 
   * @author Iulian Rotaru
   * @since 1.0
   */
  private static enum State
  {
    /** Source file text, outside reference. */
    TEXT,

    /** At-meta annotation is parsing. */
    AT_META,

    /** At-meta is completely parsed and its value is returning, char by char. */
    VALUE
  }

  /**
   * At-meta annotation builder is enacted when stream reader discover at-meta mark. It collects at-meta annotation
   * characters till end mark or EOF, see {@link #add(int)}. On the fly detects if at-meta is a resource reference or an
   * expression evaluation and update {@link #isEvaluation} flag accordingly.
   * 
   * @author Iulian Rotaru
   */
  private static class MetaBuilder
  {
    /** Widget actual parameter identifier. */
    private static final String PARAMETER_ID = "@param";

    /** Expression evaluation identifier. */
    private static final String EVALUATION_ID = "@eval";

    /** Mark for expression begin. */
    private static final char EXPRESSION_BEGIN = '(';

    /** Mark for expression end. */
    private static final char EXPRESSION_END = ')';

    private FilePath sourceFile;

    /** Internal characters collector for reference value. */
    private StringBuilder builder = new StringBuilder();

    /** Store reference separator index for reference instance creation, see {@link #getReference()}. */
    private int separatorIndex;

    /** True if this at-meta annotation is a reference to a variable or media file. */
    private boolean isReference;

    /** True if this at-meta annotation is an widget parameters. */
    private boolean isParameter;

    /** Flag indicating that this at-meta annotation is an expression evaluation. */
    private boolean isEvaluation;

    /**
     * Expression to evaluate allows for nested expressions of not limited nesting level. This field keeps track of
     * expressions nesting level. It is used to detect expression end.
     */
    private int expressionNestingLevel;

    public MetaBuilder(FilePath sourceFile)
    {
      super();
      this.sourceFile = sourceFile;
    }

    /**
     * Reset this instance state for a new reference build.
     */
    public void reset()
    {
      builder.setLength(0);
      separatorIndex = 0;
      isReference = false;
      isParameter = false;
      isEvaluation = false;
      expressionNestingLevel = 0;
    }

    /**
     * Store reference character and return true if collecting is to be continuing. Returns false if reference end mark
     * or end of file is detected. On the fly updates {@link #separatorIndex}.
     * <p>
     * This method is designed for usage in a <code>while</code> loop, see sample code.
     * 
     * <pre>
     * while(referenceBuilder.add(c)) {
     *   c = reader.read();
     * }
     * </pre>
     * 
     * @param c reference character to add.
     * @return true if reference collecting is to be continuing.
     */
    public boolean add(int c)
    {
      if(c == -1 || isEndMark(c)) {
        return false;
      }

      // detect separator for both resource reference and expression, that is, '/' or '('
      // if expression separator found initialize expression flag and expression nesting level to 1
      if(separatorIndex == 0) {
        switch(c) {
        case EXPRESSION_BEGIN:
          if(EVALUATION_ID.equals(builder.toString())) {
            isEvaluation = true;
            expressionNestingLevel = 1;
          }
          separatorIndex = builder.length();
          break;

        case Reference.SEPARATOR:
          if(PARAMETER_ID.equals(builder.toString())) {
            isParameter = true;
          }
          else {
            isReference = true;
          }
          separatorIndex = builder.length();
          break;
        }
      }

      // update expression nesting level only after separator found
      else {
        switch(c) {
        case EXPRESSION_BEGIN:
          ++expressionNestingLevel;
          break;

        case EXPRESSION_END:
          --expressionNestingLevel;
          break;
        }
      }

      builder.append((char)c);
      return true;
    }

    /**
     * Test if current processing at-meta is a resource reference. This predicate should be called only after at-meta
     * build completes otherwise its value is not defined.
     * <p>
     * Current implementation consider resource reference if at-meta uses '/' as separator.
     * 
     * @return true if this at-meta is a resource reference.
     */
    public boolean isReference()
    {
      // if no separator found this at-meta is a source file syntax at-syntax, perhaps CSS at-rule or script annotation
      return isReference;
    }

    /**
     * Get resource reference instance. This method should be called only if {@link #isReference()} predicate returns
     * true. Otherwise returned value is not defined.
     * 
     * @return reference instance.
     */
    public Reference getReference()
    {
      ResourceType resourceType = ResourceType.getValueOf(builder.substring(1, separatorIndex));
      String name = builder.substring(separatorIndex + 1);
      return new Reference(sourceFile, resourceType, name);
    }

    /**
     * Test if current parsing at-meta is an expression evaluation. At-meta qualifies for expression evaluation if
     * expression separator was found and if at-meta identified equals {@link #EVALUATION_ID}.
     * 
     * @return true if this at-meta is an expression evaluation.
     */
    public boolean isEvaluation()
    {
      return isEvaluation;
    }

    /**
     * Return expression including opening and closing parenthesis. This method returns meaningful data only if this
     * at-meta is an expression evaluation. Caller should ensure that calling first {@link #isEvaluation}.
     * 
     * @return the expression of the evaluation meta.
     */
    public String getExpression()
    {
      return builder.substring(separatorIndex);
    }

    /**
     * Test if current parsing at-meta is an widget parameter. At-meta is a widget parameter if at-meta identified
     * equals {@link #PARAMETER_ID}.
     * 
     * @return true if this at-meta is an expression evaluation.
     */
    public boolean isParameter()
    {
      return isParameter;
    }

    public String getParameter()
    {
      return builder.substring(separatorIndex + 1);
    }

    /**
     * Return instance string representation of current loaded reference value.
     * 
     * @return instance string representation.
     */
    @Override
    public String toString()
    {
      return builder.toString();
    }

    /**
     * Test if character is a reference end mark.
     * 
     * @param c character to test.
     * @return true if character is reference end mark.
     */
    private boolean isEndMark(int c)
    {
      return isEvaluation ? expressionNestingLevel == 0 : !Reference.isChar(c) && c != EXPRESSION_BEGIN;
    }
  }
}
