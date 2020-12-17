package js.tools.commons.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Generic type.
 * <p>
 * Description for terms usage. Be it a generic class definition and a reference, see sample codes. On definition,
 * <code>Box</code> is the class name; in GType class context it is name {@link #rawType}. The type parameter section, delimited
 * by angle brackets, follows the class name. It specifies the type parameters (also called type variables), in this case T.
 * Type parameters are not represented by GType class.
 * 
 * <pre>
 * class Box&lt;T&gt; {
 * 	private T t;
 * }
 * </pre>
 * <p>
 * To reference the generic Box class from within your code, you must perform a generic type invocation, which replaces T with
 * some concrete value, such as Integer:
 * 
 * <pre>
 * Box&lt;Integer&gt; integers = new Box&lt;Integer&gt;();
 * </pre>
 * <p>
 * The list of type arguments used to reference a generic type is named {@link #actualTypeArguments} into GType class. Note that
 * a generic type reference, in which type parameters/variables are replaced by concrete type is named
 * <em>parameterized type</em>.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class GType implements ParameterizedType {
	/**
	 * Class or interface declaring this parameterized type. For example, for <code>Map&lt;String, Integer&gt;</code> this field
	 * contains <code>java.util.Map</code>.
	 */
	private final Type rawType;

	/**
	 * Actual type arguments used to reference parameterized type. For example, for <code>Map&lt;String, Integer&gt;</code> this
	 * field contains <code>java.lang.String</code> and <code>java.lang.Integer</code>.
	 */
	private final Type[] actualTypeArguments;

	/**
	 * Construct immutable parameterized type instance.
	 * 
	 * @param rawType class or interface declaring this parameterized type,
	 * @param actualTypeArguments actual type arguments used to reference parameterized type.
	 */
	public GType(Type rawType, Type... actualTypeArguments) {
		this.rawType = rawType;
		this.actualTypeArguments = actualTypeArguments;
	}

	/**
	 * Returns an array of Type objects representing the actual type arguments to this type.
	 * <p>
	 * Note that in some cases, the returned array be empty. This can occur if this type represents a non-parameterized type
	 * nested within a parameterized type.
	 * 
	 * @return an array of Type objects representing the actual type arguments to this type.
	 * @see #actualTypeArguments
	 */
	@Override
	public Type[] getActualTypeArguments() {
		return actualTypeArguments;
	}

	/**
	 * Always returns null since GType is top level class, that is, is not declared as inner class.
	 */
	@Override
	public Type getOwnerType() {
		return null;
	}

	/**
	 * Returns the Type object representing the class or interface that declared this type.
	 * 
	 * @return the Type object representing the class or interface that declared this type.
	 * @see #rawType
	 */
	@Override
	public Type getRawType() {
		return rawType;
	}

	/** Cached string representation for this GType instance. */
	private String string;

	/** String representation. */
	@Override
	public String toString() {
		if (string == null) {
			StringBuilder builder = new StringBuilder();
			builder.append(rawType);
			builder.append('<');
			for (int i = 0; i < actualTypeArguments.length; ++i) {
				if (i > 0) {
					builder.append(", ");
				}
				builder.append(actualTypeArguments[i]);
			}
			builder.append('>');
		}
		return string;
	}
}
