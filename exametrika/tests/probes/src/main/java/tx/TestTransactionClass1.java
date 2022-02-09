package tx;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.messaging.ConnectionConfiguration;
import com.sun.messaging.jmq.jmsclient.runtime.BrokerInstance;
import com.sun.messaging.jmq.jmsclient.runtime.ClientRuntime;
import com.sun.messaging.jmq.jmsservice.BrokerEvent;
import com.sun.messaging.jmq.jmsservice.BrokerEventListener;

/**
 * {@link TestTransactionClass1} is a complex test modelling distributed communication between several {@link HttpURLConnection}
 * and {@link HttpServlet} and JMS brokers.
 *
 * @author medvedev
 */
public class TestTransactionClass1 {
    private static final Logger logger = LoggerFactory.getLogger(TestTransactionClass1.class);
    private static final AtomicLong counter = new AtomicLong();

    public static void main(String[] args) throws Throwable {
        Thread client1 = new Thread(new HttpClient1(), "client1");
        Thread client2 = new Thread(new HttpClient1(), "client2");
        Thread client3 = new Thread(new HttpClient1(), "client3");
        Thread server = new Thread(new HttpServer(args[0]), "server1");
        Thread broker = new Thread(new JmsBroker(args[1], args[2]), "broker");
        Thread consumer = new Thread(new JmsConsumer2(), "consumer");
        client1.start();
        client2.start();
        client3.start();
        server.start();
        broker.start();
        consumer.start();
        client1.join();
        client2.join();
        client3.join();
        server.join();
        broker.join();
        consumer.join();
    }

    private static class HttpClient1 implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(5000);

                for (int i = 0; true; i++) {
                    test();
                    System.out.println("---------------- " + Thread.currentThread().getName() + ":" + i);
                    //Thread.sleep(1000);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        public void test() throws Throwable {
            post();
        }

        private void post() throws Throwable {
            for (int i = 0; i < 1; i++)
                sendPost(i);
        }

        private void sendPost(int index) throws Throwable {
            URL url = new URL("http://localhost:8180/test1/" + (index % 100) + "?query=" + index);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("xx-index", Integer.toString(index));

            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            for (int i = 0; i < 10; i++)
                out.write("Hello world!!!");
            out.close();

            try {
                InputStream stream = connection.getInputStream();
                connection.getContentLength();

                BufferedReader in = new BufferedReader(new InputStreamReader(stream));
                while (in.readLine() != null)
                    ;
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
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
        private JmsProducer producer = new JmsProducer();

        @Override
        public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));

            while (in.readLine() != null)
                ;

            boolean error = (TestTransactionClass1.counter.getAndIncrement() % 1000) == 0;

            try {
                int i = Integer.valueOf(request.getHeader("xx-index"));
                producer.send(i, error);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

            if (error) {
                logger.error("Test log error record.");
                throw new RuntimeException("Test exception.");
            }

            response.setContentLength(140);
            Writer writer = new OutputStreamWriter(response.getOutputStream());
            for (int i = 0; i < 10; i++)
                writer.write("Hello world!!!");
            writer.flush();
        }
    }

    private static class JmsBroker implements Runnable {
        private BrokerInstance broker;
        private final String homePath;
        private final String varPath;

        public JmsBroker(String homePath, String varPath) {
            this.homePath = homePath;
            this.varPath = varPath;
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
            createBroker();
            System.in.read();
            destroyBroker();
        }

        private void createBroker() throws Throwable {
            ClientRuntime clientRuntime = ClientRuntime.getRuntime();

            broker = clientRuntime.createBrokerInstance();

            Properties props = broker.parseArgs(new String[]{"-imqhome", homePath, "-varhome", varPath});

            BrokerEventListener listener = new BrokerEventListener() {
                @Override
                public boolean exitRequested(BrokerEvent arg0, Throwable arg1) {
                    return true;
                }

                @Override
                public void brokerEvent(BrokerEvent arg0) {
                }
            };
            broker.init(props, listener);

            broker.start();

            com.sun.messaging.ConnectionFactory cf = new com.sun.messaging.ConnectionFactory();
            cf.setProperty(ConnectionConfiguration.imqAddressList, "mq://localhost:7676");
            Connection connection = cf.createConnection();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue("testQueue");
            MessageConsumer consumer1 = session.createConsumer(queue);
            consumer1.setMessageListener(new JmsConsumer());
            connection.start();
        }

        private void destroyBroker() {
            broker.stop();
            broker.shutdown();

            broker = null;
        }
    }

    private static class JmsProducer {
        private ConnectionFactory connectionFactory;
        private Connection connection;
        private QueueConnection queueConnection;
        private TopicConnection topicConnection;
        private JMSContext context;
        private Session session;
        private QueueSession queueSession;
        private TopicSession topicSession;
        private Queue queue1;
        private Topic topic1;
        private JMSProducer producer1;
        private Queue queue2;
        private MessageProducer producer2;
        private Queue queue3;
        private QueueSender sender3;
        private Topic topic4;
        private TopicPublisher publisher4;

        public synchronized void send(int i, boolean error) throws Throwable {
            send1(i, error);
            send2(i, error);
            send3(i, error);
            send4(i, error);
        }

        private void send1(int i, boolean error) throws Throwable {
            if (connection == null)
                createConnection();

            producer1.clearProperties();
            producer1.setProperty("xx_index", Integer.toString(i));
            if (error)
                producer1.setProperty("xx_error", true);

            producer1.send(queue1, new byte[100]);
            producer1.send(topic1, context.createTextMessage("Hello world!!!"));
            producer1.send(topic1, new ArrayList(Arrays.asList("value1", "value2")));
            producer1.send(topic1, "Hello world!!!");
        }

        private void send2(int i, boolean error) throws Throwable {
            if (connection == null)
                createConnection();

            Message message1 = session.createMessage();
            message1.setStringProperty("xx_index", Integer.toString(i));
            if (error)
                message1.setBooleanProperty("xx_error", true);
            producer2.send(message1);

            BytesMessage message2 = session.createBytesMessage();
            message2.setStringProperty("xx_index", Integer.toString(i));
            if (error)
                message2.setBooleanProperty("xx_error", true);
            message2.writeBoolean(true);
            message2.writeByte((byte) 127);
            message2.writeBytes(new byte[100]);
            message2.writeBytes(new byte[100], 0, 10);
            message2.writeChar('c');
            message2.writeDouble(1.01);
            message2.writeFloat(1.01f);
            message2.writeInt(123);
            message2.writeLong(Long.MAX_VALUE);
            message2.writeObject("Hello world!!!");
            message2.writeShort((short) 12345);
            message2.writeUTF("Hello world!!!");
            producer2.send(message2);

            MapMessage message3 = session.createMapMessage();
            message3.setStringProperty("xx_index", Integer.toString(i));
            if (error)
                message3.setBooleanProperty("xx_error", true);
            message3.setString("key1", "value1");
            message3.setInt("key2", 123);
            producer2.send(message3);

            ObjectMessage message4 = session.createObjectMessage();
            message4.setStringProperty("xx_index", Integer.toString(i));
            if (error)
                message4.setBooleanProperty("xx_error", true);
            message4.setObject(new ArrayList(Arrays.asList("element1", "element2")));
            producer2.send(message4);

            StreamMessage message5 = session.createStreamMessage();
            message5.setStringProperty("xx_index", Integer.toString(i));
            if (error)
                message5.setBooleanProperty("xx_error", true);
            message5.writeBoolean(true);
            message5.writeByte((byte) 127);
            message5.writeBytes(new byte[100]);
            message5.writeBytes(new byte[100], 0, 10);
            message5.writeChar('c');
            message5.writeDouble(1.01);
            message5.writeFloat(1.01f);
            message5.writeInt(123);
            message5.writeLong(Long.MAX_VALUE);
            message5.writeObject("Hello world!!!");
            message5.writeShort((short) 12345);
            message5.writeString("Hello world!!!");

            producer2.send(message5);

            TextMessage message6 = session.createTextMessage();
            message6.setText("Hello world!!!");
            message6.setStringProperty("xx_index", Integer.toString(i));
            if (error)
                message6.setBooleanProperty("xx_error", true);
            producer2.send(message6);
        }

        private void send3(int i, boolean error) throws Throwable {
            if (queueConnection == null)
                createQueueConnection();

            TextMessage message = queueSession.createTextMessage("Hellow world!!!");
            message.setStringProperty("xx_index", Integer.toString(i));
            if (error)
                message.setBooleanProperty("xx_error", true);
            sender3.send(message);
        }

        private void send4(int i, boolean error) throws Throwable {
            if (topicConnection == null)
                createTopicConnection();

            TextMessage message = topicSession.createTextMessage("Hellow world!!!");
            message.setStringProperty("xx_index", Integer.toString(i));
            if (error)
                message.setBooleanProperty("xx_error", true);
            publisher4.publish(message);
        }

        private synchronized void createConnection() {
            if (connection != null)
                return;

            try {
                com.sun.messaging.ConnectionFactory cf = new com.sun.messaging.ConnectionFactory();
                cf.setProperty(ConnectionConfiguration.imqAddressList, "mq://localhost:7676");
                connectionFactory = cf;
                connection = cf.createConnection();
                connection.start();
                context = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE);
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                queue1 = context.createQueue("testQueue");
                topic1 = context.createTopic("testTopic");
                producer1 = context.createProducer();
                queue2 = session.createQueue("testQueue");
                producer2 = session.createProducer(queue2);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private synchronized void createQueueConnection() {
            if (queueConnection != null)
                return;

            try {
                com.sun.messaging.QueueConnectionFactory cf = new com.sun.messaging.QueueConnectionFactory();
                cf.setProperty(ConnectionConfiguration.imqAddressList, "mq://localhost:7676");
                queueConnection = cf.createQueueConnection();
                queueConnection.start();
                queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                queue3 = queueSession.createQueue("testQueue");
                sender3 = queueSession.createSender(queue3);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private synchronized void createTopicConnection() {
            if (topicConnection != null)
                return;

            try {
                com.sun.messaging.TopicConnectionFactory cf = new com.sun.messaging.TopicConnectionFactory();
                cf.setProperty(ConnectionConfiguration.imqAddressList, "mq://localhost:7676");
                topicConnection = cf.createTopicConnection();
                topicConnection.start();
                topicSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
                topic4 = topicSession.createTopic("testTopic");
                publisher4 = topicSession.createPublisher(topic4);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class JmsConsumer implements MessageListener {
        private ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100));

        @Override
        public void onMessage(Message message) {
            try {
                handle(message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        private void handle(final Message message) {
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    onPrivateMessage(message);
                }
            });
        }

        private void onPrivateMessage(Message message) {
            try {
                if (message.getJMSRedelivered())
                    return;

                normal(message.propertyExists("xx_error"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void normal(boolean error) {
            //for (int i = 0; i < 50; i++)
            normal2();

            if (error)
                exception();
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
            System.nanoTime();
        }

        private void delay2() {
            System.nanoTime();
            System.nanoTime();
        }

        private void exception() {
            try {
                exception2();
            } catch (Throwable e) {
            }
        }

        private void exception2() {
            logger.error("Test log error record.");
            throw new RuntimeException("Test exception.");
        }
    }

    private static class JmsConsumer2 implements Runnable {
        private ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100));
        private ThreadPoolExecutor executor2 = new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100));

        @Override
        public void run() {
            try {
                Thread.sleep(5000);

                com.sun.messaging.ConnectionFactory cf = new com.sun.messaging.ConnectionFactory();
                cf.setProperty(ConnectionConfiguration.imqAddressList, "mq://localhost:7676");
                Connection connection = cf.createConnection();

                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Topic topic = session.createTopic("testTopic");
                MessageConsumer consumer = session.createConsumer(topic);
                connection.start();

                while (true) {
                    Message message = consumer.receive();
                    handle(message);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void handle(final Message message) {
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    onMessage(message);
                }
            });
        }

        private void onMessage(Message message) {
            try {
                if (message.getJMSRedelivered())
                    return;

                normal(message.propertyExists("xx_error"));
                executor2.execute(new Runnable() {
                    @Override
                    public void run() {
                        test2();
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void normal(boolean error) {
            //for (int i = 0; i < 50; i++)
            normal2();

            if (error)
                exception();
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
            System.nanoTime();
        }

        private void delay2() {
            System.nanoTime();
            System.nanoTime();
        }

        private void exception() {
            try {
                exception2();
            } catch (Throwable e) {
            }
        }

        private void exception2() {
            logger.error("Test log error record.");
            throw new RuntimeException("Test exception.");
        }

        private void test2() {
            try {
                Thread.sleep(0);
            } catch (Exception e) {
            }
        }
    }
}