package js.tools.commons.rmi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import js.tools.commons.json.JSON;
import js.tools.commons.util.Files;

/**
 * JSON reader for remote method returned value.
 * 
 * @author Iulian Rotaru
 * @since 1.8
 * @version draft
 */
final class JsonValueReader implements ValueReader
{
  @Override
  public Object read(InputStream inputStream, Type returnType) throws IOException
  {
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
    try {
      return JSON.parse(reader, returnType);
    }
    finally {
      Files.close(reader);
    }
  }
}