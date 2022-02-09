package tcp;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class TestTcpClass2 {
    private static final int COUNT = 1000;

    public static void main(String[] args) throws Throwable {
        Thread client = new Thread(new TcpSocketClient(), "client");
        Thread server = new Thread(new TcpSocketServer(), "server");
        client.start();
        server.start();
        client.join();
        server.join();
    }

    private static abstract class Base implements Runnable {
        protected Socket socket;
        protected final byte[] buffer1 = new byte[10000];

        @Override
        public void run() {
            try {
                for (int i = 0; i < 10000; i++) {
                    long t = System.currentTimeMillis();

                    connect();
                    test();

                    t = System.currentTimeMillis() - t;

                    Thread.sleep(100);
                    socket.close();
                    Thread.sleep(100);

                    System.out.println(getClass().getSimpleName() + " ---------------- " + i + ": " + t);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        protected abstract void connect() throws Throwable;

        protected abstract void test() throws Throwable;

        protected void writeStream() throws Throwable {
            OutputStream out = socket.getOutputStream();
            for (int i = 0; i < COUNT; i++) {
                out.write(10);
                out.write(buffer1);
                out.write(buffer1, 0, buffer1.length);
            }
            out.flush();
        }

        protected void readStream() throws Throwable {
            InputStream in = socket.getInputStream();
            for (int i = 0; i < COUNT; i++) {
                in.read();
                int count = 0;
                while (count < buffer1.length)
                    count += in.read(buffer1, count, buffer1.length - count);
                count = 0;
                while (count < buffer1.length)
                    count += in.read(buffer1, count, buffer1.length - count);
            }
        }
    }

    public static class TcpSocketClient extends Base {
        @Override
        protected void connect() throws Throwable {
            socket = new Socket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(9998));
            socket.connect(new InetSocketAddress("localhost", 9999));
        }

        @Override
        protected void test() throws Throwable {
            writeStream();
            readStream();
        }
    }

    public static class TcpSocketServer extends Base {
        private final ServerSocket serverSocket;

        public TcpSocketServer() {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(9999));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void connect() throws Throwable {
            socket = serverSocket.accept();
        }

        @Override
        protected void test() throws Throwable {
            readStream();
            writeStream();
        }
    }
}