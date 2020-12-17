package js.tools.commons.rmi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import js.tools.commons.BugError;
import js.tools.commons.json.JSON;
import js.tools.commons.util.Classes;
import js.tools.commons.util.Files;
import js.tools.commons.util.Params;
import js.tools.commons.util.Strings;
import js.tools.commons.util.Types;

public class HttpRmi
{
  /** Magic name for client packages used by service providers. */
  private static final String CLIENT_PACKAGE_SUFIX = ".client";

  /** Status code (200) indicating the request succeeded normally. */
  private static final int SC_OK = 200;

  /** Status code (204) for successful processing but no content returned. */
  private static final int SC_NO_CONTENT = 204;

  /** Status code (401) indicating that the request requires HTTP authentication. */
  private static final int SC_UNAUTHORIZED = 401;

  /** Status code (403) indicating the server understood the request but refused to fulfill it. */
  private static final int SC_FORBIDDEN = 403;

  /** Status code (404) indicating that the requested resource is not available. */
  private static final int SC_NOT_FOUND = 404;

  /** Status code (400) indicating the request sent by the client was syntactically incorrect. */
  private static final int SC_BAD_REQUEST = 400;

  /** Status code (500) indicating an error inside the HTTP server which prevented it from fulfilling the request. */
  private static final int SC_INTERNAL_SERVER_ERROR = 500;

  /** Status code (503) indicating that the HTTP server is temporarily overloaded, and unable to handle the request. */
  private static final int SC_SERVICE_UNAVAILABLE = 503;

  /** HTTP-RMI connection timeout, in milliseconds. */
  private static final int CONNECTION_TIMEOUT = 60000;

  /** HTTP-RMI read timeout, in milliseconds. */
  private static final int READ_TIMEOUT = 120000;

  private final ConnectionFactory connectionFactory;

  /** URL for host where remote method is deployed. */
  private final String implementationURL;

  private final String className;

  /** Returned value type, default to void. This type should be consistent with remote method signature. */
  private Type returnType = Void.TYPE;

  /** Remote method exceptions list as declared into method signature. */
  private List<String> exceptions = new ArrayList<String>();

  /** Remote method arguments writer. */
  private ArgumentsWriter argumentsWriter;

  /**
   * Create HTTP-RMI transaction with default connection factory.
   * 
   * @param implementationURL UR where remote implementation is hosted, trailing path separator ignored,
   * @param remoteInterface remote interface.
   */
  public HttpRmi(String implementationURL, Class<?> remoteInterface)
  {
    this(new DefaultConnectionFactory(), implementationURL, remoteInterface);
  }

  /**
   * Create HTTP-RMI transaction for remote interface hosted on given URL. Uses specified connection factory to actually
   * open a connection with given URL.
   * 
   * @param connectionFactory connection factory,
   * @param implementationURL UR where remote implementation is hosted, trailing path separator ignored,
   * @param remoteInterface remote interface.
   */
  public HttpRmi(ConnectionFactory connectionFactory, String implementationURL, Class<?> remoteInterface)
  {
    this.connectionFactory = connectionFactory;
    if(implementationURL.charAt(implementationURL.length() - 1) == '/') {
      implementationURL = implementationURL.substring(0, implementationURL.length() - 1);
    }
    this.implementationURL = implementationURL;

    String packageName = remoteInterface.getPackage().getName();
    String className = remoteInterface.getName();
    if(packageName.endsWith(CLIENT_PACKAGE_SUFIX)) {
      // if remote class is declared into client package just use parent package instead
      className = className.replace(CLIENT_PACKAGE_SUFIX, "");
    }
    this.className = className;
  }

  /**
   * Set expected type for the returned value. Returned type should be consistent with remote method signature.
   * 
   * @param returnType expected type for returned value.
   * @throws IllegalArgumentException if <code>returnType</code> argument is null.
   */
  public void setReturnType(Type returnType)
  {
    Params.notNull(returnType, "Return value type");
    this.returnType = returnType;
  }

  /**
   * Set method exceptions list. This exceptions list is used by the logic that handle remote exception. It should be
   * consistent with remote method signature.
   * 
   * @param exceptions method exceptions list.
   */
  public void setExceptions(Class<?>... exceptions)
  {
    for(Class<?> exception : exceptions) {
      // uses simple name because exception may be declared into .client package
      this.exceptions.add(exception.getSimpleName());
    }
  }

  @SuppressWarnings("unchecked")
  public <T> T invoke(String methodName, Object... arguments) throws Exception
  {
    StringBuilder builder = new StringBuilder();
    builder.append(implementationURL);
    builder.append('/');
    builder.append(Files.dot2urlpath(className));
    builder.append('/');
    builder.append(methodName);
    builder.append(".rmi");
    // TODO: create some sort of logging class
    System.out.printf("Execute HTTP-RMI on %s\r\n", builder.toString());

    URL url = new URL(builder.toString());
    HttpURLConnection connection = connectionFactory.openConnection(url);
    connection.setConnectTimeout(CONNECTION_TIMEOUT);
    connection.setReadTimeout(READ_TIMEOUT);

    connection.setRequestMethod(arguments == null ? "GET" : "POST");

    connection.setRequestProperty("User-Agent", "j(s)-lib");
    connection.setRequestProperty("Accept", "application/json, text/xml, application/octet-stream");
    connection.setRequestProperty("Pragma", "no-cache");
    connection.setRequestProperty("Cache", "no-cache");

    if(arguments.length > 0) {
      connection.setDoOutput(true);
      argumentsWriter = ClientEncoders.getInstance().getArgumentsWriter(arguments);
      String contentType = argumentsWriter.getContentType();
      if(contentType != null) {
        connection.setRequestProperty("Content-Type", contentType);
      }
      argumentsWriter.write(connection.getOutputStream(), arguments);
    }

    int statusCode = connection.getResponseCode();
    if(statusCode != SC_OK && statusCode != SC_NO_CONTENT) {
      onError(connection, statusCode);
      // error handler throws exception on any status code
    }

    if(Types.isVoid(returnType)) {
      return null;
    }
    if(!Types.isVoid(returnType) && connection.getContentLength() == 0) {
      throw new BugError("Invalid HTTP-RMI transaction with |%s|. Expect return value of type |%s| but got empty response.", connection.getURL(), returnType);
    }

    ValueReader valueReader = ClientEncoders.getInstance().getValueReader(connection);
    try {
      return (T)valueReader.read(connection.getInputStream(), returnType);
    }
    catch(IOException e) {
      throw new BugError("Invalid HTTP-RMI transaction with |%s|. Response cannot be parsed to type |%s|. Cause: %s", connection.getURL(), returnType, e);
    }
  }

  @SuppressWarnings("unchecked")
  private void onError(HttpURLConnection connection, int statusCode) throws Exception
  {
    // if status code is [200 300) range response body is accessible via getInputStream
    // otherwise getErrorStream should be used
    // trying to use getInputStream for status codes not in [200 300) range will rise IOException

    switch(statusCode) {
    case SC_FORBIDDEN:
      // server understood the request but refuses to fulfill it
      // compared with SC_UNAUTHORIZED, sending authentication will not grant access
      // common SC_FORBIDDEN condition may be Tomcat filtering by remote address and client IP is not allowed
      throw new RmiException("Server refuses to process request |%s|. Common cause may be Tomcat filtering by remote address and this IP is not allowed.", connection.getURL());

    case SC_UNAUTHORIZED:
      throw new RmiException("Attempt to access private remote method |%s| without authorization.", connection.getURL());

    case SC_NOT_FOUND:
      // not found may occur if front end Apache HTTP server does not recognize the protocol, e.g. trying to access
      // securely a
      // public method or because of misspelled extension; also virtual host configuration for remote context may be
      // wrong
      throw new RmiException("Method |%s| not found. Check URL spelling, protocol unmatched or unrecognized extension.", connection.getURL());

    case SC_SERVICE_UNAVAILABLE:
      // front end HTTP server is running but application server is down; front end server responds with 503
      throw new RmiException("Front-end HTTP server is up but back-end is down. HTTP-RMI transaction |%s| aborted.", connection.getURL());

    case SC_BAD_REQUEST:
      // handle business constrain as server internal error
    case SC_INTERNAL_SERVER_ERROR:
      if(isJSON(connection.getContentType())) {
        RemoteException remoteException = (RemoteException)readJsonObject(connection.getErrorStream(), RemoteException.class);

        // if remote exception is an exception declared by method signature we throw it in this virtual machine
        if(exceptions.contains(getRemoteExceptionCause(remoteException))) {
          Class<? extends Throwable> cause = null;
          try {
            cause = (Class<? extends Throwable>)Class.forName(remoteException.getCause());
          }
          catch(ClassNotFoundException expected) {}

          if(cause != null) {
            String message = remoteException.getMessage();
            if(message == null) {
              throw (Exception)Classes.newInstance(cause);
            }
            throw (Exception)Classes.newInstance(cause, remoteException.getMessage());
          }
        }

        // if received remote exception is not listed by method signature replace it with BugError
        throw new BugError("HTTP-RMI server execution error on |%s|: %s", connection.getURL(), remoteException);
      }
    }

    throw new RmiException("HTTP-RMI error on |%s|. Server returned |%d|.", connection.getURL(), statusCode);
  }

  /**
   * Test if content type describe JSON stream. Note that this library uses <code>application/json</code> as default
   * value for content type. As a consequence null <code>contentType</code> parameter is accepted as JSON.
   * 
   * @param contentType content type, possible null.
   * @return true if content type describe a JSON stream.
   */
  private static boolean isJSON(String contentType)
  {
    return contentType == null || contentType.startsWith("application/json");
  }

  /**
   * Read JSON object from input stream and return initialized object instance.
   * 
   * @param stream input stream,
   * @param type expected type.
   * @return object instance of requested type.
   * @throws IOException if stream reading or JSON parsing fails.
   */
  private static Object readJsonObject(InputStream stream, Type type) throws IOException
  {
    Reader reader = new BufferedReader(new InputStreamReader(stream));
    try {
      return JSON.parse(reader, type);
    }
    finally {
      // do not use Files.close because we want to throw IOException is reader close fails
      if(reader != null) {
        reader.close();
      }
    }
  }

  /**
   * Get the class simple name of the remote exception cause.
   * 
   * @param remoteException remote exception instance.
   * @return remote exception cause simple name.
   */
  private static String getRemoteExceptionCause(RemoteException remoteException)
  {
    return Strings.last(remoteException.getCause(), '.');
  }

  /**
   * Connection factory opens a connection with remote implementation.
   * 
   * @author Iulian Rotaru
   * @version final
   */
  private static class DefaultConnectionFactory implements ConnectionFactory
  {
    @Override
    public HttpURLConnection openConnection(URL url) throws IOException
    {
      return (HttpURLConnection)url.openConnection();
    }
  }
}
