package js.wood;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import js.container.AuthorizationException;
import js.container.ManagedClassSPI;
import js.container.ManagedMethodSPI;
import js.core.Factory;
import js.http.ContentType;
import js.http.encoder.ArgumentsReader;
import js.http.encoder.ServerEncoders;
import js.json.Json;
import js.lang.SyntaxException;
import js.util.Classes;

/**
 * Invoke method identified by REST path.
 * 
 * @author Iulian Rotaru
 */
public class RestRequestHandler
{
  /**
   * A request path starting with <code>rest</code> is considered REST request.
   * 
   * @param requestPath request path.
   * @return true if request path identify a REST resource.
   */
  public static boolean accept(String requestPath)
  {
    return requestPath.startsWith("rest");
  }

  /** Remote method. */
  private ManagedMethodSPI netMethod;

  public RestRequestHandler(Project project, String requestPath)
  {
    // RestMethods restMethods = Factory.getInstance(RestMethods.class);
    // netMethod = restMethods.getRestMethod("/" + requestPath);
  }

  public void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws SyntaxException, IllegalArgumentException, InvocationTargetException, IOException, AuthorizationException
  {
    if(netMethod == null) {
      return;
    }
    // ManagedClassSPI netClass = netMethod.getDeclaringClass();

    ServerEncoders encoders = ServerEncoders.getInstance();
    ArgumentsReader argumentsReader = encoders.getArgumentsReader(httpRequest, netMethod.getParameterTypes());
    Object[] parameters = argumentsReader.read(httpRequest, netMethod.getParameterTypes());

    Object instance = Factory.getAppFactory().getInstance(ManagedClassSPI.class);
    Object value = netMethod.invoke(instance, parameters);

    httpResponse.setCharacterEncoding("UTF-8");
    if(netMethod.isVoid()) {
      httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
      httpResponse.setContentLength(0);
      return;
    }

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    Json json = Classes.loadService(Json.class);
    json.stringify(new OutputStreamWriter(buffer, "UTF-8"), value);

    httpResponse.setStatus(HttpServletResponse.SC_OK);
    httpResponse.setContentType(ContentType.APPLICATION_JSON.getValue());
    httpResponse.setContentLength(buffer.size());

    OutputStream outputStream = httpResponse.getOutputStream();
    outputStream.write(buffer.toByteArray());
    outputStream.flush();
    outputStream.close();
  }
}
