package udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;


public class TestUdpClass2 {
    private static final int READ_COUNT = 1000;
    private static final int WRITE_COUNT = 1200;

    public static void main(String[] args) throws Throwable {
        Thread client = new Thread(new UdpSocketClient(), "client");
        Thread server = new Thread(new UdpSocketServer(), "server");
        client.start();
        server.start();
        client.join();
        server.join();
    }

    private static abstract class Base implements Runnable {
        protected DatagramSocket socket;
        protected InetSocketAddress address;
        protected final byte[] buffer1 = new byte[10000];

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
            DatagramPacket packet = new DatagramPacket(buffer1, buffer1.length, address);
            for (int i = 0; i < WRITE_COUNT; i++)
                socket.send(packet);
        }

        protected void readPacket() throws Throwable {
            DatagramPacket packet = new DatagramPacket(buffer1, buffer1.length);
            for (int i = 0; i < READ_COUNT; i++)
                socket.receive(packet);
        }
    }

    public static class UdpSocketClient extends Base {
        @Override
        protected void connect() throws Throwable {
            address = new InetSocketAddress("localhost", 9990);
            socket = new DatagramSocket(9991);
        }

        @Override
        protected void test() throws Throwable {
            writePacket();
        }
    }

    public static class UdpSocketServer extends Base {
        @Override
        protected void connect() throws IOException {
            socket = new DatagramSocket(9991);
        }

        @Override
        protected void test() throws Throwable {
            readPacket();
        }
    }
}