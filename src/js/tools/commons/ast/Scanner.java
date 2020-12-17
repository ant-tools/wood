package js.tools.commons.ast;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.BreakStatement;
import org.mozilla.javascript.ast.CatchClause;
import org.mozilla.javascript.ast.ConditionalExpression;
import org.mozilla.javascript.ast.ContinueStatement;
import org.mozilla.javascript.ast.DoLoop;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.EmptyExpression;
import org.mozilla.javascript.ast.EmptyStatement;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.ForInLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.LabeledStatement;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NewExpression;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.RegExpLiteral;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.SwitchCase;
import org.mozilla.javascript.ast.SwitchStatement;
import org.mozilla.javascript.ast.ThrowStatement;
import org.mozilla.javascript.ast.TryStatement;
import org.mozilla.javascript.ast.UnaryExpression;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;
import org.mozilla.javascript.ast.WhileLoop;

/**
 * Parse j(s)-script source into a Rhino abstract tree then traverse all AST nodes enacting registered handlers on the
 * fly.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class Scanner
{
  /** Scanner logger. */
  private final Log log;
  /** User defined AST node handlers bound to specific Rhino node class. */
  private Map<Class<? extends Node>, AstHandler> handlers = new HashMap<Class<? extends Node>, AstHandler>();
  /** The root of Rhino nodes tree, valid only after {@link #parse(Reader, String)} was executed. */
  private AstRoot root;
  private boolean debug;
  private boolean verbose;

  /** Create scanner instance with default logger. */
  public Scanner()
  {
    this.log = new Log();
  }

  /**
   * Create scanner instance with custom logger.
   * 
   * @param log custom scanner logger.
   */
  public Scanner(Log log)
  {
    this.log = log;
  }

  /**
   * Get scanner logger reference.
   * 
   * @return scanner logger.
   */
  public Log getLog()
  {
    return log;
  }

  /**
   * Bind user defined AST node handler to specified Rhino node class.
   * 
   * @param nodeClass Rhino node class,
   * @param handler user defined AST node handler.
   */
  public void bind(Class<? extends Node> nodeClass, AstHandler handler)
  {
    handler.setLog(log);
    handlers.put(nodeClass, handler);
  }

  /**
   * Get registered custom AST node handler for requested Rhino node class. Returned value can be null if there is no
   * custom AST node handler registered for requested Rhino node class.
   * 
   * @param nodeClass Rhino node class.
   * @return custom AST node handler, possible null.
   */
  public AstHandler getHandler(Class<? extends Node> nodeClass)
  {
    return handlers.get(nodeClass);
  }

  /**
   * Get the root of parsed Rhino nodes tree.
   * 
   * @return parsed nodes tree root.
   */
  public AstRoot getRoot()
  {
    return root;
  }

  /**
   * Get formatted j(s)-script source.
   * 
   * @return script source.
   */
  public String toSource()
  {
    return root.toSource();
  }

  /**
   * Enable or disable debug messages output.
   * 
   * @param debug debug log enabled state.
   */
  public void setDebug(boolean debug)
  {
    this.debug = debug;
  }

  /**
   * Enable or disable verbose debugging.
   * 
   * @param verbose verbose debugging flag.
   */
  public void setVerbose(boolean verbose)
  {
    this.verbose = verbose;
  }

  /**
   * Parse j(s)-script source using Rhino parser then execute {@link #scan(Node, int)} on parsed tree root.
   * 
   * @param reader j(s)-script source reader,
   * @param source source name for logging.
   */
  public void parse(Reader reader, String source)
  {
    log.setCurrentSource(source);
    if(verbose) {
      log.println("Scan j(s)-script source file: " + source);
    }

    ContextFactory contextFactory = ContextFactory.getGlobal();
    Context context = contextFactory.enterContext();
    Context.enter();

    try {
      CompilerEnvirons env = new CompilerEnvirons();
      env.setLanguageVersion(Context.VERSION_1_8);
      env.setRecordingComments(true);
      env.setRecordingLocalJsDocComments(true);
      env.initFromContext(context);
      Parser parser = new Parser(env, env.getErrorReporter());

      root = parser.parse(reader, source, 1);
      scan(root, 0);
    }
    catch(IOException e) {
      Context.exit();
      e.printStackTrace();
    }
  }

  /**
   * Scan recursively Rhino AST nodes tree and execute custom AST node handlers, if any registered for currently
   * processing node.
   * 
   * @param node currently processing Rhino AST node,
   * @param level nesting level used for logging.
   */
  private void scan(Node node, int level)
  {
    if(node == null) {
      return;
    }
    debug(level, node);

    AstHandler handler = handlers.get(node.getClass());
    if(handler != null) {
      log.setCurrentNode(node);
      handler.handle(node);
    }
    ++level;

    if(node.getClass() == ObjectLiteral.class) {
      ObjectLiteral objectLiteral = (ObjectLiteral)node;
      for(ObjectProperty property : objectLiteral.getElements()) {
        scan(property, level);
      }
    }
    else if(node.getClass() == ObjectProperty.class) {
      ObjectProperty objectProperty = (ObjectProperty)node;
      scan(objectProperty.getLeft(), level);
      scan(objectProperty.getRight(), level);
    }
    else if(node.getClass() == ArrayLiteral.class) {
      ArrayLiteral arrayLiteral = (ArrayLiteral)node;
      for(AstNode element : arrayLiteral.getElements()) {
        scan(element, level);
      }
    }
    else if(node.getClass() == FunctionNode.class) {
      FunctionNode functionNode = (FunctionNode)node;
      scan(functionNode.getBody(), level);
    }
    else if(node.getClass() == FunctionCall.class) {
      FunctionCall functionCall = (FunctionCall)node;
      scan(functionCall.getTarget(), level);
      for(AstNode argument : functionCall.getArguments()) {
        scan(argument, level);
      }
    }
    else if(node.getClass() == NewExpression.class) {
      NewExpression newExpression = (NewExpression)node;
      scan(newExpression.getTarget(), level);
      for(AstNode argument : newExpression.getArguments()) {
        scan(argument, level);
      }
    }
    else if(node.getClass() == ExpressionStatement.class) {
      ExpressionStatement statement = (ExpressionStatement)node;
      scan(statement.getExpression(), level);
    }
    else if(node.getClass() == Assignment.class) {
      Assignment assignment = (Assignment)node;
      scan(assignment.getLeft(), level);
      scan(assignment.getRight(), level);
    }
    else if(node.getClass() == IfStatement.class) {
      IfStatement ifStatement = (IfStatement)node;
      scan(ifStatement.getCondition(), level);
      scan(ifStatement.getThenPart(), level);
      scan(ifStatement.getElsePart(), level);
    }
    else if(node.getClass() == ReturnStatement.class) {
      ReturnStatement returnStatement = (ReturnStatement)node;
      scan(returnStatement.getReturnValue(), level);
    }
    else if(node.getClass() == PropertyGet.class) {
      PropertyGet propertyGet = (PropertyGet)node;
      scan(propertyGet.getProperty(), level);
      scan(propertyGet.getTarget(), level);
    }
    else if(node.getClass() == ElementGet.class) {
      ElementGet elementGet = (ElementGet)node;
      scan(elementGet.getTarget(), level);
      scan(elementGet.getElement(), level);
    }
    else if(node.getClass() == Name.class) {
      Name name = (Name)node;
      name.getIdentifier();
    }
    else if(node.getClass() == KeywordLiteral.class) {
      @SuppressWarnings("unused")
      KeywordLiteral thisLiteral = (KeywordLiteral)node;
    }
    else if(node.getClass() == VariableDeclaration.class) {
      VariableDeclaration variableDeclaration = (VariableDeclaration)node;
      for(VariableInitializer variableInitializer : variableDeclaration.getVariables()) {
        scan(variableInitializer, level);
      }
    }
    else if(node.getClass() == VariableInitializer.class) {
      VariableInitializer variableInitializer = (VariableInitializer)node;
      scan(variableInitializer.getTarget(), level);
      scan(variableInitializer.getInitializer(), level);
    }
    else if(node.getClass() == ForLoop.class) {
      ForLoop forLoop = (ForLoop)node;
      scan(forLoop.getInitializer(), level);
      scan(forLoop.getCondition(), level);
      scan(forLoop.getIncrement(), level);
      scan(forLoop.getBody(), level);
    }
    else if(node.getClass() == ForInLoop.class) {
      ForInLoop forInLoop = (ForInLoop)node;
      scan(forInLoop.getIteratedObject(), level);
      scan(forInLoop.getIterator(), level);
    }
    else if(node.getClass() == WhileLoop.class) {
      WhileLoop whileLoop = (WhileLoop)node;
      scan(whileLoop.getCondition(), level);
      scan(whileLoop.getBody(), level);
    }
    else if(node.getClass() == DoLoop.class) {
      DoLoop doLoop = (DoLoop)node;
      scan(doLoop.getBody(), level);
      scan(doLoop.getCondition(), level);
    }
    else if(node.getClass() == SwitchStatement.class) {
      SwitchStatement switchStatement = (SwitchStatement)node;
      scan(switchStatement.getExpression(), level);
      for(SwitchCase switchCase : switchStatement.getCases()) {
        scan(switchCase, level);
      }
    }
    else if(node.getClass() == SwitchCase.class) {
      SwitchCase switchCase = (SwitchCase)node;
      scan(switchCase.getExpression(), level);
      List<AstNode> statements = switchCase.getStatements();
      if(statements != null) {
        for(AstNode statement : statements) {
          scan(statement, level);
        }
      }
    }
    else if(node.getClass() == BreakStatement.class) {
      BreakStatement breakStatement = (BreakStatement)node;
      scan(breakStatement.getBreakLabel(), level);
      // next line will stack overflow
      // scan(breakStatement.getBreakTarget(), level);
    }
    else if(node.getClass() == ContinueStatement.class) {
      @SuppressWarnings("unused")
      ContinueStatement continueStatement = (ContinueStatement)node;
      // next line will stack overflow
      // scan(continueStatement.getTarget(), level);
    }
    else if(node.getClass() == ParenthesizedExpression.class) {
      ParenthesizedExpression parenthesizedExpression = (ParenthesizedExpression)node;
      scan(parenthesizedExpression.getExpression(), level);
    }
    else if(node.getClass() == StringLiteral.class) {
      StringLiteral stringLiterar = (StringLiteral)node;
      stringLiterar.getValue();
    }
    else if(node.getClass() == NumberLiteral.class) {
      NumberLiteral numberLiteral = (NumberLiteral)node;
      numberLiteral.getValue();
    }
    else if(node.getClass() == RegExpLiteral.class) {
      @SuppressWarnings("unused")
      RegExpLiteral regExpLiteral = (RegExpLiteral)node;
    }
    else if(node.getClass() == ConditionalExpression.class) {
      ConditionalExpression conditionalExpression = (ConditionalExpression)node;
      scan(conditionalExpression.getTestExpression(), level);
      scan(conditionalExpression.getTrueExpression(), level);
      scan(conditionalExpression.getFalseExpression(), level);
    }
    else if(node.getClass() == InfixExpression.class) {
      InfixExpression infixExpression = (InfixExpression)node;
      scan(infixExpression.getLeft(), level);
      scan(infixExpression.getRight(), level);
    }
    else if(node.getClass() == UnaryExpression.class) {
      UnaryExpression unaryExpression = (UnaryExpression)node;
      scan(unaryExpression.getOperand(), level);
    }
    else if(node.getClass() == EmptyExpression.class) {
      @SuppressWarnings("unused")
      EmptyExpression emptyExpression = (EmptyExpression)node;
    }
    else if(node.getClass() == EmptyStatement.class) {
      @SuppressWarnings("unused")
      EmptyStatement emptyExpression = (EmptyStatement)node;
    }
    else if(node.getClass() == LabeledStatement.class) {
      LabeledStatement labeledStatement = (LabeledStatement)node;
      scan(labeledStatement.getStatement(), level);
    }
    else if(node.getClass() == Block.class) {
      Block block = (Block)node;
      for(Node statement : block) {
        scan(statement, level);
      }
    }
    else if(node.getClass() == Scope.class) {
      Scope scope = (Scope)node;
      for(Node statement : scope) {
        scan(statement, level);
      }
    }
    else if(node.getClass() == ScriptNode.class) {
      for(Node child : node) {
        scan(child, level);
      }
    }
    else if(node.getClass() == AstRoot.class) {
      for(Node child : node) {
        scan(child, level);
      }
    }
    else if(node.getClass() == TryStatement.class) {
      TryStatement tryStatement = (TryStatement)node;
      scan(tryStatement.getTryBlock(), level);
      for(CatchClause catchClause : tryStatement.getCatchClauses()) {
        scan(catchClause, level);
      }
      scan(tryStatement.getFinallyBlock(), level);
    }
    else if(node.getClass() == CatchClause.class) {
      CatchClause catchClause = (CatchClause)node;
      scan(catchClause.getCatchCondition(), level);
      scan(catchClause.getBody(), level);
    }
    else if(node.getClass() == ThrowStatement.class) {
      ThrowStatement throwStatement = (ThrowStatement)node;
      scan(throwStatement.getExpression(), level);
    }
    else {
      throw new IllegalStateException("Unprocessed node: " + node);
    }
  }

  /**
   * Print debug message about currently processing AST node.
   * 
   * @param level node nesting level into abstract tree,
   * @param node current processing node.
   */
  private void debug(int level, Node node)
  {
    if(debug) {
      while(--level > 0) {
        log.print('-');
      }
      log.println(Token.typeToName(node.getType()));
    }
  }
}
