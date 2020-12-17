package js.tools.commons.dep;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import js.tools.commons.ast.AstHandler;
import js.tools.commons.ast.Names;
import js.tools.commons.ast.Scanner;
import js.tools.commons.util.Files;
import js.tools.commons.util.Utils;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ConditionalExpression;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.NewExpression;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;

/**
 * Dependencies scanner for j(s)-script sources. A j(s)-script is a standard ECMA script forced to OOP by semantic
 * conventions. Note that j(s)-script syntax is fully compatible with ECMA Script but applies some constrains on
 * semantic.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public class DependencyScanner
{
  /** Aliases for script classes. Map alias to class qualified name. */
  private static final Map<String, String> CLASS_ALIAS = new HashMap<String, String>();
  static {
    CLASS_ALIAS.put("$package", "js.lang.Operator");
    CLASS_ALIAS.put("$declare", "js.lang.Operator");
    CLASS_ALIAS.put("LogFactory", "js.lang.Log");
    CLASS_ALIAS.put("WinMain", "js.ua.Window");
  }

  /** Script AST scanner. */
  private Scanner scanner;

  /** List of class names that will not be considered as dependencies. */
  private List<String> excludes;

  /** Script discovered dependencies, in no particular order. */
  private Collection<Dependency> dependencies = new HashSet<>();

  /**
   * Scan j(s)-script source file for dependencies and return the found ones, in no particular order. This method just
   * delegates {@link #getDependencies(File, String...)}.
   * 
   * @param jsScriptFile j(s)-script source file,
   * @param excludes set of class names to exclude.
   * @return discovered dependencies.
   * @throws FileNotFoundException if script file is missing.
   */
  public Collection<Dependency> getDependencies(File jsScriptFile, Set<String> excludes) throws FileNotFoundException
  {
    return getDependencies(jsScriptFile, excludes.toArray(new String[0]));
  }

  /**
   * Scan j(s)-script file for dependencies and return those found, in no particular order.
   * 
   * @param jsScriptFile j(s)-script file,
   * @param excludes variable number of j(s)-script classes to exclude.
   * @return discovered dependencies, in no particular order.
   * @throws FileNotFoundException if j(s)-script file is missing.
   */
  public Collection<Dependency> getDependencies(File jsScriptFile, String... excludes) throws FileNotFoundException
  {
    this.excludes = Arrays.asList(excludes);

    scanner = new Scanner();

    scanner.bind(Assignment.class, new AssignmentHandler());
    scanner.bind(VariableDeclaration.class, new VariableDeclarationHandler());
    scanner.bind(ObjectProperty.class, new ObjectPropertyHandler());
    scanner.bind(FunctionCall.class, new FunctionCallHandler());
    scanner.bind(NewExpression.class, new NewExpressionHandler());
    scanner.bind(InfixExpression.class, new InfixExpressionHandler());
    scanner.bind(IfStatement.class, new IfStatementHandler());
    scanner.bind(ReturnStatement.class, new ReturnStatementHandler());

    beforeParsing(scanner);

    Reader reader = null;
    try {
      scanner.parse(new FileReader(jsScriptFile), jsScriptFile.getName());
    }
    finally {
      Files.close(reader);
    }
    return dependencies;
  }

  /**
   * Hook invoked just before parsing. Allow subclass to bind its own specific AST handlers using
   * {@link Scanner#bind(Class, AstHandler)}. It is called from {@link #getDependencies(File, String...)}.
   * 
   * @param scanner script AST scanner.
   */
  protected void beforeParsing(Scanner scanner)
  {
  }

  /**
   * Get script source for parsed file. This method returns meaningful value only if called after script parse, see
   * {@link #getDependencies(File, String...)}.
   * 
   * @return script source.
   */
  public String toSource()
  {
    return scanner.toSource();
  }

  /**
   * Add given script class name to dependencies list, if not already present. Dependency type is determined by
   * enclosing function scope where dependency is declared: if no enclosing function - <code>enclosingFunction</code>
   * argument is null, declaring scope is global and dependency is strong. Otherwise is weak.
   * <p>
   * If {@link #excludes} list contains given script class name this method does nothing.
   * 
   * @param jsClassName dependency script class name,
   * @param enclosingFunction enclosing function scope where class name is declared, possible null.
   */
  private void addJsClassDependency(String jsClassName, FunctionNode enclosingFunction)
  {
    if(excludes.contains(jsClassName)) {
      return;
    }

    // a dependency is weak if discovered in a function scope, that is, is not executed at hosting script load
    // if a dependency is discovered in global scope it is strong since should be executed at script load
    Dependency.Type type = enclosingFunction != null ? Dependency.Type.WEAK : Dependency.Type.STRONG;

    // if dependency to add is strong and is happen to be already added force its type
    if(type == Dependency.Type.STRONG) {
      Dependency dependency = getDependencyByName(jsClassName);
      if(dependency != null) {
        dependency.setType(type);
        return;
      }
    }

    dependencies.add(new JsClassDependency(jsClassName, type));
  }

  private Dependency getDependencyByName(String name)
  {
    for(Dependency dependency : dependencies) {
      if(dependency.getName().equals(name)) {
        return dependency;
      }
    }
    return null;
  }

  /**
   * Extract the class name and enclosing function from given AST node, respective parent node then delegates
   * {@link #addJsClassDependency(String, FunctionNode)}.
   * 
   * @param parent parent node for enclosing function,
   * @param node current processing node containing script class name.
   */
  private void addDependency(AstNode parent, AstNode node)
  {
    String name = Names.getName(node);
    if(Utils.isDependencyName(name)) {
      addJsClassDependency(Utils.getDependencyQualifiedClassName(name), parent.getEnclosingFunction());
    }
  }

  /**
   * Add script dependency declared by <code>include</code> pseudo-operator.
   * 
   * @param jsClassName dependency class name.
   */
  private void addDependency(String jsClassName)
  {
    dependencies.add(new ThirdPartyDependency(jsClassName));
  }

  // ------------------------------------------------------
  // Handlers for AST scanner

  final class AssignmentHandler extends AstHandler
  {
    @Override
    public void handle(Node node)
    {
      Assignment assignment = (Assignment)node;
      if(assignment.getEnclosingFunction() == null) {
        return;
      }
      addDependency(assignment, assignment.getLeft());
      addDependency(assignment, assignment.getRight());
    }
  }

  final class VariableDeclarationHandler extends AstHandler
  {
    @Override
    public void handle(Node node)
    {
      VariableDeclaration variableDeclaration = (VariableDeclaration)node;
      for(VariableInitializer variableInitializer : variableDeclaration.getVariables()) {
        AstNode initializer = variableInitializer.getInitializer();
        if(initializer != null && initializer.getType() == Token.GETPROP) {
          addDependency(variableDeclaration, initializer);
        }
      }
    }
  }

  final class ObjectPropertyHandler extends AstHandler
  {
    @Override
    public void handle(Node node)
    {
      ObjectProperty property = (ObjectProperty)node;
      AstNode right = property.getRight();
      if(right.getType() == Token.GETPROP) {
        addDependency(property, right);
      }
    }
  }

  final class InfixExpressionHandler extends AstHandler
  {
    @Override
    public void handle(Node node)
    {
      InfixExpression expression = (InfixExpression)node;

      AstNode left = expression.getLeft();
      if(left.getType() == Token.GETPROP) {
        addDependency(expression, left);
      }

      AstNode right = expression.getRight();
      if(right.getType() == Token.GETPROP) {
        addDependency(expression, right);
      }
    }
  }

  private static final String PACKAGE = "$package";
  private static final String INCLUDE = "$include";
  private static final String DECLARE = "$declare";
  private static final String EXTENDS = "$extends";
  private static final String INIT = "$init";
  private static final String GET_LOGGER = "LogFactory.getLogger";
  private static final String LOG_FACTORY = "LogFactory";
  private static final String WIN_MAIN = "WinMain";

  final class FunctionCallHandler extends AstHandler
  {
    @Override
    public void handle(Node node)
    {
      FunctionCall functionCall = (FunctionCall)node;
      AstNode target = functionCall.getTarget();
      String functionName = Names.getName(target);
      List<AstNode> arguments = functionCall.getArguments();

      if(DECLARE.equals(functionName)) {
        addJsClassDependency(CLASS_ALIAS.get(DECLARE), functionCall.getEnclosingFunction());
        return;
      }

      if(PACKAGE.equals(functionName)) {
        addJsClassDependency(CLASS_ALIAS.get(PACKAGE), functionCall.getEnclosingFunction());
        return;
      }

      if(INCLUDE.equals(functionName)) {
        String includeName = Names.getName(arguments.get(0));
        if(Utils.isDependencyName(includeName)) {
          addJsClassDependency(Utils.getDependencyQualifiedClassName(includeName), functionCall.getEnclosingFunction());
        }
        else {
          addDependency(includeName);
        }
        return;
      }

      if(EXTENDS.equals(functionName)) {
        addDependency(functionCall, arguments.get(1));
        return;
      }

      if(INIT.equals(functionName)) {
        return;
      }

      if(GET_LOGGER.equals(functionName)) {
        addJsClassDependency(CLASS_ALIAS.get(LOG_FACTORY), functionCall.getEnclosingFunction());
        return;
      }

      if(functionName.startsWith(WIN_MAIN)) {
        // WinMain.on(...
        addJsClassDependency(CLASS_ALIAS.get(WIN_MAIN), functionCall.getEnclosingFunction());
        return;
      }
      
      if(Utils.isDependencyName(functionName)) {
        // here we have two variants:
        // 1. static method invocation: js.lang.LogFactory.getLogger(...);
        // 2. constructor as a function: js.util.Timer(...)
        addJsClassDependency(Utils.getDependencyQualifiedClassName(functionName), functionCall.getEnclosingFunction());
      }

      for(AstNode argument : arguments) {
        addDependency(functionCall, argument);
      }
    }
  }

  final class NewExpressionHandler extends AstHandler
  {
    @Override
    public void handle(Node node)
    {
      NewExpression newExpression = (NewExpression)node;
      addDependency(newExpression, newExpression.getTarget());
    }
  }

  final class IfStatementHandler extends AstHandler
  {
    @Override
    public void handle(Node node)
    {
      IfStatement ifStatement = (IfStatement)node;
      AstNode condition = ifStatement.getCondition();
      if(condition.getType() == Token.GETPROP) {
        addDependency(ifStatement, condition);
      }
    }
  }

  public class ReturnStatementHandler extends AstHandler
  {
    @Override
    public void handle(Node node)
    {
      ReturnStatement returnStatement = (ReturnStatement)node;
      AstNode returnValue = returnStatement.getReturnValue();
      if(returnValue == null) { // null if void
        return;
      }

      if(returnValue.getType() == Token.HOOK) {
        ConditionalExpression conditionalExpression = (ConditionalExpression)returnValue;

        AstNode testExpression = conditionalExpression.getTestExpression();
        if(testExpression.getType() == Token.GETPROP) {
          addDependency(returnStatement, testExpression);
        }

        AstNode trueExpression = conditionalExpression.getTrueExpression();
        if(trueExpression.getType() == Token.GETPROP) {
          addDependency(returnStatement, trueExpression);
        }

        AstNode falseExpression = conditionalExpression.getFalseExpression();
        if(falseExpression.getType() == Token.GETPROP) {
          addDependency(returnValue, falseExpression);
        }
      }
    }
  }
}
