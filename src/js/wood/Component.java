package js.wood;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import js.dom.Attr;
import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
import js.dom.Element;
import js.dom.w3c.DocumentBuilderImpl;
import js.lang.BugError;
import js.util.Strings;

/**
 * Meta-data about component consolidated instance. Component is a piece of user interface designed to interconnect with
 * another components. Following OOP paradigm there are two major relations: inheritance and composition. Inheritance is
 * implemented via templates mechanism whereas composition is achieved using widget components. This class discovers
 * relations and consolidates the component instance. On the fly collects data about used resources and takes care to
 * inject layout parameters.
 * <p>
 * A component resides into own directory and has mandatory layout file (HTML5) and optional style (CSS3), script (JS)
 * and resource files - resources are variables and media files. A component may also have a descriptor. By convention
 * component name is the same as directory, layout, style, script and descriptor files basename.
 * <p>
 * This class deals with source files aggregation: it consolidates layout from its templates hierarchy and widgets tree
 * and collect used style files and script classes. But does not deal with resources references. For that purpose,
 * component class is configured with {@link ReferenceHandler} that is in charge with resource references processing:
 * inject variables and process media files. Resources processing is externalized because build and preview processes
 * have different needs.
 * <p>
 * Implementation note. Resource references handling is performed on source file reading, on the fly. Component needs to
 * load its layout and all depending layout documents from file; while layout file is reading {@link #referenceHandler}
 * resolve variables and copy media files. The point is, after layout loaded, media files are already present into build
 * directory and loaded document has references resolved.
 * 
 * @author Iulian Rotaru
 */
public class Component
{
  /** Parent project reference. */
  private final Project project;

  /** Operators handler created by project, based on the naming strategy selected by developer. */
  private final OperatorsHandler operators;

  /** Component name. By convention is the name of component directory. */
  private final String name;

  /** Display is the component name formatted for user interface. */
  private final String display;

  /** This component base layout file path. */
  private final FilePath baseLayoutPath;

  /** Layout parameters used by source reader to inject parameter values. */
  private final LayoutParameters layoutParameters;

  /** External defined resource references handler in charge with resources processing. */
  private final ReferenceHandler referenceHandler;

  /** The list of style files used by this component, in the proper oder for page document inclusion. */
  private final List<FilePath> styleFiles = new ArrayList<FilePath>();

  /** Aggregated set of script classes declared by this component, in no particular order. */
  private final Set<String> scriptClasses = new HashSet<String>();

  /** Third party scripts are loaded from remote servers, for example google maps API. This list contains absolute URLs. */
  private final Set<String> thirdPartyScripts = new HashSet<String>();

  /** Optional component descriptor, default to null. */
  private final ComponentDescriptor descriptor;

  /**
   * Consolidated layout for this component instance. It contains layouts from templates hierarchy and widgets tree.
   * Also references are resolved, that is, references replaced with variables values and media URL paths.
   */
  private Element layout;

  /**
   * The list of script files used by this component, in proper order for page document inclusion. This list contains
   * scripts directly referenced by component and all dependencies but does not include third party scripts.
   */
  private List<ScriptFile> scriptFiles;

  /**
   * Create aggregated instance for component identified by given path. Just delegates
   * {@link #Component(FilePath, ReferenceHandler)} with component layout path.
   * 
   * @param compoPath component path,
   * @param referenceHandler resource references handler.
   */
  public Component(CompoPath compoPath, ReferenceHandler referenceHandler)
  {
    this(compoPath.getLayoutPath(), referenceHandler);
  }

  /**
   * Create component instance identified by its layout file path. This constructor completely initializes all its
   * fields: consolidates its layout and fill styles and scripts lists . While reading layout files
   * {@link #referenceHandler} is invoked, that, on its turn resolves resource references on the fly.
   * <p>
   * This constructor is designed for preview process; it allows for preview script inclusion into script dependencies
   * scanning.
   * 
   * @param layoutPath component layout file path,
   * @param referenceHandler resource references handler,
   */
  public Component(FilePath layoutPath, ReferenceHandler referenceHandler)
  {
    this.project = layoutPath.getProject();
    this.operators = this.project.getOperatorsHandler();
    this.layoutParameters = new LayoutParameters();
    this.referenceHandler = referenceHandler;

    this.baseLayoutPath = layoutPath;
    this.name = layoutPath.getBaseName();
    this.display = Strings.toTitleCase(name);

    FilePath descriptorFile = layoutPath.getDirPath().getFilePath(layoutPath.getBaseName() + CT.DOT_XML_EXT);
    // TODO: create component descriptor only if file exists
    this.descriptor = new ComponentDescriptor(descriptorFile, referenceHandler);
  }

  public void scan(boolean includePreviewScript)
  {
    // consolidate component layout from its templates and widgets
    // update internal styles list with components related style file
    layout = scanComponentsTree(baseLayoutPath, 0);

    // after layout consolidated scan it for included scripts and class and formatter declarations
    // 'script' is used only to declare script dependencies and remove after processing
    // 'class' and 'format' are j(s)-script library operators and need to be preserved

    Element pageClassEl = operators.getByOperator(layout, Operator.SCRIPT);
    if(pageClassEl != null) {
      operators.removeOperator(pageClassEl, Operator.SCRIPT);
    }

    for(Element editableEl : operators.findByOperator(layout, Operator.EDITABLE)) {
      if(editableEl.isEmpty()) {
        editableEl.remove();
      }
      else {
        operators.removeOperator(editableEl, Operator.EDITABLE);
      }
    }

    // uses project detected script classes to find out script files that this component depends on
    scriptFiles = collectScriptFiles(project.getScriptFiles(scriptClasses));

    // if preview script file is to be included updates this component scripts
    // preview script and its direct dependencies are included last
    if(includePreviewScript) {
      ScriptFile previewScript = project.getPreviewScript(getPreviewScript());
      if(previewScript != null) {
        updateScriptFiles(scriptFiles, previewScript);
      }
    }
  }

  /**
   * Scan project resources and library directories for requested component layout, resolving its dependencies. Scanning
   * process is recursively, in depth-first order, and actually solves two kinds of dependencies: templates hierarchy
   * and child widgets tree. Templates hierarchy processing is delegated to {@link #loadLayoutDocument(FilePath, int)}
   * whereas widgets tree is handled by this method logic.
   * <p>
   * Widgets are processed in the order from this parent layout, self-invoking this method recursively. This way an
   * widget may have other child widgets creating a not restricted complex widgets tree.
   * <p>
   * Note that the actual HTML document file reading is performed by {@link SourceReader} that detects resource
   * references and invoke configured {@link #referenceHandler}; on its turn resource references handler takes care to
   * inject variables and process media files.
   * 
   * @param layoutPath layout file path,
   * @param guardCount circular reference guard.
   * @return component layout with templates hierarchy and widgets tree resolved.
   */
  private Element scanComponentsTree(FilePath layoutPath, int guardCount)
  {
    if(guardCount++ == 8) {
      throw new WoodException("Circular compo references suspicion. Too many nesting levels on |%s|. Please check wood:widget attributes!", layoutPath);
    }

    // components tree scanning is an recursive process that happens in two steps:
    // 1. getLayoutDocument() takes care to process component templates hierarchy, if this base component uses
    // 2. this method logic scans recursively for and insert widget components - for short widgets

    // if the case, returned component layout has templates elements with base component content inserted
    Document layout = loadLayoutDocument(layoutPath, guardCount);

    // scan for widget components referenced by this base component
    // an external widget component is identified by widget path, which is a standard project file path

    // widget path element is part of base component and contains the widget path
    // it acts as insertion point; is where widget layout is inserted
    for(Element widgetPathElement : getWidgetPathElements(layout)) {

      // widget layout is the root of widget layout definition
      // it is loaded recursively in depth-first order so that when a widget level is returned all its
      // descendants are guaranteed to be resolved
      CompoPath widgetPath = new CompoPath(project, layoutPath, operators.getOperand(widgetPathElement, Operator.WIDGET));

      // widget path element may have invocation parameters for widget layout customization
      // load parameters, if any, and on loadLayoutDocument passes them to source reader
      // source reader takes care to inject parameter values into widget layout
      layoutParameters.load(operators.getOperand(widgetPathElement, Operator.PARAM));
      operators.removeOperator(widgetPathElement, Operator.PARAM);
      Element widgetLayout = scanComponentsTree(widgetPath.getLayoutPath(), guardCount);

      // update widget path element attributes with values from the widget layout root
      // widget path element has precedence over widget layout attributes so that parent can control widget attributes
      mergeAttrs(widgetPathElement, widgetLayout.getAttrs());

      // remove all children from widget path element and insert the actual widget layout elements
      widgetPathElement.removeChildren();
      for(Element widgetChild : widgetLayout.getChildren()) {
        widgetPathElement.addChild(widgetChild);
      }

      operators.removeOperator(widgetPathElement, Operator.WIDGET);
    }

    // component layout is loaded with LayoutReader decorator that insert XML declaration and root element
    // this is needed because component layout file may have more HTML fragments and XML requires single root
    return layout.getRoot().getFirstChild();
  }

  /**
   * Load document layout resolving template(s), if the case and delegates resources processing, on the fly to
   * {@link #referenceHandler}. If present, template(s) are processed invoking this method recursively till entire
   * hierarchy is done.
   * <p>
   * Returns loaded layout document. Anyway, if template is used, returns template document instead - with this layout
   * content inserted in template editable area.
   * <p>
   * Document file is loaded using {@link LayoutReader} and {@link SourceReader} decorators. Layout reader insert XML
   * declaration and document root; component layout is child to injected document root. Source reader detects resource
   * references and invoke {@link #referenceHandler} that handle variables replacement and media files processing.
   * <p>
   * This method insert the related style file into {@link #styleFiles} list. By convention layout and style files have
   * the same name; anyway, style file is not mandatory. Also takes care to insert style file path in the proper order,
   * suitable for page header inclusion.
   * 
   * @param layoutPath component layout path,
   * @param guardCount circular reference guard.
   * @return layout document.
   */
  private Document loadLayoutDocument(FilePath layoutPath, int guardCount)
  {
    if(guardCount++ == 8) {
      throw new WoodException("Circular templates references suspicion. Too many nesting levels on |%s|. Please check 'template' attributes!", layoutPath);
    }

    Reader reader = new LayoutReader(new SourceReader(layoutPath, layoutParameters, referenceHandler));
    DocumentBuilder builder = new DocumentBuilderImpl();
    Document layout = project.hasNamespace() ? builder.loadXMLNS(reader) : builder.loadXML(reader);
    if(!layout.getRoot().hasChildren()) {
      throw new WoodException("Empty layout |%s|.", layoutPath);
    }

    // collect script classes declared into loaded layout fragment
    collectScriptClasses(layoutPath, layout.getRoot());

    // component layout may have related style file; collect if into this base component used styles list
    collectRelatedStyle(layoutPath);

    // content element is the root of content layout designed to replace an editable element from a template
    // a content element is an element with 'template' attribute
    // if this layout has no content elements it does not use templates and return it as it is

    // 'template' attribute contains the path to template named editable area
    EList contentElements = operators.findByOperator(layout, Operator.TEMPLATE);
    if(contentElements.isEmpty()) {
      return layout;
    }

    // current component describe content for a template because it has at least one reference to an editable element
    // an editable element is a template hole that need to be filled
    // since templates can have more than one editable element, editable path should identify both the template and
    // editable element: EDITABLE_PATH := TEMPLATE_PATH # EDITABLE_NAME

    // current component should define content for all editable elements from template
    // if not it become a template too and creates a hierarchy of templates similar to class inheritance

    // a content layout file may contain content elements for multiple editable elements from template
    // but all pointing to the same template layout file
    // uses first content element to acquire template layout path and load template layout document
    // also load layout parameters from content element data-param operator
    // layout parameters are used by layout source file reader to inject parameter values

    Document template = null;
    Editables editables = null;

    for(Element contentElement : contentElements) {
      EditablePath editablePath = new EditablePath(project, layoutPath, operators.getOperand(contentElement, Operator.TEMPLATE));

      if(template == null) {
        // prepare layout parameters, possible empty, before loading template from source file
        // subsequent loadLayoutDocument passes parameters to source reader
        // source reader takes care to inject parameter values into template layout
        layoutParameters.load(operators.getOperand(contentElement, Operator.PARAM));
        operators.removeOperator(contentElement, Operator.PARAM);

        template = loadLayoutDocument(editablePath.getLayoutPath(), guardCount);
        editables = new Editables(template);
      }

      operators.removeOperator(contentElement, Operator.TEMPLATE);
      Element editableElement = editables.get(editablePath.getEditableName());
      if(editableElement == null) {
        throw new WoodException("Invalid template source code. Missing editable |%s| requested from component |%s|.", editablePath, layoutPath);
      }

      // insert content element - and all its descendants into template document, before editable element
      // then merge newly inserted content and editable elements attributes, but content attributes takes precedence
      editableElement.insertBefore(contentElement);
      mergeAttrs(editableElement.getPreviousSibling(), editableElement.getAttrs());
    }

    editables.remove();
    return template;
  }

  /**
   * Return parent project to which this component belongs to.
   * 
   * @return parent project.
   * @see #project
   */
  public Project getProject()
  {
    return project;
  }

  /**
   * Return optional component descriptor.
   * 
   * @return component descriptor.
   * @throws BugError if component descriptor is missing.
   */
  public ComponentDescriptor getDescriptor()
  {
    if(descriptor == null) {
      throw new BugError("Attempt to retrieve missing missing component descriptor for |%s|.", name);
    }
    return descriptor;
  }

  /**
   * Get this component name.
   * 
   * @return component name.
   * @see #name
   */
  public String getName()
  {
    return name;
  }

  /**
   * Get this component display name.
   * 
   * @return component display.
   * @see #display
   */
  public String getDisplay()
  {
    return display;
  }

  /**
   * Get aggregated layout element.
   * 
   * @return this component layout HTML fragment.
   * @see #layout
   */
  public Element getLayout()
  {
    return layout;
  }

  /**
   * Get the name of the component layout file.
   * 
   * @return layout file name.
   */
  public String getLayoutFileName()
  {
    return baseLayoutPath.getName();
  }

  /**
   * Get component style files in the proper order for page document inclusion.
   * 
   * @return component style files.
   * @see #styleFiles
   */
  public List<FilePath> getStyleFiles()
  {
    return styleFiles;
  }

  /**
   * Get component script files in order proper for page document inclusion. Returned list contains this component
   * direct referenced files and all dependencies, less third party scripts - see {@link #thirdPartyScripts}.
   * 
   * @return component used script files.
   * @see #scriptFiles
   */
  public List<ScriptFile> getScriptFiles()
  {
    return scriptFiles;
  }

  /**
   * Return absolute URL list for third party scripts used by this component.
   * 
   * @return component third party scripts, in no particular order.
   * @see #thirdPartyScripts
   */
  public Set<String> getThirdPartyScripts()
  {
    return thirdPartyScripts;
  }

  /**
   * Get the file path for optional preview style. Preview style is used only by preview process and provides styles for
   * unit testing context.
   * <p>
   * Returned file path is optional, that is, designed file is not mandatory to exist. Is caller responsibility to
   * ensure file exist before using it.
   * 
   * @return component style preview file.
   */
  public FilePath getPreviewStyle()
  {
    return baseLayoutPath.getDirPath().getFilePath(CT.PREVIEW_STYLE);
  }

  /**
   * Get the file path for optional preview script. Preview script describe unit testing logic; it is used by preview
   * process to literally replace component script.
   * <p>
   * Returned file path is optional, that is, designed file is not mandatory to exist. Is caller responsibility to
   * ensure file exist before using it.
   * 
   * @return component script preview file.
   */
  public FilePath getPreviewScript()
  {
    return baseLayoutPath.getDirPath().getFilePath(CT.PREVIEW_SCRIPT);
  }

  /**
   * Return this component {@link #display}, that is used also as string representation.
   * 
   * @return component display.
   * @see #display
   */
  @Override
  public String toString()
  {
    return display;
  }

  // --------------------------------------------------------
  // Internal helper methods

  /**
   * Scan given layout fragment for script classes and add to component script classes set.
   * 
   * @param layoutFile layout source file, for error reporting,
   * @param layout layout fragment to scan for script classes.
   * @throws WoodException if layout contains script classes declaration for which project has not a definition file.
   * @see #scriptClasses
   */
  private void collectScriptClasses(FilePath layoutFile, Element layout)
  {
    Element pageClassEl = operators.getByOperator(layout, Operator.SCRIPT);
    if(pageClassEl != null) {
      addScriptClass(layoutFile, operators.getOperand(pageClassEl, Operator.SCRIPT));
    }

    // collect all script classes used for custom elements and formatting
    // first include custom classes then formatting
    // both class and formatting operators are declared by template engine and prefixed with data-

    for(Element scriptClassEl : layout.findByXPath("//*[@data-class]")) {
      addScriptClass(layoutFile, scriptClassEl.getAttr("data-class"));
    }
    for(Element scriptClassEl : layout.findByXPath("//*[@data-format]")) {
      addScriptClass(layoutFile, scriptClassEl.getAttr("data-format"));
    }
  }

  /**
   * Add script class to component script classes set, {@link #scriptClasses}. Given script class is collected from
   * layout source file created by developer. Throws exception if requested script class is not defined in a project
   * script file; a reason may be class name misspelling.
   * 
   * @param layoutFile layout file declaring script class, for error reporting,
   * @param scriptClass qualified script class name.
   * @throws WoodException if script class definition file is not found.
   */
  private void addScriptClass(FilePath layoutFile, String scriptClass)
  {
    if(scriptClass == null) {
      throw new WoodException("Empty script reference on |%s|. Please check wood:script, data-class and data-format attributes.", layoutFile);
    }
    if(!project.scriptFileExists(scriptClass)) {
      throw new WoodException("Broken script reference. No script file found for class |%s| requested from |%s|.", scriptClass, layoutFile);
    }
    scriptClasses.add(scriptClass);
  }

  /**
   * Collect style file related to given source file, be it layout or script file. A component layout or script file may
   * have a related style. A related style file have the same base name as source file but with style extension. This
   * method uses {@link FilePath#cloneToStyle()} to get related style file.
   * <p>
   * If related style file exists add it to this component used style files list, see {@link #styleFiles}. Takes care to
   * keep styles proper order for page document and preview inclusion.
   * 
   * @param sourceFile source file.
   */
  private void collectRelatedStyle(FilePath sourceFile)
  {
    // by convention component, component layout, script file and style sheet share the same name
    // for example, component res/path/compo has res/path/compo/compo.htm layout, res/path/compo/compo.js script and
    // res/path/compo/compo.css style

    FilePath stylePath = sourceFile.cloneToStyle();
    if(stylePath.exists() && !styleFiles.contains(stylePath)) {
      // component style files are linked into build and preview document header
      // in the order from this component styles list, first style file on top

      // we need to ensure templates, widgets and scripts related style files are included before component base style
      // for this reason we insert discovered styles at the styles list beginning
      styleFiles.add(0, stylePath);
    }
  }

  /** Constant for empty element array. */
  private static final Element[] EMPTY_ARRAY = new Element[0];

  /**
   * An widget path is a reference to an external component; this method returns defined widget paths. Returns a newly
   * created array with all widget path elements found in given layout document. This method creates a new array because
   * {@link EList} is live and we need to remove widget paths while iterating. Returns empty array if no widget found.
   * <p>
   * Widget path element is identified by attribute with name <code>wood:widget</code>.
   * 
   * @param layout layout document.
   * @return widgets array possible empty.
   */
  private Element[] getWidgetPathElements(Document layout)
  {
    EList elist = operators.findByOperator(layout, Operator.WIDGET);
    if(elist.isEmpty()) {
      return EMPTY_ARRAY;
    }
    Element[] array = new Element[elist.size()];
    for(int i = 0; i < elist.size(); ++i) {
      array[i] = elist.item(i);
    }
    return array;
  }

  /**
   * Add attributes to element but do not overwrite existing ones. If attribute to add is CSS class add newly class
   * values but also takes care to not overwrite existing ones.
   * 
   * @param element document element,
   * @param attrs attributes list to add.
   */
  private static void mergeAttrs(Element element, Iterable<Attr> attrs)
  {
    for(Attr attr : attrs) {
      if(attr.getName().equals("class")) {
        // merge attributes and element CSS classes but do not overwrite element classes
        Set<String> classes = new HashSet<String>();
        classes.addAll(Strings.split(attr.getValue(), ' '));
        String elementClass = element.getAttr("class");
        if(elementClass != null) {
          classes.addAll(Strings.split(elementClass, ' '));
        }
        element.setAttr("class", Strings.join(classes, ' '));
        continue;
      }
      if(!element.hasAttr(attr.getName())) {
        element.setAttr(attr.getName(), attr.getValue());
      }
    }
  }

  /**
   * Create the list of this component script files in dependencies order. Uses given script classes to retrieve script
   * files declaring them, see {@link Project#getScriptFiles(Collection)}. Add obtained script files and their
   * dependencies using {@link #updateScriptFiles(List, ScriptFile)} recursively. On scanning process updates third
   * party scripts list, see {@link #thirdPartyScripts}.
   * <p>
   * Returned list scripts are in the proper order for inclusion into {@link PageDocument}.
   * 
   * @param collection script classes discovered by this component, in no particular order.
   * @return the list of script files, in dependencies order.
   */
  private List<ScriptFile> collectScriptFiles(Collection<ScriptFile> componentScriptFiles)
  {
    List<ScriptFile> scriptFiles = new Vector<ScriptFile>();
    for(ScriptFile scriptFile : componentScriptFiles) {
      updateScriptFiles(scriptFiles, scriptFile);
    }
    return scriptFiles;
  }

  /**
   * Add source script and its dependencies to target scripts and update {@link #thirdPartyScripts}. Target scripts
   * order matters; it should be the proper order to include into {@link PageDocument}. To ensure inclusion order, this
   * method uses next heuristic:
   * <ul>
   * <li>first add recursively source script strong dependencies,
   * <li>add source script, if not already included,
   * <li>lastly add recursively weak dependencies.
   * </ul>
   * <p>
   * This logic ensure all strong dependencies tree is included before source script; also all weak dependencies tree is
   * included after. This is true for every recursive iteration so that a strong dependency and all its next level
   * dependencies are included before source script. Also this logic ensure a script is included only once.
   * 
   * @param targetScripts target script files,
   * @param sourceScript source script file to scan for dependencies.
   */
  private void updateScriptFiles(List<ScriptFile> targetScripts, ScriptFile sourceScript)
  {
    // a script file may denote a scripted widget and may have a related style file
    // a scripted widget is a component without layout, that is, a component providing behavior and style but with
    // layout defined by parent component
    collectRelatedStyle(sourceScript.getSourceFile());

    for(String thirdPartyDependency : sourceScript.getThirdPartyDependencies()) {
      if(!thirdPartyScripts.contains(thirdPartyDependency)) {
        thirdPartyScripts.add(thirdPartyDependency);
      }
    }

    for(ScriptFile dependency : sourceScript.getStrongDependencies()) {
      updateScriptFiles(targetScripts, dependency);
    }
    if(!targetScripts.contains(sourceScript)) {
      targetScripts.add(sourceScript);
    }
    for(ScriptFile dependency : sourceScript.getWeakDependencies()) {
      updateScriptFiles(targetScripts, dependency);
    }
  }

  // ------------------------------------------------------
  // Internal classes

  /**
   * Template editable elements. This class supplies method to retrieve editable element by name and remove all template
   * editable elements, after template processing complete.
   * 
   * @author Iulian Rotaru
   */
  private class Editables
  {
    /** Template document. */
    private Document template;

    /** Template editables. */
    private Map<String, Element> editables = new HashMap<String, Element>();

    /**
     * Create editables instance for given template layout.
     * 
     * @param template template layout document.
     */
    public Editables(Document template)
    {
      this.template = template;
    }

    /**
     * Return editable element identified by given name or null if not found.
     * 
     * @param editableName editable name.
     * @return editable element or null.
     */
    public Element get(String editableName)
    {
      Element editable = editables.get(editableName);
      if(editable == null) {
        editable = operators.getByOperator(template, Operator.EDITABLE, editableName);
        editables.put(editableName, editable);
      }
      return editable;
    }

    /**
     * Remove all this template editable elements. This method is invoked after template processing complete.
     */
    public void remove()
    {
      for(Element editable : editables.values()) {
        editable.remove();
      }
    }
  }
}
