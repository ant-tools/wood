package js.wood;

import java.util.HashMap;
import java.util.Map;

import js.dom.Document;
import js.dom.Element;
import js.lang.Pair;

/**
 * Default values for attributes used for attributes not explicitly set.
 * <p>
 * Here are currently implemented values.
 * <table>
 * <tr>
 * <td><b>Element
 * <td><b>Attribute
 * <td><b>Default Value
 * <tr>
 * <td>form
 * <td>method
 * <td>post
 * <tr>
 * <td>form
 * <td>enctype
 * <td>multipart/form-data
 * <tr>
 * <td>input
 * <td>type
 * <td>text
 * <tr>
 * <td>button
 * <td>type
 * <td>button
 * </table>
 */
public class DefaultAttributes
{

  private static Map<String, Pair[]> DEF_ATTRIBUTES = new HashMap<String, Pair[]>();
  static {
    DEF_ATTRIBUTES.put("form", new Pair[]
    {
        new Pair("enctype", "multipart/form-data"), //
        new Pair("method", "post")
    });

    DEF_ATTRIBUTES.put("input", new Pair[]
    {
      new Pair("type", "text")
    });

    DEF_ATTRIBUTES.put("button", new Pair[]
    {
      new Pair("type", "button")
    });
  }

  public static void update(Document doc)
  {
    for(Map.Entry<String, Pair[]> entry : DEF_ATTRIBUTES.entrySet()) {
      for(Element element : doc.findByTag(entry.getKey())) {
        for(Pair attr : entry.getValue()) {
          if(!element.hasAttr(attr.first())) {
            element.setAttr(attr.first(), attr.second());
          }
        }
      }
    }
  }
}
