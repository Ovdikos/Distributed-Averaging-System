public class Main {
    public static void printUsage() {
        System.out.println("Distributed Averaging System (DAS)");
        System.out.println("----------------------------------------");
        System.out.println("Usage examples:");
        System.out.println("1. Start master:   java -cp out Main PORT NUMBER");
        System.out.println("2. Add slave:      java -cp out Main PORT NUMBER");
        System.out.println("3. Calculate avg:  java -cp out Main PORT 0");
        System.out.println("4. Terminate:      java -cp out Main PORT -1");
        System.out.println("----------------------------------------");
    }

    public static void main(String[] args) {
        printUsage();

        if (args.length != 2) {
            System.err.println("Error: Required exactly 2 arguments!");
            System.err.println("Usage: java Main <port> <number>");
            System.exit(1);
        }

        try {
            int port = Integer.parseInt(args[0]);
            int number = Integer.parseInt(args[1]);

            if (port < 1024 || port > 65535) {
                System.err.println("Error: Port must be between 1024 and 65535");
                System.exit(1);
            }

            System.out.println("Starting DAS with port=" + port + " and number=" + number);
            DAS das = new DAS(port, number);
            das.start();

        } catch (NumberFormatException e) {
            System.err.println("Error: Port and number must be integers!");
            System.exit(1);
        }
    }
}