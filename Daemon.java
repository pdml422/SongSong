import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class Daemon {
    private final String daemonName;
    private final String directoryHost;
    private final List<String> filesToShare;
    private int socketPort;
    private static final int FRAGMENT_SIZE = 1024 * 1024; // 1MB

    public Daemon(String daemonName, String directoryHost, List<String> filesToShare) {
        this.daemonName = daemonName;
        this.directoryHost = directoryHost;
        this.filesToShare = filesToShare;
    }

    public void start() {
        try {
            Registry registry = LocateRegistry.getRegistry(directoryHost, 1099);
            Directory directory = (Directory) registry.lookup("FileDirectory");

            // Find an available port
            try (ServerSocket ss = new ServerSocket(0)) {
                socketPort = ss.getLocalPort();
            }

            // Register files with the directory
            for (String file : filesToShare) {
                directory.registerFile(file, "localhost", socketPort); // Assuming localhost for simplicity, adjust for network
            }
            System.out.println("Daemon '" + daemonName + "' registered files and listening on port " + socketPort);

            // Start socket server to serve file fragments
            startFileServer();

        } catch (Exception e) {
            System.err.println("Daemon exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private void startFileServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(socketPort)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    handleClientRequest(clientSocket);
                }
            } catch (IOException e) {
                System.err.println("Daemon file server exception: " + e.toString());
                e.printStackTrace();
            }
        }).start();
    }

    private void handleClientRequest(Socket clientSocket) {
        new Thread(() -> {
            try (clientSocket;
                 OutputStream out = clientSocket.getOutputStream()) {
                String requestedFile = new java.io.BufferedReader(new java.io.InputStreamReader(clientSocket.getInputStream())).readLine();
                if (requestedFile != null && filesToShare.contains(requestedFile)) {
                    File file = new File(requestedFile);
                    if (file.exists()) {
                        long fileSize = file.length();
                        long fragments = (fileSize + FRAGMENT_SIZE - 1) / FRAGMENT_SIZE; // Calculate fragments
                        for (long i = 0; i < fragments; i++) {
                            byte[] buffer = new byte[FRAGMENT_SIZE];
                            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                                fileInputStream.skip(i * FRAGMENT_SIZE); // Skip to the fragment start
                                int bytesRead = fileInputStream.read(buffer);
                                if (bytesRead > 0) {
                                    out.write(buffer, 0, bytesRead);
                                } else {
                                    break; // EOF
                                }
                            }
                        }
                        System.out.println("Served file fragment for '" + requestedFile + "' to " + clientSocket.getInetAddress());
                    } else {
                        System.err.println("File not found: " + requestedFile);
                    }
                } else {
                    System.err.println("Requested file not shared or invalid request from " + clientSocket.getInetAddress());
                }
            } catch (IOException e) {
                System.err.println("Error handling client request: " + e.toString());
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: Daemon <daemonName> <directoryHost> <file1> <file2> ...");
            return;
        }
        String daemonName = args[0];
        String directoryHost = args[1];
        List<String> filesToShare = java.util.Arrays.asList(java.util.Arrays.copyOfRange(args, 2, args.length));

        Daemon daemon = new Daemon(daemonName, directoryHost, filesToShare);
        daemon.start();
    }
}