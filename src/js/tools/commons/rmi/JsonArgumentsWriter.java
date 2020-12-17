package js.tools.commons.rmi;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import js.tools.commons.json.JSON;
import js.tools.commons.util.Files;

/**
 * Client side JSON writer for remote method parameters.
 * 
 * @author Iulian Rotaru
 * @since 1.8
 * @version draft
 */
final class JsonArgumentsWriter implements ArgumentsWriter
{
  @Override
  public boolean isSynchronous()
  {
    return false;
  }

  @Override
  public String getContentType()
  {
    return "application/json";
  }

  @Override
  public void write(OutputStream outputStream, Object[] arguments) throws IOException
  {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
    try {
      JSON.stringify(writer, arguments);
    }
    finally {
      Files.close(writer);
    }
  }
}