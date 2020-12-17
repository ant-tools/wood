package js.wood;

import java.util.HashMap;
import java.util.Map;

import js.lang.Pair;
import js.util.Strings;

/**
 * Widget and template layout parameters. Layout parameters are loaded from <code>wood:param</code> operator and used by
 * {@link SourceReader} to inject values into layout, on the fly. Into layout source file, parameters are declared using
 * <code>@param/</code> reference. Parameter reference is text replaced with parameter value.
 * <p>
 * Below is an example widget layout with <code>caption</code> parameter. The number of parameter references is not
 * limited but all should be defined when reference the widget. Also a parameter references can be used multiple times
 * in a given layout. Because parameter references is text replaced with string value, it can appear into layout
 * anywhere a string is valid.
 * 
 * <pre>
 * &lt;div class="listview"&gt;
 *   &lt;h4 class="caption"&gt;@param/caption&lt;/h4&gt;
 *   ...
 * </pre>
 * <p>
 * When reference the widget, declares parameter values, of course if widget layout requires. All parameter references
 * from layout should be resolved. If more parameters, position into list is not relevant since parameters are passed by
 * name.
 * 
 * <pre>
 * &lt;div wood:widget="compo/list-view" wood:param="caption:Info Links"&gt;&lt;/div&gt;
 * </pre>
 * 
 * <p>
 * Parameters definition syntax is similar to inline CSS style. It is a list of parameters separated by semicolon and a
 * parameter is a name / value pair separated by colon. Spaces are allowed around separators.
 * 
 * <pre>
 * parameters = parameter *[PARAM_SEP parameter] PARAM_SEP
 * parameter = name NAME_SEP value
 * PARAM_SEP = *SP ';' *SP
 * NAME_SEP = *SP ':' *SP
 * ; SP = space
 * </pre>
 * 
 * @author Iulian Rotaru
 */
public class LayoutParameters
{
  /** Parameters storage. */
  private Map<String, String> parameters = new HashMap<String, String>();

  /**
   * Load layout parameters from parameters definition, possible null. If <code>parameters</code> argument is null this
   * method does nothing. Parameters definition format is similar inline CSS style, see class description.
   * 
   * @param parametersDefintion layout parameters definition, null accepted.
   */
  public void load(String parametersDefintion)
  {
    if(parametersDefintion == null) {
      return;
    }
    parameters.clear();
    for(Pair pair : Strings.splitPairs(parametersDefintion, ';', ':')) {
      parameters.put(pair.first(), Strings.escapeXML(pair.second()));
    }
  }

  /**
   * Get parameter value or null if named parameter does not exist.
   * 
   * @param name parameter name.
   * @return parameter value, possible null.
   */
  public String getValue(String name)
  {
    return parameters.get(name);
  }
}
