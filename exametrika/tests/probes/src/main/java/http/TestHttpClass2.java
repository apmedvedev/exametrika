package http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

/**
 * {@link TestHttpClass2} tests {@link HttpsURLConnection}.
 *
 * @author medvedev
 */
public class TestHttpClass2 {
    public static void main(String[] args) throws Throwable {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession sslSession) {
                return true;
            }
        });

        Thread client = new Thread(new HttpClient(), "client");
        Thread server = new Thread(new HttpServer(args[0]), "server");
        client.start();
        server.start();
        client.join();
        server.join();
    }

    private static class HttpClient implements Runnable {
        private final URL url;
        private final URL bulkUrl;
        private final URL errorUrl;
        private static final int COUNT = 10;

        public HttpClient() throws Throwable {
            url = new URL("https://localhost:8543/test/");
            bulkUrl = new URL("https://localhost:8543/test/bulk");
            errorUrl = new URL("https://localhost:8543/test/error");
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
            get();
            put();
            post();
            error();
        }

        private void get() throws Throwable {
            for (int i = 0; i < COUNT; i++) {
                doGet1();
                doGet2();
            }
        }

        private void put() throws Throwable {
            for (int i = 0; i < COUNT; i++)
                doPut();
        }

        private void post() throws Throwable {
            for (int i = 0; i < COUNT; i++) {
                doPost1();
                doPost2();
            }
        }

        private void error() throws Throwable {
            doError();
        }

        private void doGet1() throws Throwable {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(createSslSocketFactory());

            connection.setRequestMethod("GET");

            connection.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            connection.getContentLength();

            while (in.readLine() != null)
                ;
            in.close();
        }

        private void doGet2() throws Throwable {
            HttpsURLConnection connection = (HttpsURLConnection) bulkUrl.openConnection();
            connection.setSSLSocketFactory(createSslSocketFactory());

            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            connection.getContentLength();

            while (in.readLine() != null)
                ;
            in.close();
        }

        private void doPut() throws Throwable {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(createSslSocketFactory());

            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);

            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            for (int i = 0; i < 10; i++)
                out.write("Hello world!!!");
            out.close();

            connection.getResponseCode();
            connection.getContentLength();
        }

        private void doPost1() throws Throwable {
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(createSslSocketFactory());

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            for (int i = 0; i < 10; i++)
                out.write("Hello world!!!");
            out.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            connection.getContentLength();

            while (in.readLine() != null)
                ;
            in.close();
        }

        private void doPost2() throws Throwable {
            HttpsURLConnection connection = (HttpsURLConnection) bulkUrl.openConnection();
            connection.setSSLSocketFactory(createSslSocketFactory());

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(100);

            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            for (int i = 0; i < 1000; i++)
                out.write("Hello world!!!");
            out.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            connection.getContentLength();

            while (in.readLine() != null)
                ;
            in.close();
        }

        private void doError() throws Throwable {
            HttpsURLConnection connection = (HttpsURLConnection) errorUrl.openConnection();
            connection.setSSLSocketFactory(createSslSocketFactory());

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            for (int i = 0; i < 10; i++)
                out.write("Hello world!!!");
            out.close();

            connection.getResponseCode();
            connection.getContentLength();

            if (connection.getErrorStream() != null) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                while (true) {
                    String str = in.readLine();
                    if (str == null)
                        break;

                    System.out.println(str);
                }
            }
        }

        private SSLSocketFactory createSslSocketFactory() throws Exception {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(getClass().getResourceAsStream("truststore.jks"), "testtest".toCharArray());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, "testtest".toCharArray());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
            return sslContext.getSocketFactory();
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
            connector.setPort(8543);
            connector.setAttribute("SSLEnabled", true);
            connector.setAttribute("scheme", "https");
            connector.setAttribute("secure", true);
            connector.setAttribute("keystoreFile", workPath + "/../keystore.jks");
            connector.setAttribute("keyPass", "testtest");
            connector.setAttribute("clientAuth", "false");
            connector.setAttribute("sslProtocol", "TLS");

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

    private static class TestServlet extends HttpServlet {
        private final AtomicLong getTagCount = new AtomicLong();
        private final AtomicLong bulkGetTagCount = new AtomicLong();
        private final AtomicLong putTagCount = new AtomicLong();
        private final AtomicLong postTagCount = new AtomicLong();
        private final AtomicLong bulkPostTagCount = new AtomicLong();

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            boolean bulk = request.getRequestURI().equals("/test/bulk");
            int count = bulk ? 1000 : 10;
            if (request.getHeader("EXA_TRACE_TAG") != null) {
                long n;
                if (!bulk)
                    n = getTagCount.incrementAndGet();
                else
                    n = bulkGetTagCount.incrementAndGet();

                if ((n % 1000) == 0)
                    System.out.println("=============== GET tag received:" + n + ", bulk:" + bulk);
            }

            Writer writer = new OutputStreamWriter(response.getOutputStream());
            for (int i = 0; i < count; i++)
                writer.write("Hello world!!!");
            writer.close();
        }

        @Override
        public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
            if (request.getHeader("EXA_TRACE_TAG") != null) {
                long n = putTagCount.incrementAndGet();

                if ((n % 1000) == 0)
                    System.out.println("=============== PUT tag received:" + n);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));

            while (in.readLine() != null)
                ;
            in.close();
        }

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            if (request.getRequestURI().equals("/test/error")) {
                response.sendError(400, "Test error.");
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                return;
            }

            boolean bulk = request.getRequestURI().equals("/test/bulk");
            int count = bulk ? 1000 : 10;
            if (request.getHeader("EXA_TRACE_TAG") != null) {
                long n;
                if (!bulk)
                    n = postTagCount.incrementAndGet();
                else
                    n = bulkPostTagCount.incrementAndGet();

                if ((n % 1000) == 0)
                    System.out.println("=============== POST tag received:" + n + ", bulk:" + bulk);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));

            while (in.readLine() != null)
                ;
            in.close();

            Writer writer = new OutputStreamWriter(response.getOutputStream());
            for (int i = 0; i < count; i++)
                writer.write("Hello world!!!");
            writer.close();
        }
    }
}