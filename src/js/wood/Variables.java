package js.wood;


import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import js.util.Strings;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Variables store name/value pairs that can be referenced from source files. In addition to having a name, variable
 * belongs to a type; so to fully refer a variable one need to know both type and name. See {@link ResourceType} for
 * recognized types and {@link Reference} for syntax and sample usage.
 * <p>
 * Variables are resources and are processed the same way as media files: by {@link ReferenceHandler}. When source file
 * is read, {@link SourceReader} discovers references and delegates reference handler. There are distinct reference
 * handler instances for build and preview processes but basically variables references are text replaces by their
 * values; this class provides getters just for that.
 * <p>
 * A variables has a scope; its name is private to a component. Is legal for variables from different component to have
 * the same name. Anyway, asset variables are global. Value retrieving logic attempts first to get value from component
 * variables and only if value miss tries asset variables. Also, when language is requested, attempt first to retrieve
 * that language and if not found uses default.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class Variables
{
  private Project project;
  
  /** Optional asset variables used when this variables miss a value. */
  private Variables assetVariables;

  /** Site theme variables used when component and asset variables miss value. */
  private Variables themeVariables;

  /**
   * Variable values mapped to languages. Resources without language variant are identified by null language. Null
   * language values are used when project is not multi-language. Also used when a value for a given language is
   * missing.
   */
  private Map<Locale, Map<Reference, String>> localeValues = new HashMap<Locale, Map<Reference, String>>();

  /** XML synchronous parser. */
  private SAXParser parser;

  /** Handler for SAX parser in charge with variables files loading. */
  private Scanner scanner;

  /** References resolver for this variable values. */
  ReferencesResolver resolver;

  /**
   * Stack for references nesting level trace, global per execution thread. Nesting level trace logic assume references
   * tree iteration occurs in a single thread.
   */
  private static ThreadLocal<Stack<String>> levelTraceTLS = new ThreadLocal<Stack<String>>();

  /**
   * Create empty variables instance.
   */
  public Variables(Project project)
  {
    this.project = project;
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      this.parser = factory.newSAXParser();
      this.resolver = new ReferencesResolver();
    }
    catch(Exception e) {
      throw new WoodException(e);
    }
  }

  /**
   * Create variables instance and load its values from provided directory files. Directory should designate a component
   * or project assets. If given <code>dir</code> parameter is not an existing directory this constructor does not load
   * values and resulting variables instance is empty.
   * 
   * @param dirPath directory to scan for variable files.
   */
  public Variables(Project project, DirPath dirPath)
  {
    this(project);
    load(dirPath);
  }

  /**
   * Set asset variables used when this variables miss a value. It is caller responsibility to ensure given asset
   * variables are loaded from project asset directory.
   * 
   * @param assetVariables asset variables already initialized.
   */
  public void setAssetVariables(Variables assetVariables)
  {
    this.assetVariables = assetVariables;
  }

  public void setThemeVariables(Variables themeVariables)
  {
    this.themeVariables = themeVariables;
  }

  /**
   * Load variable values form file. This method just delegates {@link #_load(FilePath)} re-throwing exceptions as
   * library runtime exception.
   * 
   * @param file file to load values.
   * @throws WoodException if file reading or parsing fails.
   */
  public void load(FilePath file) throws WoodException
  {
    try {
      _load(file);
    }
    catch(Exception e) {
      throw new WoodException(e);
    }
  }

  /**
   * Handy alternative for {@link #get(String, Reference, FilePath, ReferenceHandler)} when language variants are not
   * used. Returns null if value not found.
   * 
   * @param reference variable reference,
   * @param source source file where reference is declared,
   * @param listener resource references handler.
   * @return variable value.
   * @throws WoodException if variable value not found.
   */
  public String get(Reference reference, FilePath source, ReferenceHandler listener) throws WoodException
  {
    return get(null, reference, source, listener);
  }

  /**
   * Get variable value for requested language and resolve nested references. If <code>language</code> is null uses
   * default language; default language values are those loaded from files without language variant.
   * <p>
   * This method attempts to retrieve variable value using next heuristic:
   * <ol>
   * <li>attempt to get value from this variables instance, for specified language,
   * <li>if not found try to get value for default language,
   * <li>if still not found try to retrieve value form project global assets and theme variables, in this order - see
   * {@link #assetVariables}, {@link #themeVariables}; execute this step only if source file is not asset or theme,
   * <li>throw exception if not found
   * </ol>
   * <p>
   * Is legal for a variable value to contain nested references. This method takes care to normalize returned value,
   * that is, it invokes {@link ReferencesResolver} with found value. There is a recursive chain of methods invoked till
   * references tree is completely resolved. See {@link SourceReader} for a discussion on references tree iteration.
   * 
   * @param language language, null for default language,
   * @param reference variable reference,
   * @param source source file where reference is declared,
   * @param handler resource references handler.
   * @return variable value.
   * @throws WoodException if variable value not found.
   */
  public String get(Locale locale, Reference reference, FilePath source, ReferenceHandler handler) throws WoodException
  {
    String value = getValue(locale, reference, source, handler);

    // 3. if still not found try to retrieve value form project global assets and theme variables, in this order
    // anyway, do not execute this step if source file is from assets or site styles
    if(value == null && assetVariables != null) {
      value = assetVariables.getValue(locale, reference, source, handler);
    }
    if(value == null && themeVariables != null) {
      value = themeVariables.getValue(locale, reference, source, handler);
    }

    // 4. if value not found throws exception with formatted message
    if(value == null) {
      throw new WoodException("Missing variables |%s| referenced from |%s|.", reference, source);
    }
    return value;
  }

  private String getValue(Locale locale, Reference reference, FilePath source, ReferenceHandler handler)
  {
    String value = null;

    // 1. attempt to get value from this variables instance, for specified language
    Map<Reference, String> values = localeValues.get(locale);
    if(values != null) {
      value = values.get(reference);
    }

    // 2. if not found try to get value for default locale
    if(value == null) {
      values = localeValues.get(project.getDefaultLocale());
      if(values != null) {
        value = values.get(reference);
      }
    }

    // 4. if value not found or is empty throws exception with formatted message
    if(value == null || value.isEmpty()) {
      return null;
    }

    // trace contains source file and reference and avoid circular referencing
    Stack<String> levelTrace = levelTrace();
    String trace = String.format("%s:%s", source, reference);
    if(levelTrace.contains(trace)) {
      throw new WoodException(exceptionMessage(levelTrace, "Circular variable references."));
    }
    levelTrace.push(trace);

    // resolve nested references; see resolver API
    value = resolver.parse(value, source, handler);
    levelTrace.pop();
    return value;
  }

  /**
   * Retrieve level trace stack for current thread. Create instance on the fly, if missing.
   * 
   * @return level trace stack.
   */
  private static Stack<String> levelTrace()
  {
    Stack<String> levelTrace = levelTraceTLS.get();
    if(levelTrace == null) {
      levelTrace = new Stack<String>();
      levelTraceTLS.set(levelTrace);
    }
    return levelTrace;
  }

  /**
   * Create exception message with level trace dump.
   * 
   * @param levelTrace level trace stack,
   * @param message exception message.
   * @return formatted exception message.
   */
  private static String exceptionMessage(Stack<String> levelTrace, String message)
  {
    StringBuilder builder = new StringBuilder(message + " Level trace stack follows:\n");
    for(int i = 0; i < levelTrace.size(); ++i) {
      builder.append(Strings.concat("\t- ", levelTrace.get(i), "\n"));
    }
    return builder.toString();
  }

  /**
   * Load variable values from given directory files. Traverses directory files in no particular order searching for
   * names matching variables file pattern. If a resource file is found store its values mapped to language variant; if
   * no language variant is detected uses null. Note that scanning process workhorse is the {@link Scanner} class.
   * 
   * @param dir directory path, should point to an existing directory.
   */
  private void load(DirPath dir)
  {
    dir.files(new FilesHandler()
    {
      @Override
      public void onFile(FilePath file) throws Exception
      {
        if(file.isVariables()) {
          _load(file);
        }
      }
    });
  }

  /**
   * Load variable values from file. This workhorse is designed for {@link #load(FilePath)} and {@link #load(DirPath)}
   * values loaders.
   * 
   * @param file file to load values.
   * @throws IOException if file reading fails.
   * @throws SAXException if XML parsing fails.
   */
  private void _load(FilePath file) throws SAXException, IOException
  {
    Locale locale = file.getVariants().getLocale();
    if(locale == null) {
      locale = project.getDefaultLocale();
    }
    Map<Reference, String> values = localeValues.get(locale);
    if(values == null) {
      values = new HashMap<Reference, String>();
      localeValues.put(locale, values);
    }
    scanner = new Scanner(file, values);
    parser.parse(file.toFile(), scanner);
  }

  // ------------------------------------------------------
  // Internal classes.

  /**
   * Base class for variable value builders. Value builders are used in conjunction with XML SAX handler, see
   * {@link Scanner}. A value builder instance is created on root element from variables definition file, considering
   * {@link ResourceType} encoded by root - see {@link #instance(ResourceType)}. Builder instance is reused for all
   * variable elements from file; there is {@link #reset()} method that prepare instance for new value. Once variable
   * element discovered, value builder helps collecting variable value from XML stream via
   * {@link #addValue(char[], int, int)} method.
   * <p>
   * This class deals with plain string only. There are specialized value builders for formatted text and styles, see
   * {@link TextValueBuilder} and {@link StyleValueBuilder}. Also, this base class defines hooks methods for subclasses
   * benefit: {@link #addParameter(String, String)}, {@link #startTag(String)} and {@link #endTag(String)}.
   * 
   * @author Iulian Rotaru
   * @since 1.0
   */
  private static class ValueBuilder
  {
    /** Value builder storage. */
    protected StringBuilder value = new StringBuilder();

    /**
     * Prepare builder instance for new value assembling.
     */
    public void reset()
    {
      value.setLength(0);
    }

    /**
     * Add value characters from buffer.
     * 
     * @param buffer XML stream characters buffer,
     * @param offset buffer offset,
     * @param length the number of characters to process.
     */
    public void addValue(char[] buffer, int offset, int length)
    {
      value.append(buffer, offset, length);
    }

    /**
     * A value parameter is extra data related to particular value. It is defined as and attribute of the value element.
     * See every subclass implementation for value parameters usage. For base case this method is not supported.
     * 
     * @param name parameter name,
     * @param value parameter value.
     * @throws UnsupportedOperationException base class does not support value parameters.
     */
    public void addParameter(String name, String value) throws UnsupportedOperationException
    {
      throw new UnsupportedOperationException();
    }

    /**
     * Called by variables scanner when a new variable element is discovered.
     * 
     * @param name variable element name.
     * @throws UnsupportedOperationException base class does not process this hook.
     */
    public void startTag(String name) throws UnsupportedOperationException
    {
      throw new UnsupportedOperationException();
    }

    /**
     * Called by variables scanner when a variable element closing tag is discovered.
     * 
     * @param name variable element name.
     * @throws UnsupportedOperationException base class does not process this hook.
     */
    public void endTag(String name) throws UnsupportedOperationException
    {
      throw new UnsupportedOperationException();
    }

    /**
     * Get value as string.
     * 
     * @return builder value.
     * @see #value
     */
    @Override
    public String toString()
    {
      return value.toString();
    }

    /**
     * Create value builder instance suitable for resource type.
     * 
     * @param resourceType resource type.
     * @return value builder instance for resource type.
     */
    public static ValueBuilder instance(ResourceType resourceType)
    {
      switch(resourceType) {
      case TEXT:
        return new TextValueBuilder();

      case STYLE:
        return new StyleValueBuilder();

      default:
        return new ValueBuilder();
      }
    }
  }

  /**
   * Value builder for {@link ResourceType#TEXT} variables. This builder collect formatted text that is in essence a
   * HTML fragment. Takes care to collect, beside text characters, start and end tags for formatting elements.
   * 
   * @author Iulian Rotaru
   * @since 1.0
   */
  private static class TextValueBuilder extends ValueBuilder
  {
    /**
     * Add opening tag for formatting element.
     * 
     * @param name element tag name.
     */
    @Override
    public void startTag(String name)
    {
      value.append('<');
      value.append(name);
      value.append('>');
    }

    /**
     * Add closing tag for formatting element.
     * 
     * @param name element tag name.
     */
    @Override
    public void endTag(String name)
    {
      value.append("</");
      value.append(name);
      value.append('>');
    }
  }

  /**
   * Value builder for {@link ResourceType#STYLE} variables. This builder convert style XML description into standard
   * syntax for CSS rules, see sample code below.
   * <p>
   * Here is a sample of CSS style properties description as present into <code>style</code> variables definition files.
   * Style property name is encoded as element tag name whereas style property value as element text. The name
   * <code>compo</code> is used by style reference, see <code>@style/comp</code> from next sample code.
   * 
   * <pre>
   * &lt;compo&gt;
   *    &lt;background-color&gt;black&lt;/background-color&gt;
   *    &lt;width&gt;50%&lt;/width&gt;
   * &lt;/compo&gt;
   * </pre>
   * 
   * <pre>
   * body {
   *    {@literal @}style/compo
   *    . . .
   * }
   * </pre>
   * <p>
   * This value builder convert above XML styles into CSS style properties that can text replace style reference. Note
   * that, though not depicted references are supported for style property values.
   * 
   * <pre>
   * background-color: black;
   * width: 50%;
   * </pre>
   * 
   * @author Iulian Rotaru
   *
   */
  private static class StyleValueBuilder extends ValueBuilder
  {
    /**
     * Enable CSS style property value processing. This flag is controlled by start and end of a new element into XML
     * stream. If not set to true {@link #addValue(char[], int, int)} is actually ignoring given buffer value.
     */
    private boolean expectValue;

    /**
     * Style value support <code>parent</code> parameter from which inherits properties. Parent is encoded in resulting
     * CSS style properties as reference to style, that is, <code>@style/parent</code>.
     * 
     * @param name parameter name, always <code>parent</code>,
     * @param value parameter value is in fact the parent name to be encoded into style reference.
     */
    @Override
    public void addParameter(String name, String value)
    {
      if("parent".equals(name)) {
        this.value.append("@style/");
        this.value.append(value);
      }
    }

    /**
     * When new element is discovered into XML stream create a new CSS style property with element name. This method
     * also takes care to include colon separator.
     * <p>
     * After writing CSS style proper name this method enable property value processing setting {@link #expectValue}
     * flag to true.
     * 
     * @param name element name translated into CSS style property name.
     */
    @Override
    public void startTag(String name)
    {
      if(value.length() > 0) {
        // every new rule -less the first, starts on new line with tab
        // first rule is expected to inherit new line and tab from insertion point on style file
        value.append("\n\t");
      }
      value.append(name);
      value.append(": ");
      expectValue = true;
    }

    /**
     * Add CSS style property value is guaranteed to be called after {@link #startTag(String)}. It contains chunks of
     * element text content that is translated into CSS style property value. For a given CSS property value this method
     * can be called multiple times.
     * 
     * @param buffer XML stream characters buffer,
     * @param offset buffer offset,
     * @param length the number of characters to process.
     */
    @Override
    public void addValue(char[] buffer, int offset, int length)
    {
      if(expectValue) {
        super.addValue(buffer, offset, length);
      }
    }

    /**
     * Uses XML stream element end to end CSS style property with semicolon. After concluding CSS style property this
     * method disable CSS style value processing by setting {@link #expectValue} flag to false.
     * 
     * @param name element name, ignored but requested by interface.
     */
    @Override
    public void endTag(String name)
    {
      value.append(";");
      expectValue = false;
    }
  }

  /**
   * Scanner for variable values definition file. Variables values are stored into XML files processed by a SAX parser.
   * This scanner actually implement SAX parser handler.
   * <p>
   * See below sample file for expected format. {@link ResourceType} is the root of the XML document. A resources file
   * can contain only one resource type; this is by design to promote clear types separation. Direct child on root is
   * the reference name, that is, the name used by source file to refer the variable - see {@link Reference} for syntax.
   * Everything inside reference element is considered value and is copied as plain text. If reference element has
   * nested children - e.g. {@link ResourceType#TEXT}, they are processed like a XML fragment and copied with element
   * start/end tags included. Anyway, the actual value reading from SAX stream is delegated to {@link ValueBuilder}.
   * 
   * <pre>
   *  &lt;type&gt;
   *      . . .   
   *      &lt;reference&gt;value&lt;/reference&gt;
   *      . . .
   *  &lt;/type&gt;
   *  
   *  &lt;body&gt;
   *      &lt;h1&gt;@type/reference&lt;/h1&gt;
   *      . . .
   *  &lt;/body&gt;
   * </pre>
   * 
   * In above example there is a variables definition file and a sample reference from a layout file. After reference
   * processing <code>value</code> from XML file will be inserted into layout file as text for <code>h1</code> element.
   * 
   * @author Iulian Rotaru
   */
  private static class Scanner extends DefaultHandler
  {
    /** Keep values definition file for error tracking. */
    private FilePath sourceFile;

    /** Variable values storage mapped to related reference. This storage instance is created externally. */
    private Map<Reference, String> values;

    /**
     * Variable values definition file has a resource type used to create reference instances. Note that by design all
     * variables from a file should have the same type.
     */
    private ResourceType resourceType;

    /**
     * Value builder used to actually collect variable value. There are specialized value builders for different
     * resource types. Builder instance is reused for entire variables definition file.
     */
    private ValueBuilder builder;

    /**
     * Current element nesting level acts as state for finite state automaton controlling this scanner behavior. Level
     * value start with 0, is incremented on every element start and decremented on element end. This way it keeps track
     * of nesting level.
     */
    private int level;

    /**
     * Create scanner instance that store discovered variable into given variables storage.
     * 
     * @param file the path for variables definition file, for error tracking,
     * @param values external storage for variable values.
     */
    public Scanner(FilePath file, Map<Reference, String> values)
    {
      super();
      this.sourceFile = file;
      this.values = values;
    }

    /**
     * Takes care to reset nesting level for every new document.
     */
    @Override
    public void startDocument() throws SAXException
    {
      level = 0;
    }

    /**
     * Handle new element discovered into SAX stream. When detect root element this method initialized
     * {@link #resourceType} and create value builder instance, see {@link #builder}. For every element direct child to
     * root, that is, nesting level 1, reset the builder and add parameters from element attributes, if any. For the
     * other deeper descendants just invoke {@link ValueBuilder#startTag(String)} with element name.
     * 
     * @param uri unused namespace URI,
     * @param localName local name unused because namespace is not used,
     * @param qName element qualified name,
     * @param attributes attributes attached to element, possible empty.
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
      switch(level) {
      case 0:
        resourceType = ResourceType.getValueOf(qName);
        if(resourceType == ResourceType.UNKNOWN) {
          throw new WoodException("Bad resource type |%s| in file |%s|", qName, sourceFile);
        }
        builder = ValueBuilder.instance(resourceType);
        break;

      case 1:
        builder.reset();
        for(int i = 0; i < attributes.getLength(); ++i) {
          builder.addParameter(attributes.getQName(i), attributes.getValue(i));
        }
        break;

      default:
        if(resourceType != ResourceType.TEXT && resourceType != ResourceType.STYLE) {
          throw new WoodException("Not allowed nested element |%s| in  file |%s|. Only text and style variables support nested elements.", qName, sourceFile);
        }
        builder.startTag(qName);
      }
      ++level;
    }

    /**
     * Handle element closing tag. For root direct child elements stores value from {@link #builder} to {@link #values
     * values storage}. For deeper descendants just invoke {@link ValueBuilder#endTag(String)}.
     * 
     * @param uri unused namespace URI,
     * @param localName local name unused because namespace is not used,
     * @param qName element qualified name.
     */
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
      --level;
      switch(level) {
      case 0:
        break;

      case 1:
        values.put(new Reference(sourceFile, resourceType, qName), builder.toString());
        break;

      default:
        builder.endTag(qName);
      }
    }

    /**
     * Send text stream to value builder.
     * 
     * @param ch buffer of characters from SAX stream,
     * @param start buffer offset,
     * @param length buffer capacity.
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
      if(level > 1) {
        builder.addValue(ch, start, length);
      }
    }
  }
}
