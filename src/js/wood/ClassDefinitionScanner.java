package js.wood;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import js.tools.commons.ast.AstHandler;
import js.tools.commons.ast.Names;
import js.tools.commons.ast.Scanner;
import js.util.Files;
import js.util.Strings;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;

/**
 * Script scanner for class definitions. This utility class discovers script classes defined by a script file or
 * declared as part of third party API. Note that only qualified class names are considered. Uses
 * {@link AssignmentHandler} for classes defined by given script file and {@link FunctionCallHandler} for declared third
 * party classes.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class ClassDefinitionScanner
{
  /**
   * Get classes defined by script file or declared as third party classes.
   * 
   * @param scriptFile script file to scan.
   * @return set of declared classes, in no particular order.
   */
  public static Set<String> getClasses(File scriptFile)
  {
    Scanner scanner = new Scanner();

    Set<String> classes = new HashSet<String>();
    scanner.bind(Assignment.class, new AssignmentHandler(classes));
    scanner.bind(FunctionCall.class, new FunctionCallHandler(classes));

    Reader reader = null;
    try {
      reader = new FileReader(scriptFile);
      scanner.parse(reader, scriptFile.getName());
    }
    catch(FileNotFoundException e) {
      throw new WoodException("Missing script file |%s|.", scriptFile);
    }
    finally {
      Files.close(reader);
    }
    return classes;
  }

  /**
   * AST scanner handler for classes declared by scanned script file. This handler consider that a class is defined when
   * assigning something to a qualified name. Assignment right part is not limited to function and object; in fact right
   * part type is not considered at all. Anyway, only assignment in global scope is allowed. It is bad practice to
   * create global variables from local scope.
   * <p>
   * Note that nested classes are allowed but not qualified names are ignored.
   * <p>
   * In sample below <code>test.wood.GeoMap</code> and <code>test.wood.Index</code> are valid class definitions whereas
   * <code>test.wood.Marker</code> is ignored, even if syntactically correct, because is not in global scope.
   * 
   * <pre>
   * test.wood.GeoMap = function() {
   *     test.wood.Marker = {};
   * };
   * 
   * test.wood.Index = 0;
   * </pre>
   * 
   * @author Iulian Rotaru
   * @since 1.0
   */
  private static class AssignmentHandler extends AstHandler
  {
    /** Reference to defined classes collector. */
    private Set<String> classes;

    /**
     * Construct handler instance using given defined class collector.
     * 
     * @param classes defined classes collector.
     */
    public AssignmentHandler(Set<String> classes)
    {
      this.classes = classes;
    }

    /**
     * Extract the name of left part from assignment node and, if qualified, add it to {@link #classes}.
     */
    @Override
    public void handle(Node node)
    {
      Assignment assignment = (Assignment)node;
      if(assignment.getEnclosingFunction() == null) {
        String name = Names.getName(assignment.getLeft());
        if(Strings.isQualifiedClassName(name)) {
          classes.add(name);
        }
      }
    }
  }

  /**
   * AST scanner handler used to discover third party classes. This handler scan for <code>$declare</code> script
   * pseudo-operator, used to declare third party script classes.
   * <p>
   * Sample code below uses <code>$declare</code> pseudo-operator to declare classed defined by Google Maps API, then
   * include script URL. Note that script URL is processed by dependencies scanner.
   * 
   * <pre>
   * $declare(&quot;google.maps.LatLngBounds&quot;, false);
   * $declare(&quot;google.maps.MapTypeId&quot;, false);
   * $declare(&quot;google.maps.LatLng&quot;, false);
   * $declare(&quot;google.maps.Map&quot;, false);
   * $declare(&quot;google.maps.Marker&quot;, false);
   * 
   * $include(&quot;http://maps.google.com/maps/api/js?sensor=false&quot;);
   * </pre>
   * 
   * @author Iulian Rotaru
   * @since 1.0
   */
  private static class FunctionCallHandler extends AstHandler
  {
    /** Constant for <code>declare</code> script pseudo-operator, used to declare third party scripts. */
    private static final String DECLARE = "$declare";

    /** Reference to defined classes collector. */
    private Set<String> classes;

    /**
     * Construct handler instance using given parameter to collect defined classes.
     * 
     * @param classes defined classes collector.
     */
    public FunctionCallHandler(Set<String> classes)
    {
      this.classes = classes;
    }

    /**
     * Extract the name of the first argument of function call node and, if qualified add, it to {@link #classes}.
     */
    @Override
    public void handle(Node node)
    {
      FunctionCall functionCall = (FunctionCall)node;
      AstNode target = functionCall.getTarget();
      String functionName = Names.getName(target);
      List<AstNode> arguments = functionCall.getArguments();

      if(DECLARE.equals(functionName)) {
        String name = Names.getName(arguments.get(0));
        if(Strings.isQualifiedClassName(name)) {
          classes.add(name);
        }
      }
    }
  }
}
