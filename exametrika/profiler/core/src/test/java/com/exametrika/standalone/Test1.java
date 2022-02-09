import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Test1 {
    public static void main(String[] args) {
    }

    public static void main1(String[] args) {
        while (true) {
            HttpURLConnection connection = null;

            try {
                URL serverAddress = new URL("http://localhost:8080/examples/servlets/servlet/RequestInfoExample");

                connection = (HttpURLConnection) serverAddress.openConnection();
                connection.setRequestMethod("GET");
                //connection.setDoOutput(true);
                //connection.setReadTimeout(10000);

                //connection.connect();

                // get the output stream writer and write the output to the server
                // not needed in this example
                // wr = new OutputStreamWriter(connection.getOutputStream());
                // wr.write("");
                // wr.flush();

                // read the result from the server
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder builder = new StringBuilder();

                while (true) {
                    String line = reader.readLine();
                    if (line == null)
                        break;

                    builder.append(line + '\n');
                }

                System.out.println(builder.toString());
            } catch (Exception e) {
                e.printStackTrace();
                break;
            } finally {
                //connection.disconnect();
            }
        }
    }
}
