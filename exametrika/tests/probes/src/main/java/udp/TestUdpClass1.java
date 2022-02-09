package udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;


public class TestUdpClass1 {
    private static final int READ_COUNT = 1000;
    private static final int WRITE_COUNT = 1200;

    public static void main(String[] args) throws Throwable {
        Thread client = new Thread(new UdpChannelClient(), "client");
        Thread server = new Thread(new UdpChannelServer(), "server");
        client.start();
        server.start();
        client.join();
        server.join();
    }

    private static abstract class Base implements Runnable {
        protected DatagramSocket socket;
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

                    Thread.sleep(1000);
                    socket.close();


                    System.out.println(getClass().getSimpleName() + " ---------------- " + i + ": " + t);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        protected abstract void connect() throws Throwable;

        protected abstract void test() throws Throwable;

        protected void writePacket() throws Throwable {
            DatagramPacket packet = new DatagramPacket(buffer1, buffer1.length);
            for (int i = 0; i < WRITE_COUNT; i++)
                socket.send(packet);
        }

        protected void readPacket() throws Throwable {
            DatagramPacket packet = new DatagramPacket(buffer1, buffer1.length);
            for (int i = 0; i < READ_COUNT; i++)
                socket.receive(packet);
        }

        protected void writeChannel() throws Throwable {
            DatagramChannel channel = socket.getChannel();
            for (int i = 0; i < WRITE_COUNT; i++) {
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
            DatagramChannel channel = socket.getChannel();
            for (int i = 0; i < READ_COUNT; i++) {
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

    public static class UdpChannelClient extends Base {
        @Override
        protected void connect() throws Throwable {
            DatagramChannel channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(9998));
            channel.connect(new InetSocketAddress("localhost", 9999));
            socket = channel.socket();
        }

        @Override
        protected void test() throws Throwable {
            writePacket();
            writeChannel();
        }
    }

    public static class UdpChannelServer extends Base {
        @Override
        protected void connect() throws IOException {
            DatagramChannel channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(9999));
            channel.connect(new InetSocketAddress("localhost", 9998));
            socket = channel.socket();
        }

        @Override
        protected void test() throws Throwable {
            readPacket();
            readChannel();
        }
    }
}