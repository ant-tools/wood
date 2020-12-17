package js.wood;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import js.core.Factory;
import js.json.Json;
import js.json.impl.JsonParserException;
import js.util.Classes;
import js.util.Files;
import js.util.Strings;
import js.util.Types;

/**
 * Handler for remote method invocations occurred from preview scripts.
 * 
 * @author Iulian Rotaru
 */
public class RmiRequestHandler
{
  public static boolean accept(String requestPath)
  {
    return requestPath.endsWith(".rmi");
  }

  private String className;
  private String methodName;

  protected RmiRequestHandler(Project project, String requestPath)
  {
    // assume we are into preview for component '/res/compos/dialogs/alert'
    // current loaded content is the last part of the path, i.e. 'alert'
    // from a component script there is a RMI request for sixqs.site.controller.MainController#getCategoriesSelect

    // in this case browser generated URL is like
    // http://localhost/site2/compos/dialog/sixqs/site/controller/MainController/getCategoriesSelect.rmi
    // note that from prefix path 'alert' is missing since is current loaded content

    // in order to discover RMI class path need to remove prefix path, after extension remove
    // for that remove from request path the paths components that are directories into project resources
    // in above example remove 'compos/dialogs/'

    // compos/dialogs/sixqs/site/controller/MainController/getCategoriesSelect.rmi
    requestPath = Files.removeExtension(requestPath);

    // compos/dialogs/sixqs/site/controller/MainController/getCategoriesSelect
    List<String> pathParts = Strings.split(requestPath, '/');
    // [ compos, dialogs, sixqs, site, controller, MainController, getCategoriesSelect]

    // remove path parts that are directories into project resources
    File resourcesPath = project.getResourcesDir().toFile();
    for(;;) {
      resourcesPath = new File(resourcesPath, pathParts.get(0));
      if(!resourcesPath.isDirectory()) {
        break;
      }
      pathParts.remove(0);
    }

    // [ sixqs, site, controller, MainController, getCategoriesSelect]
    methodName = pathParts.remove(pathParts.size() - 1); // getCategoriesSelect
    className = Strings.join(pathParts, '.'); // sixqs.site.controller.MainController
  }

  public void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws JsonParserException, IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException
  {
    Class<?> managedClass = Classes.forName(className);
    Method method = Classes.findMethod(managedClass, methodName);

    Type[] parameterTypes = method.getGenericParameterTypes();
    Class<?> returnType = method.getReturnType();

    Object managedInstance = Factory.getInstance(managedClass);
    Json json = Factory.getInstance(Json.class);
    Object[] arguments = json.parse(new InputStreamReader(httpRequest.getInputStream()), parameterTypes);
    Object result = method.invoke(managedInstance, arguments);

    if(!Types.isVoid(returnType)) {
      httpResponse.setContentType("application/json; charset=UTF-8");
      json.stringify(new OutputStreamWriter(httpResponse.getOutputStream()), result);
    }
  }
}
