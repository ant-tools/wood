package js.tools.commons.json;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable lexer token.
 * 
 * @author Iulian Rotaru
 */
final class Token {
	static final int NONE = 0;
	static final int LEFT_BRACE = 1;
	static final int RIGHT_BRACE = 2;
	static final int LEFT_SQUARE = 3;
	static final int RIGHT_SQUARE = 4;
	static final int NAME = 5;
	static final int VALUE = 6;
	static final int COLON = 7;
	static final int ITEM = 8;
	static final int COMMA = 9;
	static final int EOF = 10;

	/** Token enumeration ordinal value. */
	private final int ordinal;

	/** Stores JSON token related value. It is a string with enclosing quote marks trimmed, if the case. */
	private final String value;

	/**
	 * Construct a token with null value.
	 * 
	 * @param ordinal token enumeration ordinal value.
	 */
	public Token(int ordinal) {
		this.ordinal = ordinal;
		this.value = null;
	}

	/**
	 * Construct a token with given string value.
	 * 
	 * @param ordinal token enumeration ordinal,
	 * @param value token string value.
	 */
	public Token(int ordinal, String value) {
		this.ordinal = ordinal;
		this.value = value;
	}

	/**
	 * Return token ordinal.
	 * 
	 * @return token ordinal.
	 * @see #ordinal
	 */
	public int ordinal() {
		return ordinal;
	}

	/**
	 * Get token value.
	 * 
	 * @return token value.
	 * @see #value
	 */
	public String value() {
		return value;
	}

	@Override
	public String toString() {
		return names.get(this.ordinal);
	}

	private static final Map<Integer, String> names = new HashMap<Integer, String>();
	static {
		try {
			for (Field field : Token.class.getDeclaredFields()) {
				int modifiers = field.getModifiers();
				if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && Character.isUpperCase(field.getName().charAt(0))) {
					field.setAccessible(true);
					names.put(field.getInt(null), field.getName());
				}
			}
		} catch (IllegalAccessException unused) {
			// is not possible to have illegal access on field with accessibility true
		}
	}
}
