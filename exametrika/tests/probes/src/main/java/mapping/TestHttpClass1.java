package mapping;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

/**
 * {@link TestHttpClass1} tests request mapping.
 *
 * @author medvedev
 */
public class TestHttpClass1 {
    private static RequestInequalityEstimator estimator = new RequestInequalityEstimator();

    public static void main(String[] args) throws Throwable {
        Thread client = new Thread(new HttpClient(), "client");
        Thread server = new Thread(new HttpServer(args[0]), "server");
        client.start();
        server.start();
        client.join();
        server.join();
    }

    private static class HttpClient implements Runnable {
        private static final int COUNT = 100;

        @Override
        public void run() {
            try {
                Thread.sleep(2000);

                for (int i = 0; i < 10000; i++) {
                    long t = System.currentTimeMillis();

                    post();

                    t = System.currentTimeMillis() - t;

                    System.out.println("---------------- " + i + ": " + t);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        private void post() throws Throwable {
            for (int i = 0; i < COUNT; i++)
                doPost1(i);
        }

        private void doPost1(int i) throws Throwable {
            int a = i / 50, b = i / 25, c = i / 10, d = i;//2-4-10-100
            URL url = new URL(MessageFormat.format("http://localhost:8180/test/a-{0}/b-{1}?c={2}&d={3}", a, b, c, d));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            out.write(Integer.toString(i) + "\n");
            for (int k = 0; k < 10; k++)
                out.write("Hello world!!!");
            out.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            connection.getContentLength();

            while (in.readLine() != null)
                ;
            in.close();
        }

    }

    private static class TestServlet extends HttpServlet {
        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            estimator.addRequest(request.getRequestURI() + "?" + request.getQueryString());

            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
            int i = Integer.parseInt(in.readLine());
            handle(i);

            while (in.readLine() != null)
                ;
            in.close();

            Writer writer = new OutputStreamWriter(response.getOutputStream());
            for (int k = 0; k < 10; k++)
                writer.write("Hello world!!!");
            writer.close();
        }

        private void handle(int i) {
            if (i == 99)
                sleep(1000);
            else if (i == 50 || i == 25)
                sleep(100);
            else if (i >= 80 && i < 90)
                sleep(10);
            else if (i >= 0 && i < 50)
                sleep(1);
        }

        private void sleep(long duration) {
            try {
                Thread.sleep(duration);
            } catch (Exception e) {
            }
        }
    }

    private static class HttpServer implements Runnable {
        private Tomcat server;
        private final String workPath;

        public HttpServer(String workPath) {
            this.workPath = workPath;
        }

        @Override
        public void run() {
            try {
                test();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        public void test() throws Throwable {
            createServer(workPath);
            System.in.read();
            destroyServer();
        }

        private void createServer(String workPath) {
            Tomcat server = new Tomcat();

            Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
            connector.setPort(8180);

            connector.setAttribute("compression", "on");

            server.setConnector(connector);
            server.getService().addConnector(connector);

            File workFile = new File(workPath, "test");
            workFile.mkdirs();
            Context context = server.addContext("/test", workPath);
            context.setDisplayName("testApp");

            Tomcat.addServlet(context, "testServlet", new TestServlet());
            context.addServletMapping("/", "testServlet");

            server.setBaseDir(workPath);

            try {
                server.init();
                server.start();
            } catch (LifecycleException e) {
                throw new RuntimeException(e);
            }

            this.server = server;
        }

        private void destroyServer() {
            try {
                server.stop();
                server.destroy();
            } catch (LifecycleException e) {
                throw new RuntimeException(e);
            }

            server = null;
        }
    }
}