package js.wood;

/**
 * Operator names strategy. WOOD operators are standard X(HT)ML element attributes with predefined names. In order to
 * avoid name collision and accommodate developer style WOOD uses couple options for names format.
 * <p>
 * <table border="1" style="border-collapse:collapse;">
 * <tr>
 * <td><b>Constant
 * <td><b>Name
 * <td><b>Description
 * <tr>
 * <td>ATTR
 * <td>Attribute name
 * <td>Simple attribute name. Because it does not use prefix it is prone to name collision but is simple to use.
 * <tr>
 * <td>DATA_ATTR
 * <td>Custom attribute name
 * <td>Uses HTML custom attribute name, that is, prefixed with <code>data-</code>. This naming convention is a trade-off
 * between simplicity to use and avoiding name collisions.
 * <tr>
 * <td>XMLNS
 * <td>XML name space
 * <td>This is default naming strategy and offer a clear separation for WOOD operator name space. Anyway, add complexity
 * because name space should be declared with WOOD URI: <code>xmlns:wood="js-lib.com/wood"</code>.
 * </table>
 * <p>
 * Both build and preview tools have options to select desired naming strategy but selected naming convention is global
 * per project.
 * 
 * @author Iulian Rotaru
 */
public enum NamingStrategy
{
  /** Simple attribute name. Does not use prefix name and is prone to name collision but is simple to use. */
  ATTR,
  /**
   * HTML custom attribute name, that is, prefixed with <code>data-</code>. This naming convention is a trade-off
   * between simplicity to use and avoiding name collisions.
   */
  DATA_ATTR,
  /**
   * XML name space names. This is default naming strategy and offer a clear separation for WOOD operator name space.
   * Anyway, add complexity because name space should be declared with WOOD URI:
   * <code>xmlns:wood="js-lib.com/wood"</code>.
   */
  XMLNS
}
