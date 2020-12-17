package js.tools.commons.rmi;

import java.io.IOException;
import java.io.OutputStream;

import js.tools.commons.util.Classes;

/**
 * Abstract stream handler for remote output streams for both server and client logic. When used on server, output stream is
 * download - that is, output from server and input on client; when used on client side, output stream is used for upload -
 * output from client and input on server. Please note that remote class <em>client</em> interface takes care to reverse input
 * and output streams. For a discussion about service provider interface see package description.
 * <p>
 * On server side upload just declare the method using desired input stream as parameters; additional parameters are allowed.
 * For download need to wrap returned stream into this stream handler - see <code>downloadStream()</code> from code sample.
 * 
 * <pre>
 * &#064;Remote
 * interface Controller {
 * 	void uploadStream(InputStream inputStream) throws IOException;
 * 
 * 	StreamHandler&lt;OutputStream&gt; downloadStream();
 * }
 * </pre>
 * 
 * <pre>
 * class ControllerImpl implements Controller {
 * 	public void uploadStream(InputStream inputStream) throws IOException {
 * 		StringWriter writer = new StringWriter();
 * 		Files.copy(new InputStreamReader(inputStream, &quot;UTF-8&quot;), writer);
 * 	}
 * 
 * 	public StreamHandler&lt;OutputStream&gt; downloadStream() {
 * 		return new StreamHandler&lt;OutputStream&gt;(OutputStream.class) {
 * 			&#064;Override
 * 			public void handle(OutputStream outputStream) throws IOException {
 * 				outputStream.write(&quot;output stream&quot;.getBytes(&quot;UTF-8&quot;));
 * 			}
 * 		};
 * 	}
 * }
 * </pre>
 * <p>
 * Client side logic uses service provider interface, see <code>Controller</code> interface from below code snippet.
 * <em>Client</em> interfaces should reverse streams declared by server interface used for implementation. In our example,
 * server <code>uploadStream</code> method has input stream whereas <em>client</em> interface has output stream, wrapped in
 * stream handler instance. For <code>downloadStream</code>, <em>client</em> replaces server output stream with input and
 * because is an input stream there is no need to use stream handler.
 * 
 * <pre>
 * // service provider interface reverse streams direction
 * interface Controller {
 * 	void uploadStream(StreamHandler&lt;FileOutputStream&gt; outputStream) throws IOException;
 * 
 * 	InputStream downloadStream();
 * }
 * </pre>
 * 
 * <pre>
 * Controller controller = Factory.getInstance(controllerURL, Controller.class);
 * 
 * // wrap output stream into StreamHandler instance
 * controller.uploadStream(new StreamHandler&lt;FileOutputStream&gt;(FileOutputStream.class) {
 * 	&#064;Override
 * 	protected void handle(FileOutputStream outputStream) throws IOException {
 * 		outputStream.write(&quot;output stream&quot;.getBytes(&quot;UTF-8&quot;));
 * 	}
 * });
 * 
 * InputStream inputStream = controller.downloadStream();
 * Files.read(inputStream, outputStream); // just uses input stream the usual way
 * </pre>
 * 
 * @author Iulian Rotaru
 * @param <T> wrapped output stream.
 * @since 1.8
 * @version draft
 */
public abstract class StreamHandler<T extends OutputStream> {
	/**
	 * Concrete stream handler should implement this method, usually to writing to given output stream.
	 * 
	 * @param outputStream output stream instance.
	 * @throws IOException if output stream writing fails.
	 */
	protected abstract void handle(T outputStream) throws IOException;

	/** Wrapped output stream class. */
	private Class<T> streamClass;

	/**
	 * Construct concrete stream handler instance wrapping given output stream.
	 * 
	 * @param streamClass the class of wrapped output stream.
	 */
	public StreamHandler(Class<T> streamClass) {
		this.streamClass = streamClass;
	}

	/**
	 * Get wrapped output stream class.
	 * 
	 * @return output stream class.
	 */
	public Class<T> getStreamClass() {
		return this.streamClass;
	}

	/**
	 * Helper method used to invoke concrete output stream handler method. Please note that given output stream is closed after
	 * handler invocation, just before this method return.
	 * 
	 * @param outputStream wrapped output stream instance.
	 * @throws IOException any output stream exception is bubbled up.
	 */
	public void invokeHandler(OutputStream outputStream) throws IOException {
		@SuppressWarnings("unchecked")
		T t = OutputStream.class.equals(this.streamClass) ? (T) outputStream : Classes.newInstance(streamClass, outputStream);
		handle(t);
		t.flush();
		t.close();
	}
}
