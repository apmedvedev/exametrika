

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class Connectivity {
    public static void main(String[] args) throws Throwable {
        if (args.length == 0) {
            printUsage();
            return;
        }

        if (args[0].equals("client")) {
            if (args.length != 3) {
                printUsage();
                return;
            }

            new Client(args[1], Integer.parseInt(args[2]));
        } else if (args[0].equals("server")) {
            if (args.length != 2) {
                printUsage();
                return;
            }

            new Server(Integer.parseInt(args[1]));
        } else
            printUsage();
    }

    private static void printUsage() {
        System.out.println("Usage: client <host> <port> / server <port>");
    }

    public static class Client {
        public Client(String host, int port) throws Throwable {
            Socket socket = new Socket();
            socket.setReuseAddress(true);
            socket.connect(new InetSocketAddress(host, port));

            System.out.println("Connected.");
        }
    }

    public static class Server {
        private final ServerSocket serverSocket;

        public Server(int port) throws Throwable {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Connected to: " + socket.getRemoteSocketAddress().toString());
                socket.close();
            }
        }
    }
}