import java.net.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DAS {
    private final int port;
    private final int number;
    private DatagramSocket socket;
    private volatile boolean isMaster;
    private final AtomicBoolean isRunning;
    private final List<Integer> receivedNumbers;
    private volatile InetAddress localAddress;
    private volatile String broadcastAddress;

    public DAS(int port, int number) {
        this.port = port;
        this.number = number;
        this.isRunning = new AtomicBoolean(true);
        this.receivedNumbers = new ArrayList<>();
        this.receivedNumbers.add(number);
        initializeNetworkAddresses();
    }

    private void initializeNetworkAddresses() {
        try {
            localAddress = InetAddress.getLocalHost();
            String ipAddress = localAddress.getHostAddress();
            String[] ipParts = ipAddress.split("\\.");
            broadcastAddress = ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + ".255";

            System.out.println("Local IP: " + ipAddress);
            System.out.println("Broadcast address: " + broadcastAddress);
        } catch (UnknownHostException e) {
            System.err.println("Error getting network addresses: " + e.getMessage());
            System.exit(1);
        }
    }

    public void start() {
        try {
            socket = new DatagramSocket(port);
            isMaster = true;
            System.out.println("Started in MASTER mode on port " + port);
            System.out.println("Initial value: " + number);
            runMaster();
        } catch (BindException e) {
            try {
                socket = new DatagramSocket();
                isMaster = false;
                System.out.println("Started in SLAVE mode");
                runSlave();
                System.exit(0);
            } catch (IOException ex) {
                System.err.println("Failed to start in SLAVE mode: " + ex.getMessage());
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("Failed to start: " + e.getMessage());
            System.exit(1);
        }
    }

    private synchronized void runMaster() {
        try {
            socket.setSoTimeout(0);
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (isRunning.get()) {
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength()).trim();
                try {
                    int receivedNumber = Integer.parseInt(received);
                    InetAddress senderAddress = packet.getAddress();
                    int senderPort = packet.getPort();

                    if (receivedNumber == -1 || receivedNumber == 0) {
                        if (senderAddress.equals(localAddress) && senderPort == socket.getLocalPort()) {
                            continue;
                        }
                    }

                    if (receivedNumber == -1) {
                        handleTerminationSignal();
                        break;
                    } else if (receivedNumber == 0) {
                        handleAverageRequest();
                    } else {
                        receivedNumbers.add(receivedNumber);
                        System.out.println("Received: " + receivedNumber);
                    }

                } catch (NumberFormatException e) {
                    System.err.println("Error: Invalid number format");
                }
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("Error in master mode: " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    private void handleTerminationSignal() {
        try {
            System.out.println("Received termination signal (-1)");
            broadcastNumber(-1);
            Thread.sleep(500);
            isRunning.set(false);
            cleanup();
            System.out.println("Master terminated");
            System.exit(0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void handleAverageRequest() {
        // Рахуємо кількість ненульових чисел
        long validNumbersCount = receivedNumbers.stream()
                .filter(num -> num != 0 && num != -1)
                .count();

        if (validNumbersCount == 0) {
            return;
        }

        int sum = 0;
        int count = 0;

        for (int num : receivedNumbers) {
            if (num != 0 && num != -1) {
                sum += num;
                count++;
            }
        }

        int average = (count > 0) ? sum / count : 0;
        System.out.println("Computing average of " + count + " numbers");
        System.out.println("Average: " + average);

        broadcastNumber(average);
    }

    private void broadcastNumber(int number) {
        try {
            String message = String.valueOf(number);
            byte[] buffer = message.getBytes();
            InetAddress broadcastAddr = InetAddress.getByName(broadcastAddress);

            socket.setBroadcast(true);
            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length, broadcastAddr, port
            );

            socket.send(packet);
            System.out.println("Broadcast sent: " + number);
            Thread.sleep(100);

        } catch (Exception e) {
            System.err.println("Error broadcasting message: " + e.getMessage());
        }
    }

    private void runSlave() throws IOException {
        try {
            String message = String.valueOf(number);
            byte[] buffer = message.getBytes();
            InetAddress masterAddress = InetAddress.getLocalHost();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, masterAddress, port);

            socket.send(packet);
            System.out.println("Sent: " + number);

        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

}
