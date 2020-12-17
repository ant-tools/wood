package js.tools.commons.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

/**
 * Generic array type with a specified component type. Component type is array item type; all items in an array has the same
 * component type.
 * 
 * @author Iulian Rotaru
 * @since 1.6
 * @version final
 */
public class GAType implements GenericArrayType {
	/** Array component type. */
	private Type componentType;

	/**
	 * Create generic array type for given component type.
	 * 
	 * @param componentType component type.
	 */
	public GAType(Type componentType) {
		this.componentType = componentType;
	}

	/**
	 * Get generic component type. It is dubbed 'generic' because indeed component type can be parameterized.
	 * 
	 * @return this generic array component type.
	 */
	@Override
	public Type getGenericComponentType() {
		return componentType;
	}
}
