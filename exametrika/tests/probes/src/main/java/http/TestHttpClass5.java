package http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import com.exametrika.common.utils.Profiler;

/**
 * {@link TestHttpClass5} is a complex test modelling distributed communication between several {@link HttpURLConnection}
 * and {@link HttpServlet}.
 *
 * @author medvedev
 */
public class TestHttpClass5 {
    private static final int COUNT = Profiler.getOverhead() < 200 ? 100 : 1;
    private static long tagCount;
    private static long callCount;
    private static Set<String> tagIds = new HashSet();
    private static Set<String> prevTagIds;
    private static Set<Integer> indexes = new HashSet<Integer>();
    private static Map<String, Map<String, Integer>> requests = new TreeMap<String, Map<String, Integer>>();
    private static Map<String, Map<String, Integer>> threads = new TreeMap<String, Map<String, Integer>>();

    public static void main(String[] args) throws Throwable {
        Thread client1 = new Thread(new HttpClient1(), "client1");
        Thread client2 = new Thread(new HttpClient1(), "client2");
        Thread client3 = new Thread(new HttpClient1(), "client3");
        Thread server = new Thread(new HttpServer(args[0]), "server1");
        client1.start();
        client2.start();
        client3.start();
        server.start();
        client1.join();
        client2.join();
        client3.join();
        server.join();
    }

    private static class HttpClient1 implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(5000);

                for (int i = 0; i < 10000; i++) {
                    long t = System.currentTimeMillis();

                    test();

                    t = System.currentTimeMillis() - t;

                    StringBuilder builder = new StringBuilder("---------------- " + Thread.currentThread().getName() + ":" + i + ": " + t + "\n");
                    synchronized (tagIds) {
                        Set set = new HashSet(tagIds);
                        if (prevTagIds != null)
                            set.removeAll(prevTagIds);

                        builder.append("calls: " + callCount + ", tag calls: " + tagCount + ", tags:" + tagIds.size() +
                                ", new tags:" + set.size() + ", untagged indexes:" + indexes.size());
                        callCount = 0;
                        tagCount = 0;
                        prevTagIds = new HashSet(tagIds);
                        tagIds.clear();
                        indexes.clear();
                    }

                    System.out.println(builder.toString());
//                    if ((i % 10) == 0)
//                    synchronized (requests)
//                    {
//                        System.out.println("======================================== Begin. Requests:");
//                        for (Map.Entry<String, Map<String, Integer>> entry : requests.entrySet())
//                            System.out.println(entry.getKey() + "\n" + Strings.toString(entry.getValue().entrySet(), true));
//                        
//                        System.out.println("======================================== Threads:");
//                        for (Map.Entry<String, Map<String, Integer>> entry : threads.entrySet())
//                            System.out.println(entry.getKey() + "\n" + Strings.toString(entry.getValue().entrySet(), true));
//                        
//                        System.out.println("======================================== End");
//                    }
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        public void test() throws Throwable {
            post();
        }

        private void post() throws Throwable {
            for (int i = 0; i < COUNT; i++) {
                doPost(i);
                TestServlet2.slow(1);
            }
        }

        private void doPost(int index) throws Throwable {
            URL url = new URL("http://localhost:8180/test1/" + (index % 100) + "?query=" + index);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("xx-index", Integer.toString(index));

            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            for (int i = 0; i < 10; i++)
                out.write("Hello world!!!");
            out.close();

            InputStream stream = connection.getInputStream();
            connection.getContentLength();

            BufferedReader in = new BufferedReader(new InputStreamReader(stream));
            while (in.readLine() != null)
                ;
            in.close();
        }
    }

    private static class HttpClient2 {
        private final URL url;

        public HttpClient2() throws Throwable {
            url = new URL("http://localhost:8180/test2/");
        }

        public void post(int index) throws Throwable {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("xx-index", Integer.toString(index));

            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            for (int i = 0; i < 10; i++)
                out.write("Hello world!!!");
            out.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            while (in.readLine() != null)
                ;
            in.close();

            TestServlet2.slow2(1);
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

        private void createServer(String workPath) throws Throwable {
            Tomcat server = new Tomcat();

            Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
            connector.setPort(8180);

            connector.setAttribute("compression", "on");
            connector.setAttribute("maxThreads", 10);

            server.setConnector(connector);
            server.getService().addConnector(connector);

            File workFile = new File(workPath, "test");
            workFile.mkdirs();

            Context context1 = server.addContext("/test1", workPath);
            Tomcat.addServlet(context1, "testServlet", new TestServlet1());
            context1.addServletMapping("/", "testServlet");
            context1.setDisplayName("testApp1");

            Context context2 = server.addContext("/test2", workPath);
            context2.setDisplayName("testApp2");
            Tomcat.addServlet(context2, "testServlet", new TestServlet2());
            context2.addServletMapping("/", "testServlet");

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

    private static class TestServlet1 extends HttpServlet {
        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));

            while (in.readLine() != null)
                ;

            synchronized (requests) {
                String uri = request.getRequestURI() + '?' + request.getQueryString();

                Map<String, Integer> map = threads.get(Thread.currentThread().getName());
                if (map == null) {
                    map = new TreeMap<String, Integer>();
                    threads.put(Thread.currentThread().getName(), map);
                }

                Integer i = map.get(uri);
                if (i == null)
                    i = 0;

                map.put(uri, i + 1);

                map = requests.get(uri);
                if (map == null) {
                    map = new TreeMap<String, Integer>();
                    requests.put(uri, map);
                }

                i = map.get(Thread.currentThread().getName());
                if (i == null)
                    i = 0;

                map.put(Thread.currentThread().getName(), i + 1);
            }

            try {
                int i = Integer.valueOf(request.getHeader("xx-index"));
                HttpClient2 client = new HttpClient2();
                client.post(i);

                TestServlet2.slow(1);
                //slow(1);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

            response.setContentLength(140);
            Writer writer = new OutputStreamWriter(response.getOutputStream());
            for (int i = 0; i < 10; i++)
                writer.write("Hello world!!!");
            writer.flush();
        }
    }

    private static class TestServlet2 extends HttpServlet {
        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));

            while (in.readLine() != null)
                ;

            int index = Integer.valueOf(request.getHeader("xx-index"));

            synchronized (tagIds) {
                String tag = request.getHeader("EXA_TRACE_TAG");
                if (tag != null) {
                    tagCount++;
                    String[] parts = tag.split(";");
                    tagIds.add(parts[0]);
                } else
                    indexes.add(index);

                callCount++;
            }

            try {
                long n;
//                if (index >= 0 && index < 10)
//                    n = 100;
//                else if ((index % 100) < 10)
//                    n = 20;
                if (index > 0 && index <= 2)
                    n = 200;
                else if ((index % 10) == 0)
                    n = 50;
                else
                    n = 1;

                slow(n);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            response.setContentLength(140);
            Writer writer = new OutputStreamWriter(response.getOutputStream());
            for (int i = 0; i < 10; i++)
                writer.write("Hello world!!!");
            writer.flush();
        }

        private static void slow(long count) {
            slow2(count);
            for (int i = 0; i < count; i++)
                normal(true);
            fast();
        }

        private static void slow2(long count) {
            for (int i = 0; i < count; i++)
                normal(true);
            fast();
        }

        private static void normal(boolean recursive) {
            for (int i = 0; i < 50; i++)
                normal2();

            if (recursive)
                normal(false);
        }

        private static void normal2() {
            for (int i = 0; i < 20; i++)
                fast();
        }

        private static void fast() {
            for (int i = 0; i < 15; i++)
                delay1();
            for (int i = 0; i < 15; i++)
                delay2();
        }

        private static void delay1() {
            Profiler.rdtsc();
        }

        private static void delay2() {
            Profiler.rdtsc();
            Profiler.rdtsc();
        }
    }
}