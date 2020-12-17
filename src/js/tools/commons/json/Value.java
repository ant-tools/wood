package js.tools.commons.json;

import java.lang.reflect.Type;

/**
 * Parser value helper. First, {@link Parser} uses {@link #instance()} method to retrieve wrapped value then initialize it from
 * JSON using {@link #set(Object)}. For array / collection setter method is called repetitively.
 * 
 * @author Iulian Rotaru
 */
abstract class Value {
	/**
	 * Retrieve Java object / array / collection instance wrapped this this parser value.
	 * 
	 * @return value instance.
	 */
	abstract Object instance();

	/**
	 * Get Java object type or array / collection component type.
	 * 
	 * @return value type.
	 */
	abstract Type getType();

	/**
	 * Value setter or array / collection items collector. For primitive value this method is called once with parsed value
	 * whereas for array or collection this method is called iteratively for every parsed element.
	 * 
	 * @param value value to set / collect.
	 */
	abstract void set(Object value);
}
