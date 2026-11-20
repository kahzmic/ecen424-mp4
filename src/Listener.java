import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class Listener {
    private static Random random = new Random();

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) {
            System.out.println("Usage: java Listener <listenerPort> <talkerPort> <talkerIP> [drop true|false]");
            return;
        }
        int port = Integer.parseInt(args[0]);
        int talkerPort = Integer.parseInt(args[1]);
        InetAddress talkerAddress = InetAddress.getByName(args[2]);
        boolean drop = args.length == 4 && Boolean.parseBoolean(args[3]);

        // Request connection to Talker
        DatagramSocket talkerSocket = new DatagramSocket();
        send(talkerSocket, "REQ", talkerAddress, talkerPort);
        System.out.println("Requested connection to Talker " + talkerAddress.getHostAddress() + ":" + talkerPort);

        while (true) {
            // Wait for a Talker to connect
            DatagramSocket listenSocket = new DatagramSocket(port);
            byte[] buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            System.out.println("Waiting for Talker on port " + port + "...");
            listenSocket.receive(packet);
            InetAddress talkerAckAddress = packet.getAddress();
            int talkerAckPort = packet.getPort();
            System.out.println("Talker connected, waiting for messages");

            // Receive messages and send ACKs
            int count = Integer.parseInt(receiveMessage(talkerSocket, listenSocket, 1, talkerAckAddress, talkerAckPort, drop));
            StringBuilder message = new StringBuilder();
            for (int i = 1; i <= count; i++)
                message.append(receiveMessage(talkerSocket, listenSocket, i + 1, talkerAckAddress, talkerAckPort, drop && i < count));

            listenSocket.close();
            System.out.println("Complete message: " + message + "\n");
        }
    }

    private static String receiveMessage(DatagramSocket talkerSocket, DatagramSocket listenSocket, int ack,
            InetAddress address, int port, boolean drop) throws Exception {
        byte[] buf = new byte[256];
        while (true) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            talkerSocket.receive(packet);
            String received = new String(packet.getData(), 0, packet.getLength());
            int colon = received.indexOf(':');
            int seq = Integer.parseInt(received.substring(0, colon));
            boolean isNew = seq == ack - 1;
            System.out.println((isNew ? "Received message " : "Received duplicate message ") + seq);

            // Send ACK for the received message
            int reply = isNew ? ack : ack - 1;
            if (drop && random.nextBoolean()) {
                System.out.println("Dropped ACK for message " + seq);
            } else {
                send(listenSocket, String.valueOf(reply), address, port);
                System.out.println("Sent ACK " + reply);
            }
            if (isNew)
                return received.substring(colon + 1);
        }
    }

    private static void send(DatagramSocket socket, String message, InetAddress address, int port) throws Exception {
        byte[] buf = message.getBytes();
        socket.send(new DatagramPacket(buf, buf.length, address, port));
    }
}
