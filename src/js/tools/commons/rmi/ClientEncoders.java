package js.tools.commons.rmi;

import java.net.HttpURLConnection;

import js.tools.commons.util.Params;

/**
 * Factory for encoders user by HTTP-RMI client transactions.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public final class ClientEncoders {
	private static ClientEncoders instance = new ClientEncoders();

	public static ClientEncoders getInstance() {
		return instance;
	}

	/**
	 * Factory method for arguments writers. Current implementation choose the {@link ArgumentsWriter} implementation based on
	 * given arguments list. It is caller responsibility to ensure arguments order and types match remote method signature.
	 * <p>
	 * Heuristic to determine arguments writer:
	 * <ul>
	 * <li>uses {@link StreamArgumentsWriter} if there is a single argument of type {@link StreamHandler},
	 * <li>uses {@link MixedArgumentsWriter} if there are more documents and/or streams present,
	 * <li>otherwise uses {@link JsonArgumentsWriter}.
	 * </ul>
	 * 
	 * @param arguments invocation arguments list, not null or empty.
	 * @return parameters encoder instance.
	 * @throws IllegalArgumentException if given arguments list is null or empty.
	 */
	public ArgumentsWriter getArgumentsWriter(Object[] arguments) {
		Params.notNullOrEmpty(arguments, "Arguments");

		int streams = 0;
		for (Object argument : arguments) {
			if (argument instanceof StreamHandler) {
				++streams;
			}
		}

		if (arguments.length == 1) {
			if (streams != 0) {
				return new StreamArgumentsWriter();
			}
		}
		if (streams != 0) {
			return new MixedArgumentsWriter();
		}

		return new JsonArgumentsWriter();
	}

	public ValueReader getValueReader(HttpURLConnection connection) {
		String contentType = connection.getContentType();
		if (contentType != null) {
			int parametersSeparatorIndex = contentType.indexOf(';');
			if (parametersSeparatorIndex != -1) {
				contentType = contentType.substring(0, parametersSeparatorIndex);
			}
			contentType = contentType.toLowerCase().trim();
			if (contentType.equals("application/json")) {
				return new JsonValueReader();
			}
			if (contentType.equals("application/octet-stream")) {
				return new StreamValueReader();
			}
		}
		return new UnsupportedContentTypeValueReader();
	}
}
