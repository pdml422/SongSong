import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;

public class DaemonImpl extends UnicastRemoteObject implements Daemon {  // Implement the interface

    private final String directoryHost;
    private final int directoryPort = 4000;
    private final int daemonPort;
    private final String daemonAddress;
    private final Set<String> availableFiles;
    private ServerSocket serverSocket;

    public DaemonImpl(String directoryHost, int daemonPort, String daemonAddress, Set<String> availableFiles) throws RemoteException {
        this.directoryHost = directoryHost;
        this.daemonPort = daemonPort;
        this.daemonAddress = daemonAddress;
        this.availableFiles = availableFiles;
    }

    @Override
    public void registerFiles() throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(directoryHost, directoryPort);
            DirectoryService directoryService = (DirectoryService) registry.lookup("directory_service");

            for (String file : availableFiles) {
                directoryService.registerFile(file, this); // Pass "this" (the DaemonInterface)
                System.out.println("Registered " + file + " with directory");
            }
            System.out.println("Registered files with directory.");

        } catch (Exception e) {
            System.err.println("Error registering with directory: " + e.getMessage());
        }
    }

    @Override
    public void unregisterFiles(Set<String> files) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(directoryHost, directoryPort);
            DirectoryService directoryService = (DirectoryService) registry.lookup("directory_service");

            for (String file : files) {
                try {
                    directoryService.unregisterFile(file, this);
                    System.out.println("Unregistered " + file + " with directory");
                } catch (Exception unregEx) {
                    System.err.println("Error unregistering " + file + " : " + unregEx.getMessage());
                }
            }
            System.out.println("Unregistered files with directory.");

        } catch (Exception e) {
            System.err.println("Error unregistering with directory: " + e.getMessage());
        }
    }

    @Override
    public byte[] getFileFragment(String fileName, int fragmentNumber) throws RemoteException {
        File file = new File(fileName);
        if (!file.exists() || !availableFiles.contains(fileName)) {
            return null;
        }

        long fileSize = file.length();
        int fragmentSize = 1024 * 1024; // Example fragment size, adjust as needed
        long startByte = (long) fragmentNumber * fragmentSize;

        if (startByte >= fileSize) {
            return null; // Fragment number is out of range
        }

        int bytesToRead = (int) Math.min(fragmentSize, fileSize - startByte);
        byte[] buffer = new byte[bytesToRead];

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(startByte);
            raf.read(buffer);
        } catch (IOException e) {
            throw new RemoteException("Error getting file fragment", e);
        }

        return buffer;
    }

    @Override
    public String getDaemonAddress() throws RemoteException {
        return daemonAddress;
    }

    @Override
    public int getDaemonPort() throws RemoteException {
        return daemonPort;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(daemonPort);
            System.out.println("Daemon listening on port " + daemonPort);

            // Register files with the Directory
            registerFiles(); // Use the registerFiles method

            // Add a shutdown hook to unregister files on exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    unregisterFiles(availableFiles); // Use the unregisterFiles method
                } catch (RemoteException e) {
                    System.err.println("Error during shutdown hook unregister: " + e.getMessage());
                }
                System.out.println("Daemon shutting down, unregistered files.");
            }));

            // Start listening for download requests
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new FragmentHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Daemon exception: " + e.getMessage());
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

        try {
            DaemonImpl daemonimpl = new DaemonImpl(directoryHost, daemonPort, daemonAddress, availableFiles);
            //Remove this since you do not need it when implementing   UnicastRemoteObject
            //UnicastRemoteObject.exportObject(daemon, 0); // Export the object

            daemonimpl.start();
        } catch (RemoteException e) {
            System.err.println("Daemon exception: " + e.getMessage());
        }
    }
}