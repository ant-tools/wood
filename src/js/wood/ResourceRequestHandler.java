package js.wood;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import js.container.ContainerSPI;
import js.container.ManagedMethodSPI;
import js.core.Factory;
import js.http.Resource;
import js.http.encoder.ArgumentsReader;
import js.http.encoder.ServerEncoders;
import js.lang.BugError;
import js.util.Types;

/**
 * Request handler for dynamic resources like CAPTCHA images.
 * 
 * @author Iulian Rotaru
 */
public class ResourceRequestHandler
{
  private static final Pattern PATH_PATTERN = Pattern.compile("^(?:\\/([a-z][a-z0-9-]*)\\/)?([a-z][a-z0-9-]*)(\\..+)?(?:\\?.*)?$");

  /**
   * Filter for resource request path.
   * 
   * @param requestPath request path.
   * @return return true if request path is for a dynamic resource.
   */
  public static boolean accept(String requestPath)
  {
    return requestPath.endsWith(".xsp") || requestPath.contains("/captcha/image");
  }

  private final Map<String, ManagedMethodSPI> methods = new HashMap<>();

  private String resourcePath;

  public ResourceRequestHandler(Project project, HttpServletRequest httpRequest) throws MalformedURLException
  {
    // for historical reasons RMI requests handler uses different heuristic to detect resource path
    // for now keep it separately till this approach will be proved by production

    // -----------------------------------------------------------
    // resource request handler heuristic to detect resource path

    // starts with value we know: requestURI and referer and need to get resourcePath
    // requestURI : /kids-cademy/compo/catcha/get-resource.xsp
    // referer : http://localhost/kids-cademy/compo/captcha
    // resourcePath: /captcha/get-resource.xsp

    // heuristic description: remove refererPath from requestURI less last path component that is the current loading
    // context; refererPath is the path part of the referer URL

    String requestURI = httpRequest.getRequestURI();
    String referer = httpRequest.getHeader("Referer");

    // referer : http://localhost/kids-cademy/compo/captcha
    int firstRefererSlash = referer.indexOf('/', referer.indexOf("//") + 2);
    int lastRefererSlash = referer.lastIndexOf('/');
    String refererMask = referer.substring(firstRefererSlash, lastRefererSlash);

    // requestURI : /kids-cademy/compo/catcha/get-resource.xsp
    // refererMask: /kids-cademy/compo
    this.resourcePath = requestURI.substring(refererMask.length());
  }

  public void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception
  {
    Matcher matcher = PATH_PATTERN.matcher(resourcePath);
    if(!matcher.find()) {
      throw new BugError("Invalid reosurce path");
    }
    ContainerSPI container = (ContainerSPI)Factory.getAppFactory();

    if(methods.isEmpty()) {
      for(ManagedMethodSPI method : container.getManagedMethods()) {
        if(Types.isKindOf(method.getReturnType(), Resource.class)) {
          methods.put(getStorageKey(method), method);
        }
      }
    }

    ManagedMethodSPI netMethod = methods.get(getRetrievalKey(resourcePath));

    ArgumentsReader argumentsReader = null;
    try {
      Type[] formalParameters = netMethod.getParameterTypes();
      Object[] arguments = new Object[0];

      if(formalParameters.length > 0) {
        argumentsReader = ServerEncoders.getInstance().getArgumentsReader(httpRequest, formalParameters);
        arguments = argumentsReader.read(httpRequest, netMethod.getParameterTypes());
      }

      Object instance = container.getInstance(netMethod.getDeclaringClass());
      Resource resource = netMethod.invoke(instance, arguments);
      httpResponse.setStatus(HttpServletResponse.SC_OK);
      resource.serialize(httpResponse);
    }
    finally {
      argumentsReader.clean();
    }
  }

  // --------------------------------------------------------------------------------------------
  // UTILITY METHODS

  private static String getStorageKey(ManagedMethodSPI managedMethod)
  {
    StringBuilder requestPath = new StringBuilder();
    if(managedMethod.getDeclaringClass().getRequestPath() != null) {
      requestPath.append('/');
      requestPath.append(managedMethod.getDeclaringClass().getRequestPath());
    }
    requestPath.append('/');
    requestPath.append(managedMethod.getRequestPath());
    return requestPath.toString();
  }

  private static String getRetrievalKey(String requestPath)
  {
    int queryParametersIndex = requestPath.lastIndexOf('?');
    if(queryParametersIndex == -1) {
      queryParametersIndex = requestPath.length();
    }
    int extensionIndex = requestPath.lastIndexOf('.', queryParametersIndex);
    if(extensionIndex == -1) {
      extensionIndex = requestPath.length();
    }
    return requestPath.substring(0, extensionIndex);
  }
}
