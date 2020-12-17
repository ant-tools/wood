package js.tools.commons.rmi;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Remote instance conneciton factory.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public interface ConnectionFactory
{
  HttpURLConnection openConnection(URL url) throws IOException;
}
