package js.tools.commons.json;

import java.lang.reflect.Type;

/**
 * NOP implementation for missing fields. This helper is a special object value helper used by parser when a property from JSON
 * stream has no related field on target object.
 * 
 * @author Iulian Rotaru
 */
public class MissingFieldValue extends ObjectValue {
	@Override
	public Object instance() {
		return null;
	}

	@Override
	public Type getType() {
		return null;
	}

	@Override
	public Type getValueType() {
		return null;
	}

	@Override
	public void set(Object value) {
	}

	@Override
	public void setValue(Object value) {
	}
}
