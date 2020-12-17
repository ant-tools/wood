package js.tools.commons.rmi;

import js.tools.commons.util.Strings;

/**
 * Data transport object used by HTTP-RMI protocol to convey information about exceptional condition at server level.
 * 
 * @author Iulian Rotaru
 * @since 1.7
 * @version draft
 */
public final class RemoteException {
	/** Remote exception cause. */
	private final String cause;

	/** Remote exception message. */
	private final String message;

	/** Default constructor. */
	RemoteException() {
		this.cause = null;
		this.message = null;
	}

	/**
	 * Construct immutable remote exception instance.
	 * 
	 * @param target exception root cause.
	 */
	public RemoteException(Throwable target) {
		this.cause = target.getClass().getCanonicalName();
		this.message = target.getMessage();
	}

	/**
	 * Get the class of the exception that causes this remote exception.
	 * 
	 * @return exception cause class.
	 */
	public String getCause() {
		return cause;
	}

	/**
	 * Get exception message.
	 * 
	 * @return exception message.
	 */
	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return Strings.concat(this.cause, ": ", this.message);
	}
}
