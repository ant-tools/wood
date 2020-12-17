package js.tools.commons.rmi;

import java.net.URL;
import java.rmi.RemoteException;

/**
 * HTTP-RMI runtime exception.
 * 
 * @author Iulian Rotaru
 * @since 1.7
 * @version draft
 */
public class RmiException extends RuntimeException {
	/** Java serialization version. */
	private static final long serialVersionUID = 9037909480686795160L;

	public RmiException(URL remoteMethodURL, RemoteException remoteException) {
		super(String.format("HTTP-RMI server execution error on |%s|: %s", remoteMethodURL, remoteException));
	}

	public RmiException(URL remoteMethodURL, String remoteException) {
		super(String.format("HTTP-RMI server execution error on |%s|: %s", remoteMethodURL, remoteException));
	}

	public RmiException(String message, Object... args) {
		super(String.format(message, args));
	}
}
