package js.tools.commons.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ServiceLoader;

import js.tools.commons.BugError;
import js.tools.commons.NoSuchBeingException;

/**
 * Handy methods, mostly reflexive, related to class and class loader. This utility class allows for sub-classing. See
 * {@link js.util} for utility sub-classing description.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public final class Classes
{
  /** Prevent default constructor synthesis. */
  private Classes()
  {
  }

  /**
   * Load named class using current thread context class loader. Uses current thread context class loader to locate and
   * load requested class. If current thread context class loader is null or fails to find requested class try with this
   * utility class class loader.
   * <p>
   * This logic is designed for Tomcat class loading algorithm. Libraries are loaded using a separated class loader and
   * every application has its own class loader. This method algorithm allows for a class used by an application to be
   * found either by current thread or by library class loader.
   * <p>
   * Considering above, note that there is a subtle difference compared with standard {@link Class#forName(String)}
   * counterpart: this method uses <code>current thread context loader</code> whereas Java standard uses
   * <code>current loader</code>. Maybe not obvious, this 'semantic' difference could lead to class not found on Java
   * standard while this utility method find the class. For example, a class defined by an web application could not be
   * found by Java <code>Class.forName</code> method running inside a class defined by library.
   * 
   * @param className qualified class name.
   * @param <T> class to auto-cast named class.
   * @return class identified by name.
   * @throws NoSuchBeingException if class not found.
   * @throws ClassCastException if found class cannot cast to requested auto-cast type.
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> forName(String className)
  {
    try {
      return (Class<T>)Class.forName(className);
    }
    catch(ClassNotFoundException e) {}
    throw new NoSuchBeingException("Class not found: " + className);
  }

  /**
   * Convenient method to retrieve named resource as reader. Uses {@link #getResourceAsStream(String)} and return the
   * stream wrapped into a reader.
   * 
   * @param name resource name.
   * @return reader for named resource.
   * @throws UnsupportedEncodingException if Java run-time does not support UTF-8 encoding.
   * @throws NoSuchBeingException if resource not found.
   */
  public static Reader getResourceAsReader(String name) throws UnsupportedEncodingException
  {
    return new InputStreamReader(getResourceAsStream(name), "UTF-8");
  }

  /**
   * Retrieve named resource as input stream. This method does its best to load requested resource, as follow, but
   * throws exception if fail:
   * <ul>
   * <li>this utility class loader,
   * <li>current thread context class loader,
   * <li>this utility class,
   * <li>this utility class loader parent.
   * </ul>
   * This method uses internally {@link Class#getResourceAsStream(String)} and
   * {@link ClassLoader#getResourceAsStream(String)} and name syntax is as used by those methods.
   * 
   * @param name resource name.
   * @return resource input stream.
   * @throws NoSuchBeingException if resource not found.
   */
  private static InputStream getResourceAsStream(String name)
  {
    InputStream stream = Classes.class.getClassLoader().getResourceAsStream(name);
    if(stream == null) {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      if(loader != null) {
        stream = loader.getResourceAsStream(name);
      }
    }
    if(stream == null) {
      stream = Classes.class.getResourceAsStream(name);
      if(stream == null) {
        ClassLoader parentClassLoader = Classes.class.getClassLoader().getParent();
        if(parentClassLoader != null) {
          stream = parentClassLoader.getResourceAsStream(name);
        }
      }
    }
    if(stream == null) {
      throw new NoSuchBeingException("Resource |%s| not found.", name);
    }
    return stream;
  }

  /**
   * Retrieve text resource content as a string. Uses {@link #getResourceAsReader(String)} to reader resource content
   * and store it in a String.
   * 
   * @param name resource name.
   * @return resource content as string.
   * @throws NoSuchBeingException if resource not found.
   * @throws IOException if resource reading fails.
   */
  public static String getResourceAsString(String name) throws IOException
  {
    StringWriter writer = new StringWriter();
    Files.copy(getResourceAsReader(name), writer);
    return writer.toString();
  }

  /**
   * Create a new instance. Handy utility for hidden classes creation. Constructor accepting given arguments, if any,
   * must exists.
   * 
   * @param className fully qualified class name,
   * @param arguments variable number of arguments to be passed to constructor.
   * @param <T> instance type.
   * @return newly created instance.
   * @throws NoSuchBeingException if class or constructor not found.
   */
  @SuppressWarnings("unchecked")
  public static <T> T newInstance(String className, Object... arguments) throws ClassNotFoundException
  {
    return (T)newInstance(Class.forName(className), arguments);
  }

  /**
   * Create a new instance of specified class. If arguments are supplied a constructor with exact formal parameters is
   * located otherwise default constructor is used; if none found throws {@link NoSuchBeingException}. This method
   * forces accessibility so is not mandatory for constructor to be public.
   * 
   * @param clazz class to instantiate,
   * @param arguments optional constructor arguments.
   * @param <T> instance type.
   * @return newly created instance
   * @throws BugError if attempt to instantiate interface, abstract or void class.
   * @throws NoSuchBeingException if class or constructor not found.
   * @throws BugError for any other failing condition, since there is no particular reason to expect fail.
   * @throws JsException with target exception if constructor fails on its execution.
   */
  @SuppressWarnings("unchecked")
  public static <T> T newInstance(Class<T> clazz, Object... arguments)
  {
    if(clazz.isInterface()) {
      throw new BugError("Attempt to create new instance for interface |%s|.", clazz);
    }
    if(Modifier.isAbstract(clazz.getModifiers())) {
      throw new BugError("Attempt to create new instance for abstract class |%s|.", clazz);
    }
    if(Types.isVoid(clazz)) {
      throw new BugError("Attempt to instantiate void class.");
    }

    try {
      Constructor<T> constructor = null;
      if(arguments.length > 0) {
        constructorsLoop: for(Constructor<?> ctor : clazz.getDeclaredConstructors()) {
          Class<?>[] parameters = ctor.getParameterTypes();
          if(parameters.length != arguments.length) {
            continue;
          }
          for(int i = 0; i < arguments.length; i++) {
            if(arguments[i] == null) {
              continue;
            }
            if(!Types.isInstanceOf(arguments[i], parameters[i])) {
              continue constructorsLoop;
            }
          }
          constructor = (Constructor<T>)ctor;
          break;
        }
        if(constructor == null) {
          throw missingConstructorException(clazz, arguments);
        }
      }
      else {
        constructor = clazz.getDeclaredConstructor();
      }
      constructor.setAccessible(true);
      return constructor.newInstance(arguments);
    }
    catch(NoSuchMethodException e) {
      throw missingConstructorException(clazz, arguments);
    }
    catch(InstantiationException | IllegalAccessException | IllegalArgumentException e) {
      throw new BugError(e);
    }
    catch(InvocationTargetException e) {
      throw new BugError(e.getTargetException());
    }
  }

  /**
   * Helper for missing constructor exception.
   * 
   * @param clazz constructor class,
   * @param arguments constructor arguments.
   * @return formatted exception.
   */
  private static NoSuchBeingException missingConstructorException(Class<?> clazz, Object... arguments)
  {
    Type[] types = new Type[arguments.length];
    for(int i = 0; i < arguments.length; ++i) {
      types[i] = arguments[i].getClass();
    }
    return new NoSuchBeingException("Missing constructor(%s) for |%s|.", Arrays.toString(types), clazz);
  }

  /**
   * Get instance or class field value. Retrieve named field value from given instance; if <code>object</code> argument
   * is a {@link Class} retrieve class static field.
   * 
   * @param object instance or class to retrieve field value from,
   * @param fieldName field name.
   * @param <T> field value type.
   * @return instance or class field value.
   * @throws NullPointerException if object argument is null.
   * @throws NoSuchBeingException if field is missing.
   * @throws BugError if <code>object</code> is a class and field is not static or if <code>object</code> is an
   *           instance and field is static.
   */
  @SuppressWarnings("unchecked")
  public static <T> T getFieldValue(Object object, String fieldName)
  {
    if(object instanceof Class<?>) {
      return getFieldValue(null, (Class<?>)object, fieldName, null, false);
    }

    Class<?> clazz = object.getClass();
    try {
      Field f = clazz.getDeclaredField(fieldName);
      f.setAccessible(true);
      return (T)f.get(Modifier.isStatic(f.getModifiers()) ? null : object);
    }
    catch(java.lang.NoSuchFieldException e) {
      throw new NoSuchBeingException("Missing field |%s| from |%s|.", fieldName, clazz);
    }
    catch(IllegalAccessException e) {
      throw new BugError(e);
    }
  }

  /**
   * Helper method for field value retrieval. Get object field value, declared into specified class which can be object
   * class or superclass. If desired field type is not null retrieved field should have the type; otherwise returns
   * null. If field not found this method behavior depends on <code>optional</code> argument: if true returns null,
   * otherwise throws exception.
   * 
   * @param object instance to retrieve field value from or null if static field,
   * @param clazz class or superclass where field is actually declared,
   * @param fieldName field name,
   * @param fieldType desired field type or null,
   * @param optional if true, return null if field is missing.
   * @param <T> field value type.
   * @return field value or null.
   * @throws NoSuchBeingException if optional flag is false and field is missing.
   * @throws BugError if object is null and field is not static or if object is not null and field is static.
   */
  @SuppressWarnings("unchecked")
  private static <T> T getFieldValue(Object object, Class<?> clazz, String fieldName, Class<T> fieldType, boolean optional)
  {
    try {
      Field f = clazz.getDeclaredField(fieldName);
      if(fieldType != null && fieldType != f.getType()) {
        return null;
      }
      f.setAccessible(true);
      if(object == null ^ Modifier.isStatic(f.getModifiers())) {
        throw new BugError("Cannot access static field from instance or instance field from null object.");
      }
      return (T)f.get(object);
    }
    catch(java.lang.NoSuchFieldException e) {
      if(optional) {
        return null;
      }
      throw new NoSuchBeingException("Missing field |%s| from |%s|.", fieldName, clazz);
    }
    catch(IllegalAccessException e) {
      throw new BugError(e);
    }
  }

  /**
   * Set named instance or class field value. Try to set instance field throwing exception if field not found. If
   * <code>object</code> argument is a class, named field should be static; otherwise exception is thrown.
   * 
   * @param object instance or class to set field value to,
   * @param fieldName field name,
   * @param value field value.
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   * @throws NullPointerException if object argument is null.
   * @throws NoSuchBeingException if field not found.
   * @throws BugError if object is null and field is not static or if object is not null and field is static.
   */
  public static void setFieldValue(Object object, String fieldName, Object value) throws IllegalArgumentException, IllegalAccessException
  {
    if(object instanceof Class<?>) {
      setFieldValue(null, (Class<?>)object, fieldName, value);
    }
    else {
      setFieldValue(object, object.getClass(), fieldName, value);
    }
  }

  /**
   * Set instance field declared into superclass. Try to set field value throwing exception if field is not declared
   * into superclass; if field is static object instance should be null.
   * 
   * @param object instance to set field value to or null if field is static,
   * @param clazz instance superclass where field is declared,
   * @param fieldName field name,
   * @param value field value.
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   * @throws NoSuchBeingException if field not found.
   * @throws BugError if object is null and field is not static or if object is not null and field is static.
   */
  private static void setFieldValue(Object object, Class<?> clazz, String fieldName, Object value) throws IllegalArgumentException, IllegalAccessException
  {
    Field field = null;
    try {
      field = clazz.getDeclaredField(fieldName);
    }
    catch(NoSuchFieldException e) {
      throw new NoSuchBeingException(e);
    }
    catch(SecurityException e) {
      throw new BugError(e);
    }

    if(object == null ^ Modifier.isStatic(field.getModifiers())) {
      throw new BugError("Cannot access static field |%s| from instance |%s|.", fieldName, clazz);
    }
    Params.notNull(field, "Field");
    field.setAccessible(true);
    field.set(object, value);
  }

  /**
   * Invoke instance or class method with arguments. If this method <code>object</code> argument is a {@link Class}
   * delegate {@link #invoke(Object, Class, String, Object...)} with first argument set to null; otherwise
   * <code>object</code> is passed as first argument and its class the second.
   * 
   * @param object object instance or class,
   * @param methodName method name,
   * @param arguments variable number of arguments.
   * @param <T> returned value type.
   * @return value returned by method or null.
   * @throws NoSuchBeingException if method is not found.
   * @throws Exception if invocation fail for whatever reason including method internals.
   */
  public static <T> T invoke(Object object, String methodName, Object... arguments) throws Exception
  {
    Params.notNull(object, "Object");
    Params.notNullOrEmpty(methodName, "Method name");
    if(object instanceof Class<?>) {
      return invoke(null, (Class<?>)object, methodName, arguments);
    }
    else {
      return invoke(object, object.getClass(), methodName, arguments);
    }
  }

  /**
   * Reflexively executes a method on an object. Locate the method on given class, that is not necessarily object class,
   * e.g. it can be a superclass, and execute it. Given arguments are used for both method discovery and invocation.
   * <p>
   * Implementation note: this method is a convenient way to invoke a method when one knows actual parameters but not
   * strictly formal parameters types. When formal parameters include interfaces or abstract classes or an actual
   * parameter is null there is no way to infer formal parameter type from actual parameter instance. The only option
   * left is to locate method by name and if overloads found uses best effort to determine the right parameter list. For
   * this reason, on limit is possible to invoke the wrong method. Anyway, <b>this method is designed for tests
   * logic</b> and best effort is good enough. The same is true for {@link #invoke(Object, String, Object...)}.
   * 
   * @param object object instance,
   * @param clazz object class one of its superclass,
   * @param methodName method name,
   * @param arguments variable number of arguments.
   * @param <T> returned value type.
   * @return value returned by method or null.
   * @throws NoSuchBeingException if method is not found.
   * @throws Exception if invocation fail for whatever reason including method internals.
   */
  @SuppressWarnings("unchecked")
  public static <T> T invoke(Object object, Class<?> clazz, String methodName, Object... arguments) throws Exception
  {
    Params.notNull(clazz, "Class");
    Params.notNullOrEmpty(methodName, "Method name");
    Class<?>[] parameterTypes = getParameterTypes(arguments);
    try {
      Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
      return (T)invoke(object, method, arguments);
    }
    catch(NoSuchMethodException e) {
      // optimistic attempt to locate the method has failed
      // maybe because method parameters list includes interfaces, primitives or null
      // there is no other option but to search through all object methods

      methodsLoop: for(Method method : clazz.getDeclaredMethods()) {
        Class<?>[] methodParameters = method.getParameterTypes();
        if(!method.getName().equals(methodName)) {
          continue;
        }
        if(methodParameters.length != arguments.length) {
          continue;
        }
        // test if concrete arguments list match method formal parameters; if not continue methods loop
        // null is accepted as any type
        for(int i = 0; i < arguments.length; i++) {
          if(arguments[i] != null && !Types.isInstanceOf(arguments[i], methodParameters[i])) {
            continue methodsLoop;
          }
        }
        return (T)invoke(object, method, arguments);
      }
      throw new NoSuchBeingException("Method %s(%s) not found.", methodName, parameterTypes);
    }
  }

  /**
   * Do the actual reflexive method invocation.
   * 
   * @param object object instance,
   * @param method reflexive method,
   * @param arguments variable number of arguments.
   * @return value returned by method execution.
   * @throws Exception if invocation fail for whatever reason including method internals.
   */
  private static Object invoke(Object object, Method method, Object... arguments) throws Exception
  {
    Throwable cause = null;
    try {
      method.setAccessible(true);
      return method.invoke(object instanceof Class<?> ? null : object, arguments);
    }
    catch(IllegalAccessException e) {
      throw new BugError(e);
    }
    catch(InvocationTargetException e) {
      cause = e.getCause();
      if(cause instanceof Exception) {
        throw (Exception)cause;
      }
      if(cause instanceof AssertionError) {
        throw (AssertionError)cause;
      }
    }
    throw new BugError("Method |%s| invocation fails: %s", method, cause);
  }

  /**
   * Get method formal parameter types inferred from actual invocation arguments. This utility is a helper for method
   * discovery when have access to the actual invocation argument, but not the formal parameter types list declared by
   * method signature.
   * 
   * @param arguments variable number of method arguments.
   * @return parameter types.
   */
  public static Class<?>[] getParameterTypes(Object... arguments)
  {
    Class<?>[] types = new Class<?>[arguments.length];
    for(int i = 0; i < arguments.length; i++) {
      Object argument = arguments[i];
      if(argument == null) {
        types[i] = Object.class;
        continue;
      }
      types[i] = argument.getClass();
      if(types[i].isAnonymousClass()) {
        Class<?>[] interfaces = types[i].getInterfaces();
        Class<?> superclass = interfaces.length > 0 ? interfaces[0] : null;
        if(superclass == null) {
          superclass = types[i].getSuperclass();
        }
        types[i] = superclass;
      }
    }
    return types;
  }

  /**
   * Load service of requested interface throwing exception if provider not found. It is a convenient variant of
   * {@link #loadService(Class)} usable when a missing service implementation is a run-time stopper.
   * 
   * @param serviceInterface service interface.
   * @param <S> service type
   * @return service instance.
   * @throws NoProviderException if service provider not found on run-time.
   */
  public static <S> S loadService(Class<S> serviceInterface)
  {
    Iterator<S> services = ServiceLoader.load(serviceInterface, serviceInterface.getClassLoader()).iterator();
    if(services.hasNext()) {
      return services.next();
    }
    throw new BugError("No service provider found for |%s|.", serviceInterface);
  }
}
