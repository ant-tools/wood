package js.tools.commons.json;

import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

/**
 * Morphological parser. This class deals with words, aka. tokens; reads from characters stream and return specific
 * {@link Token} instances. It is the first stage from deserialization process. On the second, resulting tokens are
 * syntactically analyzed by the {@link Parser}.
 * <p>
 * This class takes care of string escaping, although not by itself but delegating {@link LexerValueBuilder} - used by
 * {@link #collect(char)} helper method.
 * 
 * @author Iulian Rotaru
 * @since 1.1
 */
final class Lexer {
	private static final Token TOKEN_EOF = new Token(Token.EOF);
	private static final Token TOKEN_LEFT_BRACE = new Token(Token.LEFT_BRACE);
	private static final Token TOKEN_RIGHT_BRACE = new Token(Token.RIGHT_BRACE);
	private static final Token TOKEN_LEFT_SQUARE = new Token(Token.LEFT_SQUARE);
	private static final Token TOKEN_RIGHT_SQUARE = new Token(Token.RIGHT_SQUARE);
	private static final Token TOKEN_COLON = new Token(Token.COLON);
	private static final Token TOKEN_COMMA = new Token(Token.COMMA);

	/** Character reader instance created outside lexer. */
	private CharReader reader;

	/** Lexer automata current state. It it initialized for primitives values processing. */
	private State state = State.PRIMITIVE;

	/** Stack for current processing token before entering inner objects and arrays. */
	private Stack<State> statesStack = new Stack<State>();

	/** Stack of unreaded tokens. */
	private Stack<Token> unreadTokens = new Stack<Token>();

	/** Token value builder. */
	private LexerValueBuilder builder = new LexerValueBuilder();

	/**
	 * Package private constructor.
	 * 
	 * @param reader input characters stream.
	 */
	Lexer(Reader reader) {
		this.reader = new CharReader(reader);
	}

	/**
	 * Read next token from characters stream.
	 * 
	 * @return next token from characters stream.
	 * @throws IOException if reading from input characters stream fails.
	 * @throws JsonParserException if characters stream morphological structure is violated.
	 */
	Token read() throws IOException, JsonException {
		if (state == State.EOF) {
			throw new JsonException("Attempt to read tokens after stream end.");
		}
		if (!unreadTokens.isEmpty()) {
			return unreadTokens.pop();
		}

		// excerpt for json.org: Whitespace can be inserted between any pair of tokens.
		char c = nextNonWhiteSpace();

		if (reader.eof()) {
			state = State.EOF;
			return TOKEN_EOF;
		}

		switch (c) {
		case '{':
			statesStack.push(state);
			state = State.NAME;
			return TOKEN_LEFT_BRACE;

		case '}':
			state = statesStack.pop();
			return TOKEN_RIGHT_BRACE;

		case '[':
			statesStack.push(state);
			state = State.ITEM;
			return TOKEN_LEFT_SQUARE;

		case ']':
			state = statesStack.pop();
			return TOKEN_RIGHT_SQUARE;

		case ':':
			state = State.VALUE;
			return TOKEN_COLON;

		case ',':
			state = state == State.VALUE ? State.NAME : State.ITEM;
			return TOKEN_COMMA;

		default:
			String string = collect(c);
			Token token = null;
			switch (state) {
			case NAME:
				token = new Token(Token.NAME, string);
				return token;

			case PRIMITIVE:
			case VALUE:
				token = new Token(Token.VALUE, string);
				return token;

			case ITEM:
				token = new Token(Token.ITEM, string);
				return token;

			default:
				throw new JsonException("Invalid lexer state |%s| when collecting string.", this.state);
			}
		}
	}

	/**
	 * Put back the token on characters stream. Actually token is pushed to {@link #unreadTokens} stack but overall lexer
	 * behavior is like pushing back to reader.
	 * 
	 * @param token token to put back.
	 */
	void unread(Token token) {
		unreadTokens.push(token);
	}

	/**
	 * Predicate to test if input characters stream is ended.
	 * 
	 * @return true if wrapped characters stream is ended.
	 */
	boolean eof() {
		return reader.eof();
	}

	/**
	 * Get next no white space character or undefined value if EOF reached.
	 * 
	 * @return next not white space character from reader.
	 * @throws IOException if IO read operation fails.
	 */
	private char nextNonWhiteSpace() throws IOException {
		char c = 0;
		do {
			c = reader.next();
		} while (Character.isWhitespace(c));
		return c;
	}

	/**
	 * Start collecting token value characters, blocking till value complete. This method is invoked with first character from
	 * token value then enter an internal loop collecting all value characters. Uses {@link #builder} to accumulate characters.
	 * 
	 * @param c first token value character.
	 * @return token value.
	 * @throws IOException if IO read operation fails.
	 */
	private String collect(char c) throws IOException {
		builder.clear();
		c = reader.require(c);

		if (c == '"') {
			// collect till next quotation mark but takes care of escaped quotes
			char previousChar = c;
			for (;;) {
				c = reader.require();
				if (previousChar != '\\' && c == '"') {
					break;
				}
				builder.append(c);
				previousChar = c;
			}
			reader.skipWhiteSpaces();
			return builder.toString();
		}

		if (this.state == State.PRIMITIVE) {
			// here we have a primitive value other than string, processed above; collect all till end of stream
			// primitive values cannot contain white spaces
			// at this point c variable holds first character from primitive value - is guaranteed to not be white space
			for (; !reader.eof() && !Character.isWhitespace(c);) {
				builder.append(c);
				c = reader.next();
			}
			return builder.toString();
		}

		// collect till next right brace, right square, colon or comma; unread the string end mark
		FOR_LOOP: for (boolean whitespaceFound = false;;) {
			if (Character.isWhitespace(c)) {
				whitespaceFound = true;
				c = reader.require();
				continue;
			}
			switch (c) {
			case '}':
			case ']':
			case ':':
			case ',':
				reader.unread(c);
				break FOR_LOOP;

			default:
				if (whitespaceFound) {
					throw new JsonException("Invalid primitive value with white space.");
				}
				builder.append(c);
				c = reader.require();
			}
		}
		return builder.toString();
	}

	/**
	 * Lexer automata states.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.1
	 */
	private static enum State {
		NONE, PRIMITIVE, NAME, VALUE, ITEM, EOF
	}
}
