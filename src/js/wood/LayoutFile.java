package js.wood;

import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Component layout file contains meta-data about layout. Using this class is a two steps process: first parses layout
 * document and initializes instance state, the second is to detect if layout file is a page. First step is performed by
 * constructor whereas the second by {@link #isPage()}. Instances of this class are immutable.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class LayoutFile
{
  /** Maximum number of templates allowed in a hierarchy. */
  private static final int MAX_TEMPLATES = 8;

  /** Project reference. */
  private Project project;
  
  /** Parent component path. */
  private CompoPath compoPath;

  /** The path of super template or null if there is no template reference declared by this layout. */
  private CompoPath templatePath;

  /** True if this layout file has body element. */
  private boolean hasBody;

  /**
   * Name of editable elements declared by this layout file, possible empty. Note that if a layout has editable elements
   * it is a template.
   */
  private Set<String> editables = new HashSet<String>();

  /** Name of editable elements from templates referenced by this layout, possible empty. */
  private Set<String> templates = new HashSet<String>();

  /** XML synchronous parser. */
  private SAXParser parser;

  /** Handler for XML parser in charge with editable elements and template references handling. */
  private Scanner scanner;

  /** Cached value for instance hash code. Initialized from file path. */
  private int hashCode;

  /** Cached value for string representation. */
  private String string;

  /**
   * Construct immutable instance of layout file designated by given file path. Uses SAX parser to synchronously parse
   * layout file; parser handler detects editable elements and template references and update this instance states.
   * 
   * @param filePath layout file path.
   * @throws WoodException if IO or parsing process fails.
   */
  public LayoutFile(FilePath filePath) throws WoodException
  {
    this.project = filePath.getProject();
    this.compoPath = new CompoPath(project, filePath.getDirPath().value());
    this.hashCode = filePath.hashCode();
    this.string = filePath.toString();

    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      this.parser = factory.newSAXParser();
      this.scanner = new Scanner();
      InputSource inputSource = new InputSource(new LayoutReader(filePath));
      inputSource.setEncoding("UTF-8");
      this.parser.parse(inputSource, scanner);
    }
    catch(Exception e) {
      throw new WoodException(e);
    }
  }

  /**
   * Get parent component path.
   * 
   * @return component path.
   * @see #compoPath
   */
  public CompoPath getCompoPath()
  {
    return compoPath;
  }

  // ------------------------------------------------------
  // Page detection logic

  /** Cache for page flag value returned by {@link #isPage()} method. Avoid re-scanning templates hierarchy. */
  private Boolean isPage;

  /**
   * Test if this layout file describe a page, that is, it has <code>body</code> or use a template with
   * <code>body</code> element. This method just delegates {@link #_isPage()} and caches returned value on
   * {@link #isPage}.
   * 
   * @return true if this layout file describe a page.
   */
  public boolean isPage()
  {
    if(isPage == null) {
      // initializes page flag value lazily since we use project cached layout files, see #getTemplateFile()
      isPage = _isPage();
    }
    return isPage;
  }

  /**
   * Internal workhorse delegated by {@link #isPage()} predicate. This method implements page discovery logic. Simply
   * put a page is a layout with <code>body</code> element. Building process import this body content into
   * {@link PageDocument} body.
   * <p>
   * Body element can be declared in this layout file or can be inherited from a template. If layout file has editable
   * elements is clearly template and cannot be a page; if it has no editables but has body, layout is a page. If no
   * editables and no page we cannot decide and need to scan templates hierarchy. Scanning is performed till templates
   * hierarchy root is reached; if templates root has body element this layout is a page, otherwise is not.
   * <p>
   * This method implements first part of algorithm; for templates scanning invoke
   * {@link #scanTemplatesHierarchy(LayoutFile, Set, Set, int)} recursively.
   * <p>
   * Note that all template references used by layout file should be declared as editable elements in templates
   * hierarchy; otherwise exception is thrown.
   * 
   * @return true if this layout file is a page.
   * @throws WoodException if this layout file template references are not resolved.
   */
  private boolean _isPage() throws WoodException
  {
    if(!editables.isEmpty()) {
      // a layout cannot be a page if it has editable elements, even if it has body
      // if indeed has body it is a page template, but still not page
      return false;
    }
    if(hasBody) {
      return true;
    }
    if(templates.isEmpty()) {
      // if a layout has no body element it should use templates; otherwise is clearly not a page
      return false;
    }

    Set<String> hierarchyTemplates = new HashSet<String>();
    Set<String> hierarchyEditables = new HashSet<String>();
    boolean bodyFound = scanTemplatesHierarchy(this, hierarchyTemplates, hierarchyEditables, 0);

    if(!hierarchyEditables.equals(hierarchyTemplates)) {
      System.out.printf("Inconsistent templates hierarchy:\n" + //
          "\t- component: %s\n" + //
          "\t- templates: %s\n" + //
          "\t- editables: %s", //
          compoPath, hierarchyTemplates, hierarchyEditables);
    }
    if(!hierarchyEditables.containsAll(templates)) {
      throw new WoodException("Component |%s| unresolved templates: %s", compoPath, templates);
    }
    return bodyFound;
  }

  /**
   * Recursively scan the templates hierarchy of this layout file. This method is executed recursively till templates
   * root is reached then returns its {@link #hasBody} value. On process accumulate all found editable and template
   * names.
   * 
   * @param layoutFile current iteration layout file,
   * @param templates template names collector,
   * @param editables editable names collector,
   * @param guard recursive iteration guard.
   * @return true only if templates root has body element.
   * @throws WoodException if too many recursive iterations, {@link #MAX_TEMPLATES}.
   */
  private boolean scanTemplatesHierarchy(LayoutFile layoutFile, Set<String> templates, Set<String> editables, int guard) throws WoodException
  {
    if(++guard == MAX_TEMPLATES) {
      throw new WoodException("Circular template references suspicion found on |%s|.", layoutFile);
    }

    for(String editable : layoutFile.editables) {
      if(!editables.add(editable)) {
        throw new WoodException("Inconsistent templates hierarchy. Editable |%s| from file |%s| is overwritten. Suspect component |%s|.", editable, layoutFile, compoPath);
      }
    }
    if(layoutFile.templatePath == null) {
      return layoutFile.hasBody;
    }
    templates.addAll(layoutFile.templates);
    return scanTemplatesHierarchy(getTemplateFile(layoutFile.templatePath), templates, editables, guard);
  }

  /**
   * Given the component path of a template this method returns its layout file. To avoid SAX parsing uses layout file
   * stored into project cache.
   * 
   * @param templateCompo template component.
   * @return template layout file.
   */
  private LayoutFile getTemplateFile(CompoPath templateCompo)
  {
    for(LayoutFile file : project.getLayouts()) {
      if(file.getCompoPath().equals(templateCompo)) {
        return file;
      }
    }
    return null;
  }

  /**
   * Get hash code value for this instance.
   * 
   * @return instance hash code.
   * @see #hashCode
   */
  @Override
  public int hashCode()
  {
    return hashCode;
  }

  /**
   * Two layout file instances are considered equal if they have the same hash code, that, on its turn is based on
   * layout file path.
   * 
   * @param obj file layout instance to test for equality.
   * @return true if given object is equal with this one.
   */
  @Override
  public boolean equals(Object obj)
  {
    if(this == obj) return true;
    if(obj == null) return false;
    if(getClass() != obj.getClass()) return false;
    LayoutFile other = (LayoutFile)obj;
    if(hashCode != other.hashCode) return false;
    return true;
  }

  /**
   * Return layout file string representation.
   * 
   * @return string representation.
   * @see #string
   */
  @Override
  public String toString()
  {
    return string;
  }

  // ------------------------------------------------------
  // Internal classes

  /**
   * SAX parser for editable elements and template references processing. This parser gets elements from layout file and
   * update layout instance states accordingly.
   * 
   * @author Iulian Rotaru
   * @since 1.0
   */
  private class Scanner extends DefaultHandler
  {
    /** Parser state machine. */
    private State state;

    /**
     * On document start initialize state machine to root.
     */
    @Override
    public void startDocument() throws SAXException
    {
      state = State.ROOT;
    }

    /**
     * Implements parser logic. Skip over root element, initialize {@link LayoutFile#hasBody} from first element then
     * searches for editable and template attributes. Stores editable and template names into
     * {@link LayoutFile#editables}, respective {@link LayoutFile#templates}. If a template attribute is found update
     * also {@link LayoutFile#templatePath}.
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
      switch(state) {
      case ROOT:
        state = State.WAIT_BODY;
        break;

      case WAIT_BODY:
        state = State.CONTENT;
        if(qName.equalsIgnoreCase("body")) {
          LayoutFile.this.hasBody = true;
        }
        // fall through next case

      case CONTENT:
        for(int i = 0; i < attributes.getLength(); ++i) {
          String name = attributes.getQName(i);
          if(name.endsWith("editable")) {
            // wood:editable, data-editable, editable
            LayoutFile.this.editables.add(attributes.getValue(i));
          }
          else if(name.endsWith("template")) {
            // wood:template, data-template, template
            EditablePath editablePath = new EditablePath(project, compoPath.getLayoutPath(), attributes.getValue(i));
            LayoutFile.this.templatePath = editablePath;
            LayoutFile.this.templates.add(editablePath.getEditableName());
          }
        }
      }
    }
  }

  /**
   * Layout file parser state machine.
   * 
   * @author Iulian Rotaru
   * @since 1.0
   */
  private enum State
  {
    /** Initial state. */
    ROOT,

    /** Wait for first element to detect if is body. */
    WAIT_BODY,

    /** Layout file content. */
    CONTENT
  }
}
