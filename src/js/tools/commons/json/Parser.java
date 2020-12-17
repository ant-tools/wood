package js.tools.commons.json;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

import js.tools.commons.util.Types;

final class Parser
{
  /** Morphological parser. */
  private Lexer lexer;

  /** Current state for parser automata. */
  private State state;

  /** Create parser instance and initialize automata state. */
  public Parser()
  {
    this.state = State.NONE;
  }

  public <T> T parse(Reader reader, Type type) throws IOException
  {
    lexer = new Lexer(reader);
    try {
      return _parse(type);
    }
    catch(IOException e) {
      throw e;
    }
    catch(JsonException e) {
      throw e;
    }
    catch(Throwable t) {
      throw new JsonException(t);
    }
  }

  /**
   * Create instance or expected <code>type</code>, parse tokens from internal lexer and initialize instance fields. It is
   * caller responsibility to ensure JSON stream describe an instance compatible to expected type.
   * <p>
   * Note that if type is strict object named fields from JSON stream are searched on superclass too.
   * 
   * @param type expected type.
   * @return newly created instance of requested type.
   * @throws JsonParserException if lexer fails to decode JSON character stream.
   * @throws IOException if IO read operation fails.
   * @throws SecurityException 
   * @throws NoSuchFieldException 
   */
  @SuppressWarnings("unchecked")
  private <T> T _parse(Type type) throws JsonException, IOException, NoSuchFieldException, SecurityException {
      Value value = getValueInstance(type);
      Token token = null;

      TOKENS_LOOP: for (;;) {
          token = lexer.read();

          switch (state) {

          case NONE:
              switch (token.ordinal()) {
              case Token.VALUE:
                  // item is used here to support multiple types parsing
                  // multiple types are actually a JSON array but every item with its own type
              case Token.ITEM:
                  value.set(token.value());
                  break TOKENS_LOOP;

              case Token.LEFT_BRACE:
                  if (value instanceof MapValue) {
                      state = State.WAIT_FOR_KEY;
                  } else {
                      state = State.WAIT_FOR_NAME_OR_CLASS;
                  }
                  continue;

              case Token.LEFT_SQUARE:
                  state = State.WAIT_FOR_ITEM;
                  continue;

                  // array end token on parser state none means empty JSON array; break parsing loop
              case Token.RIGHT_SQUARE:
                  lexer.unread(token);
                  break TOKENS_LOOP;

              case Token.EOF:
                  throw new JsonException("Closed reader. No data available for parsing.");

              default:
                  throw new JsonException("Invalid start token %s.", token, this.state);
              }

          case WAIT_FOR_NAME_OR_CLASS:
              if (token.ordinal() == Token.NAME) {
                  if ("class".equals(token.value())) {
                      if (value != null) {
                          throw new JsonException("Illegal state. User requested type argument on JSON with inbound class.");
                      }
                      token = lexer.read();
                      if (token.ordinal() != Token.COLON) {
                          throw new JsonException("Expected COLON but got %s.", token);
                      }
                      token = lexer.read();

                      value = getValueInstance(loadClass(token.value()));
                      state = State.WAIT_FOR_COMMA_OR_RIGHT_BRACE;
                      continue;
                  }
              }
              // fall through WAIT_FOR_NAME case

          case WAIT_FOR_NAME:
              switch (token.ordinal()) {
              case Token.RIGHT_BRACE: // empty object
                  break TOKENS_LOOP;

              case Token.NAME:
                  if (!(value instanceof ObjectValue)) {
                      throw new JsonException("Invalid value helper |%s| for target type |%s|.", value.getClass(), type);
                  }
                  ((ObjectValue) value).setFieldName(token.value());
                  state = State.WAIT_FOR_COLON;
                  continue;

              default:
                  throw new JsonException("Invalid token |%s| while waiting for a name.", token);
              }

          case WAIT_FOR_COLON:
              if (token.ordinal() != Token.COLON) {
                  throw new JsonException("Expected COLON but got |%s|.", token);
              }
              state = State.WAIT_FOR_VALUE;
              continue;

          case WAIT_FOR_VALUE:
              if (!(value instanceof ObjectValue)) {
                  throw new JsonException("Invalid value helper |%s| for target type |%s|.", value.getClass(), type);
              }
              final ObjectValue objectValue = (ObjectValue) value;
              switch (token.ordinal()) {
              case Token.LEFT_BRACE:
              case Token.LEFT_SQUARE:
                  state = State.NONE;
                  lexer.unread(token);
                  objectValue.setValue(_parse(objectValue.getValueType()));
                  state = State.WAIT_FOR_COMMA_OR_RIGHT_BRACE;
                  continue;

              case Token.VALUE:
                  objectValue.setValue(token.value());
                  state = State.WAIT_FOR_COMMA_OR_RIGHT_BRACE;
                  continue;

              default:
                  throw new JsonException("Expect VALUE, LEFT_BRACE or LEFT_SQUARE but got %s.", token);
              }

          case WAIT_FOR_COMMA_OR_RIGHT_BRACE:
              if (token.ordinal() == Token.COMMA) {
                  if (value instanceof MapValue) {
                      state = State.WAIT_FOR_KEY;
                  } else {
                      state = State.WAIT_FOR_NAME;
                  }
                  continue;
              }
              if (token.ordinal() != Token.RIGHT_BRACE) {
                  throw new JsonException("Expected RIGHT_BRACE but got %s. Maybe missing comma.", token);
              }
              break TOKENS_LOOP;

          case WAIT_FOR_KEY:
              if (!(value instanceof MapValue)) {
                  throw new JsonException("Invalid value helper |%s| for target type |%s|.", value.getClass(), type);
              }
              final MapValue mapValue = (MapValue) value;
              switch (token.ordinal()) {
              case Token.LEFT_BRACE:
                  state = State.NONE;
                  lexer.unread(token);
                  mapValue.setKey(_parse(mapValue.keyType()));
                  state = State.WAIT_FOR_COLON;
                  break;

              case Token.RIGHT_BRACE: // empty map
                  break TOKENS_LOOP;

              case Token.NAME:
                  mapValue.setKey(token.value());
                  state = State.WAIT_FOR_COLON;
                  break;

              default:
                  throw new JsonException("Unexpected token |%s| while waiting for map key.", token);
              }
              break;

          case WAIT_FOR_ITEM:
              switch (token.ordinal()) {
              case Token.LEFT_BRACE: // object inside array
              case Token.LEFT_SQUARE: // array inside array
                  state = State.NONE;
                  lexer.unread(token);
                  value.set(_parse(value.getType()));
                  state = State.WAIT_FOR_COMMA_OR_RIGHT_SQUARE;
                  continue;

              case Token.RIGHT_SQUARE: // empty array
                  break TOKENS_LOOP;

              case Token.ITEM:
                  value.set(token.value());
                  state = State.WAIT_FOR_COMMA_OR_RIGHT_SQUARE;
                  continue;

              default:
                  throw new JsonException("Expect ITEM, LEFT_BRACE or LEFT_SQUARE but got %s.", token);
              }
              // fall through WAIT_FOR_COMMA_OR_RIGHT_SQUARE case

          case WAIT_FOR_COMMA_OR_RIGHT_SQUARE:
              if (token.ordinal() == Token.COMMA) {
                  state = State.WAIT_FOR_ITEM;
                  continue;
              }
              if (token.ordinal() != Token.RIGHT_SQUARE) {
                  throw new JsonException("Expected RIGHT_SQUARE but got %s. Maybe missing comma.", token);
              }
              break TOKENS_LOOP;
          }
      }

      state = State.NONE;
      return (T) value.instance();
  }

  /**
   * Get parser value helper instance suitable for handling the given type.
   * 
   * @param type type to get value helper for.
   * @return value helper instance.
   */
  private Value getValueInstance(Type type) {
      if (type == null) {
          return new MissingFieldValue();
      }
      if (Types.isArray(type)) {
          return new ArrayValue(type);
      }
      if (Types.isCollection(type)) {
          return new CollectionValue(type);
      }
      if (Types.isPrimitiveLike(type)) {
          return new PrimitiveValue((Class<?>) type);
      }
      if (Types.isMap(type)) {
          return new MapValue(type);
      }

      // at this point type should denote a not parameterized strict object
      if (!(type instanceof Class)) {
          throw new JsonException("Illegal state. Generic objects |%s| are not supported.", type);
      }
      return new ObjectValue((Class<?>) type);
  }

  /**
   * Load named class.
   * 
   * @param className qualified class name.
   * @return class singleton.
   * @throws JsonParserException if class not found.
   */
  @SuppressWarnings("unchecked")
  private static <T> Class<T> loadClass(String className) {
      ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
      try {
          return (Class<T>) Class.forName(className, true, currentThreadClassLoader);
      } catch (ClassNotFoundException unused1) {
          if (!currentThreadClassLoader.equals(Parser.class.getClassLoader())) {
              try {
                  return (Class<T>) Class.forName(className, true, Parser.class.getClassLoader());
              } catch (ClassNotFoundException unused2) {
                  throw new JsonException("JSON requested class |%s| not found.", className);
              }
          }
      }
      return null;
  }

  /**
   * Parser state machine.
   * 
   * @author Iulian Rotaru
   * @since 1.1
   */
  private static enum State
  {
    NONE, WAIT_FOR_NAME, WAIT_FOR_NAME_OR_CLASS, WAIT_FOR_ITEM, WAIT_FOR_KEY, WAIT_FOR_COLON, WAIT_FOR_VALUE, WAIT_FOR_COMMA_OR_RIGHT_BRACE, WAIT_FOR_COMMA_OR_RIGHT_SQUARE
  }
}
