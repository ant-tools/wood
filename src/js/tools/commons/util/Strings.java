package js.tools.commons.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Strings manipulation utility. This utility class allows for sub-classing. See {@link js.util} for utility
 * sub-classing description.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public final class Strings
{
  /** Prevent default constructor synthesis. */
  private Strings()
  {
  }

  /**
   * Splits string using specified separator(s) or space if none given and returns trimmed values. Returns null if
   * string argument is null and empty list if is empty. This method supports a variable number of separator characters
   * - as accepted by {@link #isSeparator(char, char...)} predicate; if none given uses space.
   * 
   * @param string source string,
   * @param separators variable number of characters used as separators.
   * @return strings list, possible empty.
   */
  public static List<String> split(String string, char separator)
  {
    if(string == null) {
      return null;
    }

    final int length = string.length();
    final List<String> list = new ArrayList<String>();
    int beginIndex = 0;
    int endIndex = 0;

    while(endIndex < length) {
      if(string.charAt(endIndex) == separator) {
        if(endIndex > beginIndex) {
          list.add(string.substring(beginIndex, endIndex).trim());
        }
        beginIndex = ++endIndex;
      }
      ++endIndex;
    }
    if(beginIndex < length) {
      list.add(string.substring(beginIndex).trim());
    }
    return list;
  }

  /**
   * Join collection of objects, converted to string, using specified char separator. Returns null if given objects
   * array is null and empty if empty.
   * 
   * @param objects collection of objects to join,
   * @param separator character used as separator.
   * @return joined string.
   */
  public static String join(Iterable<?> objects, char separator)
  {
    return join(objects, Character.toString(separator));
  }

  /**
   * Join collection of objects, converted to string, using specified string separator.Concatenates strings from
   * collection converted to string but take care to avoid null items. Uses given separator between strings. Returns
   * null if given objects array is null and empty if empty. If separator is null uses space string instead, like
   * invoking {@link Strings#join(Iterable)}. Null objects or empty strings from given <code>objects</code> parameter
   * are ignored.
   * 
   * @param objects collection of objects to join,
   * @param separator string used as separator.
   * @return joined string.
   */
  public static String join(Iterable<?> objects, String separator)
  {
    if(objects == null) {
      return null;
    }
    if(separator == null) {
      separator = " ";
    }

    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for(Object object : objects) {
      if(object == null) {
        continue;
      }
      String value = object instanceof String ? (String)object : object.toString();
      if(value.isEmpty()) {
        continue;
      }
      if(first) {
        first = false;
      }
      else {
        builder.append(separator);
      }
      builder.append(value);
    }
    return builder.toString();
  }

  /**
   * Concatenates a variable number of objects, as strings. For every given argument, convert it to string using
   * {@link Object#toString()} overload and append to concatenated result. If a given argument happens to be null, skip
   * it. Return empty string if this method is invoked with no arguments at all.
   * 
   * @param objects variable number of objects.
   * @return concatenated objects.
   */
  public static String concat(Object... objects)
  {
    StringBuilder sb = new StringBuilder();
    for(Object object : objects) {
      if(object != null) {
        sb.append(object);
      }
    }
    return sb.toString();
  }

  /** Standard word separator. */
  private static final String SEPARATORS = " \t\r\n,:;({[<";

  /**
   * Return first word from a sentence. Search for first word separator and return substring before it. As word
   * separator uses space, tab, carriage return, line feed, point, comma, colon, semicolon and open parenthesis, all
   * round, curly, square and angular. Note that point must be followed by space to be considered word separator and
   * ellipsis is part of the word, like in Object... .
   * <p>
   * This method returns null if <code>sentence</code> argument is null and empty if empty.
   * 
   * @param sentence sentence to be scanned for its first word
   * @return sentence first word, null or empty.
   */
  public static String firstWord(String sentence)
  {
    if(sentence == null) {
      return null;
    }
    if(sentence.isEmpty()) {
      return sentence;
    }

    int length = sentence.length();
    int separatorIndex = 0;
    char c = sentence.charAt(0);
    String separators;
    boolean includeSeparator;
    switch(c) {
    case '(':
      separators = ")";
      separatorIndex = 1;
      includeSeparator = true;
      break;
    case '{':
      separators = "}";
      separatorIndex = 1;
      includeSeparator = true;
      break;
    case '[':
      separators = "]";
      separatorIndex = 1;
      includeSeparator = true;
      break;
    case '<':
      separators = ">";
      separatorIndex = 1;
      includeSeparator = true;
      break;
    default:
      separators = SEPARATORS;
      includeSeparator = false;
    }

    outerloop: for(; separatorIndex < length; ++separatorIndex) {
      c = sentence.charAt(separatorIndex);
      if(c == '.') {
        int nextIndex = separatorIndex + 1;
        if(nextIndex == length) break;
        boolean ellipsis = false;
        while(sentence.charAt(nextIndex) == '.') { // ellipsis is part to the word, e.g. Object...
          if(++nextIndex == length) break outerloop;
          ellipsis = true;
          ++separatorIndex;
        }
        if(separators.indexOf(sentence.charAt(nextIndex)) != -1) {
          if(ellipsis) ++separatorIndex;
          break;
        }
        continue;
      }
      if(separators.indexOf(c) != -1) break;
    }
    if(includeSeparator) ++separatorIndex;
    return sentence.substring(0, separatorIndex);
  }

  /** Escaped white spaces. This include standard white spaces, dot, comma, colon and semicolon. */
  private static final String WHITE_SPACES = " \t\r\n.,:;";

  /**
   * Remove first word from a sentence. First word is recognized using the heuristic from {@link #firstWord(String)}.
   * White spaces after first word are also trimmed. For the purpose of this function white spaces are: space, tab,
   * carriage return, line feed, point, comma, colon and semicolon.
   * <p>
   * This method returns null if <code>sentence</code> argument is null and empty if empty.
   * 
   * @param sentence sentence to remove first word from.
   * @return the sentence with first word removed.
   */
  public static String removeFirstWord(String sentence)
  {
    if(sentence == null) {
      return null;
    }
    if(sentence.isEmpty()) {
      return sentence;
    }

    String firstWord = firstWord(sentence);
    if(firstWord.length() == sentence.length()) return sentence;

    int i = firstWord.length();
    for(; i < sentence.length(); i++) {
      if(WHITE_SPACES.indexOf(sentence.charAt(i)) == -1) break;
    }
    return sentence.substring(i);
  }

  /**
   * Return the first sentence of a string, where a sentence ends with a sentence separator followed be white space.
   * Returns null if <code>text</code> parameter is null. Current version recognizes next sentence separators: dot (.),
   * question mark (?), exclamation mark (!) and semicolon (;).
   * 
   * @param text string to scan for first sentence.
   * @return first sentence from text.
   */
  public static String firstSentence(String text)
  {
    if(text == null) {
      return null;
    }
    int length = text.length();
    boolean sentenceSeparator = false;

    for(int i = 0; i < length; i++) {
      switch(text.charAt(i)) {
      case '.':
      case '!':
      case '?':
      case ';':
        sentenceSeparator = true;
        break;

      case ' ':
      case '\t':
      case '\n':
      case '\r':
      case '\f':
        if(sentenceSeparator) return text.substring(0, i);
        break;

      default:
        sentenceSeparator = false;
      }
    }
    return text;
  }

  /**
   * Get last string sequence following given character. If separator character is missing from the source string
   * returns entire string. Return null if string argument is null and empty if empty.
   * 
   * @param string source string,
   * @param separator character used as separator.
   * @return last string sequence.
   */
  public static String last(String string, char separator)
  {
    if(string == null) {
      return null;
    }
    if(string.isEmpty()) {
      return "";
    }
    return string.substring(string.lastIndexOf(separator) + 1);
  }

  /**
   * Convert dash separated words to Java class member name. Note that first character of returned member name is lower
   * case, e.g. <code>this-is-a-string</code> is converted to <code>thisIsAString</code>.
   * <p>
   * Returns null if words argument is null and empty if empty.
   * 
   * @param words string to convert.
   * @return camel case member name.
   */
  public static String toMemberName(String words)
  {
    if(words == null) {
      return null;
    }
    if(words.isEmpty()) {
      return "";
    }

    String[] parts = words.split("-+");
    StringBuilder sb = new StringBuilder(parts[0]);

    for(int i = 1; i < parts.length; i++) {
      assert parts[i].length() > 0;
      sb.append(Character.toUpperCase(parts[i].charAt(0)));
      sb.append(parts[i].substring(1));
    }
    return sb.toString();
  }

  /**
   * Load string from UTF-8 file content.
   * 
   * @param file source file,
   * @param maxCount optional maximum character count to load, default to entire file.
   * @return loaded string.
   * @throws IOException if file not found or file read operation fails.
   */
  public static String load(File file, Integer... maxCount) throws IOException {
      return load(new FileReader(file), maxCount);
  }

  /**
   * Load string from character stream then closes it.
   * 
   * @param reader source character stream.
   * @param maxCount optional maximum character count to load, default to entire file.
   * @return loaded string.
   * @throws IOException if read operation fails.
   */
  public static String load(Reader reader, Integer... maxCount) throws IOException {
      long maxCountValue = maxCount.length > 0 ? maxCount[0] : Long.MAX_VALUE;
      StringWriter writer = new StringWriter();

      try {
          char[] buffer = new char[1024];
          for (;;) {
              int readChars = reader.read(buffer, 0, (int) Math.min(buffer.length, maxCountValue));
              if (readChars <= 0) {
                  break;
              }
              writer.write(buffer, 0, readChars);
              maxCountValue -= readChars;
          }
      } finally {
          Files.close(reader);
          Files.close(writer);
      }

      return writer.toString();
  }
}
