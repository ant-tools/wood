package js.tools.commons.json;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * JSON stream character reader with unread and error reporter. This helper class is used by {@link Lexer} to traverse JSON
 * characters stream, one character at a time. CharReader takes care to update {@link ErrorReporter} while retrieve characters.
 * 
 * @author Iulian Rotaru
 * @since 1.0.2
 */
final class CharReader implements Closeable {
	/** Mark value for undefined character. */
	private static final char UNDEFINED = 0;

	/** Wrapped JSON characters stream. */
	private Reader reader;

	/** Last unread character or undefined if {@link #unread(char)} was not called. */
	private char unreadChar = UNDEFINED;

	/** True if characters stream reached its end. */
	private boolean eof;

	/**
	 * Construct character reader instance.
	 * 
	 * @param reader wrapped JSON characters stream.
	 */
	CharReader(Reader reader) {
		this.reader = reader instanceof BufferedReader ? reader : new BufferedReader(reader);
	}

	/**
	 * Retrieve next character from JSON characters stream or undefined if EOF reached.
	 * 
	 * @return next character or undefined if EOF reached.
	 * @throws IOException if read operation fails.
	 */
	char next() throws IOException {
		if (unreadChar != UNDEFINED) {
			char c = unreadChar;
			this.unreadChar = UNDEFINED;
			return c;
		}

		int i = reader.read();
		if (i == -1) {
			eof = true;
			return UNDEFINED;
		}
		char c = (char) i;
		return c;
	}

	/**
	 * Retrieve next character throwing exception if EOF.
	 * 
	 * @return next character from JSON characters stream.
	 * @throws IOException if read operation fails.
	 */
	char require() throws IOException {
		char c = next();
		if (c == UNDEFINED) {
			throw new JsonException("Cannot retrieve required character because of premature stream end.");
		}
		return c;
	}

	/**
	 * Retrieve next not white space character.
	 * 
	 * @return next not white space character.
	 * @throws IOException if read operation fails.
	 */
	public char require(char c) throws IOException {
		while (Character.isWhitespace(c)) {
			c = require();
		}
		return c;
	}

	public void skipWhiteSpaces() throws IOException {
		char c = 0;
		do {
			c = next();
		} while (Character.isWhitespace(c));
		if (c != UNDEFINED) {
			unread(c);
		}
	}

	/**
	 * Put given character back to this character reader. Next {@link #next()} or {@link #require()} is guaranteed to return
	 * this pushed back character.
	 * 
	 * @param c char to put back to reader.
	 */
	void unread(char c) {
		unreadChar = c;
	}

	/**
	 * Test if character reader reaches EOF.
	 * 
	 * @return true if this character reader reaches EOF.
	 */
	boolean eof() {
		return eof;
	}

	/** Close this character reader. */
	public void close() throws IOException {
		reader.close();
	}
}
