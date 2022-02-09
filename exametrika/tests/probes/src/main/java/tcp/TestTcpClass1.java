package tcp;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


public class TestTcpClass1 {
    private static final int COUNT = 1000;

    public static void main(String[] args) throws Throwable {
        Thread client = new Thread(new TcpChannelClient(), "client");
        Thread server = new Thread(new TcpChannelServer(), "server");
        client.start();
        server.start();
        client.join();
        server.join();
    }

    private static abstract class Base implements Runnable {
        protected Socket socket;
        protected final byte[] buffer1 = new byte[10000];
        protected final ByteBuffer buffer2 = ByteBuffer.allocate(10000);

        @Override
        public void run() {
            try {
                for (int i = 0; i < 10000; i++) {
                    long t = System.currentTimeMillis();

                    connect();
                    test();

                    t = System.currentTimeMillis() - t;

                    Thread.sleep(500);
                    socket.close();
                    Thread.sleep(500);

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

        protected void writeChannel() throws Throwable {
            SocketChannel channel = socket.getChannel();
            for (int i = 0; i < COUNT; i++) {
                while (buffer2.hasRemaining())
                    channel.write(buffer2);
                buffer2.rewind();
                while (buffer2.hasRemaining())
                    channel.write(new ByteBuffer[]{buffer2});
                buffer2.rewind();
                while (buffer2.hasRemaining())
                    channel.write(new ByteBuffer[]{buffer2}, 0, 1);
                buffer2.rewind();
            }
        }

        protected void readChannel() throws Throwable {
            SocketChannel channel = socket.getChannel();
            for (int i = 0; i < COUNT; i++) {
                while (buffer2.hasRemaining())
                    channel.read(buffer2);
                buffer2.rewind();
                while (buffer2.hasRemaining())
                    channel.read(new ByteBuffer[]{buffer2});
                buffer2.rewind();
                while (buffer2.hasRemaining())
                    channel.read(new ByteBuffer[]{buffer2}, 0, 1);
                buffer2.rewind();
            }
        }
    }

    public static class TcpChannelClient extends Base {
        @Override
        protected void connect() throws Throwable {
            SocketChannel channel = SocketChannel.open();
            channel.socket().setReuseAddress(true);
            channel.bind(new InetSocketAddress(9990));
            channel.connect(new InetSocketAddress("localhost", 9991));
            socket = channel.socket();
        }

        @Override
        protected void test() throws Throwable {
            writeStream();
            readStream();
            writeChannel();
            readChannel();
        }
    }

    public static class TcpChannelServer extends Base {
        private final ServerSocketChannel serverSocketChannel;

        public TcpChannelServer() {
            try {
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.bind(new InetSocketAddress(9991));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void connect() throws Throwable {
            socket = serverSocketChannel.accept().socket();
        }

        @Override
        protected void test() throws Throwable {
            readStream();
            writeStream();
            readChannel();
            writeChannel();
        }
    }
}