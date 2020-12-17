package js.tools.commons;

/**
 * No such being exception. Thrown whenever something expected is missing like a class member, class itself or even DOM element.
 * It can be seen, but not limited to, as unchecked counterpart of {@link NoSuchMethodException}, {@link NoSuchFieldException}
 * or {@link ClassNotFoundException}.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public class NoSuchBeingException extends RuntimeException {
	/** Java serialization version. */
	private static final long serialVersionUID = 2689851485194099945L;

	/**
	 * Constructor from formatted message. See {@link JsException#JsException(String, Object...)} for format description.
	 * 
	 * @param message exception formatted message,
	 * @param args variable number of arguments for formatted message.
	 */
	public NoSuchBeingException(String message, Object... args) {
		super(String.format(message, args));
	}

	/**
	 * Constructor from root cause.
	 * 
	 * @param cause root cause.
	 */
	public NoSuchBeingException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor with message and root cause.
	 * 
	 * @param message exception context messages,
	 * @param cause root cause.
	 */
	public NoSuchBeingException(String message, Throwable cause) {
		super(message, cause);
	}
}
