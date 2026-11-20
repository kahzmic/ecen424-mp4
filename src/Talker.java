import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class Talker {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java Talker <talkerPort> <listenerPort>");
            return;
        }
        int port, listenerPort;
        try {
            port = Integer.parseInt(args[0]);
            listenerPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Ports must be numbers");
            return;
        }
        Scanner input = new Scanner(System.in);

        // Wait for a Listener to connect
        DatagramSocket talkerSocket = new DatagramSocket(port);
        byte[] buf = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        System.out.println("Waiting for Listener on port " + port + "...");
        talkerSocket.receive(packet);

        // extract the Listener's address and port from the received packet
        InetAddress listenerAddress = packet.getAddress();
        int listenerDataPort = packet.getPort();
        System.out.println("Accepted Listener " + listenerAddress.getHostAddress() + ":" + listenerDataPort);

        while (true) {
            // Loop until valid message
            String line;
            while (true) {
                System.out.print("Enter message (1-50 characters): ");
                if (!input.hasNextLine())
                    return;
                line = input.nextLine();
                if (line.length() >= 1 && line.length() <= 50)
                    break;
                System.out.println("Message must be 1-50 characters, got " + line.length());
            }

            // Request connection to listener
            DatagramSocket listenSocket = new DatagramSocket();
            listenSocket.setSoTimeout(2000);
            send(listenSocket, "REQ", listenerAddress, listenerPort);

            // Split messages and frame with unique sequence of numbers
            int count = (line.length() + 9) / 10;
            String[] messages = new String[count + 1];
            messages[0] = "0:" + count;
            for (int i = 1; i <= count; i++)
                messages[i] = i + ":" + line.substring((i - 1) * 10, Math.min(line.length(), i * 10));

            for (int seq = 0; seq <= count; seq++)
                sendAndWait(talkerSocket, listenSocket, messages[seq], seq, listenerAddress, listenerDataPort);

            System.out.println("All " + (count + 1) + " messages ACKed\n");
            listenSocket.close();
        }
    }

    private static void sendAndWait(DatagramSocket talkerSocket, DatagramSocket listenSocket, String message,
            int seq, InetAddress address, int port) throws Exception {
        byte[] buf = new byte[256];
        while (true) {
            send(talkerSocket, message, address, port);
            System.out.println("Sent message " + seq);
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                listenSocket.receive(packet);
                int ack = Integer.parseInt(new String(packet.getData(), 0, packet.getLength()));
                if (ack == seq + 1) {
                    System.out.println("Message " + seq + " ACKed (ACK " + ack + ")");
                    return;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout on message " + seq + ", resending");
            }
        }
    }

    private static void send(DatagramSocket socket, String message, InetAddress address, int port) throws Exception {
        byte[] buf = message.getBytes();
        socket.send(new DatagramPacket(buf, buf.length, address, port));
    }
}
