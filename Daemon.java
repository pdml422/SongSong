import java.io.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashSet;
import java.util.Set;

public class Daemon {

    private final String directoryHost;
    private final int directoryPort = 4000;
    private final int daemonPort;
    private final String daemonAddress;
    private final Set<String> availableFiles;
    private ServerSocket serverSocket;

    public Daemon(String directoryHost, int daemonPort, String daemonAddress, Set<String> availableFiles) {
        this.directoryHost = directoryHost;
        this.daemonPort = daemonPort;
        this.daemonAddress = daemonAddress;
        this.availableFiles = availableFiles;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(daemonPort);
            System.out.println("Daemon listening on port " + daemonPort);

            // Register files with the Directory
            registerWithDirectory();

            // Start listening for download requests
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new FragmentHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Daemon exception: " + e.getMessage());
        }
    }

    private void registerWithDirectory() {
        try {
            Registry registry = LocateRegistry.getRegistry(directoryHost, directoryPort);
            DirectoryService directoryService = (DirectoryService) registry.lookup("directory_service");

            for (String file : availableFiles) {
                directoryService.registerFile(file, daemonAddress, daemonPort);
            }
            System.out.println("Registered files with directory.");

        } catch (Exception e) {
            System.err.println("Error registering with directory: " + e.getMessage());
        }
    }

    // Inner class to handle fragment requests
    private class FragmentHandler implements Runnable {
        private final Socket clientSocket;

        public FragmentHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                    ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
                    ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
            ) {
                Object requestType = ois.readObject(); // Read the request type

                if ("GET_FILE_SIZE".equals(requestType)) {
                    String fileName = (String) ois.readObject();
                    System.out.println("File size requested for: " + fileName);
                    File file = new File(fileName);
                    if (file.exists() && availableFiles.contains(fileName)) {
                        oos.writeObject(file.length()); // Send the file size
                        System.out.println("Sent file size for " + fileName + ": " + file.length());
                    } else {
                        oos.writeObject(null); // File not found
                        System.out.println("File not found: " + fileName);
                    }
                } else {
                    // Handle fragment request
                    String fileName = (String) requestType; // If it's not "GET_FILE_SIZE", assume it's the fileName
                    int fragmentNumber = (Integer) ois.readObject();
                    System.out.println("Request for fragment " + fragmentNumber + " of " + fileName);

                    // TODO: Load the file and send the fragment
                    byte[] fragment = getFileFragment(fileName, fragmentNumber);
                    if (fragment != null) {
                        oos.writeObject(fragment);
                        System.out.println("Sent fragment " + fragmentNumber + " of " + fileName);
                    } else {
                        oos.writeObject(null); // Indicate that the fragment was not found
                        System.out.println("Fragment " + fragmentNumber + " of " + fileName + " not found.");
                    }
                }

            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Fragment handler exception: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }
        }

        private byte[] getFileFragment(String fileName, int fragmentNumber) throws IOException {
            File file = new File(fileName);
            if (!file.exists() || !availableFiles.contains(fileName)) {
                return null;
            }

            long fileSize = file.length();
            int fragmentSize = 1024 * 1024; // 1MB
            long startByte = (long) fragmentNumber * fragmentSize;

            if (startByte >= fileSize) {
                return null; // Fragment number is out of range
            }

            int bytesToRead = (int) Math.min(fragmentSize, fileSize - startByte);
            byte[] buffer = new byte[bytesToRead];

            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                raf.seek(startByte);
                raf.read(buffer);
            }

            return buffer;
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: Daemon <directoryHost> <daemonPort> <daemonAddress> <file1,file2,...>");
            System.exit(1);
        }

        String directoryHost = args[0];
        int daemonPort = Integer.parseInt(args[1]);
        String daemonAddress = args[2];
        String[] files = args[3].split(",");

        Set<String> availableFiles = new HashSet<>();
        for (String file : files) {
            availableFiles.add(file.trim());
        }

        Daemon daemon = new Daemon(directoryHost, daemonPort, daemonAddress, availableFiles);
        daemon.start();
    }
}