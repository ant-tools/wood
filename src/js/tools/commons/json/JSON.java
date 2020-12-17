package js.tools.commons.json;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

public final class JSON
{
  private JSON()
  {
  }

  /**
   * Serialize value to JSON character stream and left it unclosed. Both primitive and aggregated values are allowed. If
   * value is not primitive all fields are scanned reflectively, less static, transient and synthetic. If a field is
   * aggregated on its turn traverse its fields too. This allows for not restricted fields graph. Note that there is no
   * guarantee regarding fields order inside parent object.
   * <p>
   * If <code>value</code> argument is null serialize JSON <code>null</code> keyword.
   * <p>
   * After serialization completes <code>writer</code> is flushed but left unclosed.
   * 
   * @param writer character stream to write value on,
   * @param value value to serialize, null accepted.
   * @throws IOException if IO write operation fails.
   */
  public static void stringify(Writer writer, Object value) throws IOException
  {
    Serializer serializer = new Serializer();
    serializer.serialize(writer, value);
  }

  /**
   * Deserialize value of expected type. After parsing completion used <code>reader</code> remains opened.
   * <p>
   * This method uses auto cast in order to simplify user code but is caller responsibility to ensure requested
   * <code>type</code> is cast compatible with type of variable to assign to.
   * <p>
   * This JSON parser method is relaxed and follows a best effort approach. If a named property from JSON stream does
   * not have the same name field into target object, parser just warn on log and ignore. Also, fields from target
   * object with no values into JSON stream are set to null.
   * 
   * @param reader character stream to read from,
   * @param type expected type.
   * @param <T> type to auto cast on return, cast compatible with <code>type</code> argument.
   * @return instance of expected type initialized from JSON character stream.
   * @throws IOException if read operation fails.
   * @throws JsonException if parsing process fails perhaps due to syntax violation on input.
   * @throws ClassCastException if given <code>type</code> cannot cast to expected type variable <code>T</code>.
   */
  public static <T> T parse(Reader reader, Type type) throws IOException, JsonException, ClassCastException
  {
    Parser parser = new Parser();
    return parser.parse(reader, type);
  }
}
