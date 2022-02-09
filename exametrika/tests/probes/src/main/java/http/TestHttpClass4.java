package http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import com.exametrika.common.utils.Profiler;

/**
 * {@link TestHttpClass4} tests stalled requests in {@link HttpServlet}.
 *
 * @author medvedev
 */
public class TestHttpClass4 {
    public static void main(String[] args) throws Throwable {
        Thread client = new Thread(new HttpClient(), "client");
        Thread server = new Thread(new HttpServer(args[0]), "server");
        client.start();
        server.start();
        client.join();
        server.join();
    }

    private static class HttpClient implements Runnable {
        private final URL urlStalledPost;
        private static final int COUNT = 1;

        public HttpClient() throws Throwable {
            urlStalledPost = new URL("http://localhost:8180/test/post/stalled");
        }

        @Override
        public void run() {
            try {
                Thread.sleep(2000);

                for (int i = 0; i < 10000; i++) {
                    long t = System.currentTimeMillis();

                    test();

                    t = System.currentTimeMillis() - t;

                    System.out.println("---------------- " + i + ": " + t);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        public void test() throws Throwable {
            post();
        }

        private void post() throws Throwable {
            for (int i = 0; i < COUNT; i++)
                doPost();
        }

        private void doPost() throws Throwable {
            HttpURLConnection connection = (HttpURLConnection) urlStalledPost.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            for (int i = 0; i < 10; i++)
                out.write("Hello world!!!");
            out.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            while (in.readLine() != null)
                ;
            in.close();
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
            connector.setAttribute("maxThreads", 1);

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

    private static class TestServlet extends HttpServlet {
        private final AtomicLong postTagCount = new AtomicLong();

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            System.out.println("Post received.");
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));

            while (in.readLine() != null)
                ;

            response.setContentLength(140);
            Writer writer = new OutputStreamWriter(response.getOutputStream());
            for (int i = 0; i < 10; i++)
                writer.write("Hello world!!!");
            writer.flush();

            slow();

            try {
                long n = postTagCount.incrementAndGet();
                if (n == 200)
                    Thread.sleep(100000000000000l);
                else {
                    Thread.sleep(1000l);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void slow() {
            for (int i = 0; i < 10; i++)
                normal();
            fast();
        }

        private void normal() {
            for (int i = 0; i < 50; i++)
                normal2();
        }

        private void normal2() {
            for (int i = 0; i < 20; i++)
                fast();
        }

        private void fast() {
            for (int i = 0; i < 15; i++)
                delay1();
            for (int i = 0; i < 15; i++)
                delay2();
        }

        private void delay1() {
            Profiler.rdtsc();
        }

        private void delay2() {
            Profiler.rdtsc();
            Profiler.rdtsc();
        }
    }
}