import java.io.*;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class DownloadClient {

    private final String directoryHost;
    private final int directoryPort = 4000;
    private final int fragmentSize = 1024 * 1024; // 1MB

    public DownloadClient(String directoryHost) {
        this.directoryHost = directoryHost;
    }

    public void downloadFile(String fileName, String destinationPath) {
        long startTime = System.currentTimeMillis(); // Record start time
        try {
            // 1. Get daemon locations from the Directory
            Registry registry = LocateRegistry.getRegistry(directoryHost, directoryPort);
            DirectoryService directoryService = (DirectoryService) registry.lookup("directory_service");
            List<String> daemonLocations = directoryService.getDaemonLocations(fileName);

            if (daemonLocations.isEmpty()) {
                System.out.println("No daemons have the file: " + fileName);
                return;
            } else {
                System.out.println("The file is located in following Daemons: " + daemonLocations);
            }

            // 2. Determine file size (get from first daemon)
            long fileSize = getFileSize(daemonLocations.get(0), fileName);
            if (fileSize == -1) {
                System.err.println("Could not determine file size.");
                return;
            }

            // 3. Calculate number of fragments
            int numFragments = (int) Math.ceil((double) fileSize / fragmentSize);

            // 4. Download fragments in parallel
            byte[][] fragments = new byte[numFragments][];
            Thread[] threads = new Thread[numFragments];

            for (int i = 0; i < numFragments; i++) {
                final int fragmentNumber = i;
                String daemonLocation = daemonLocations.get(i % daemonLocations.size()); // Distribute requests
                threads[i] = new Thread(() -> {
                    fragments[fragmentNumber] = downloadFragment(daemonLocation, fileName, fragmentNumber);
                });
                threads[i].start();
            }

            // 5. Wait for all threads to complete
            for (int i = 0; i < numFragments; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    System.err.println("Thread interrupted: " + e.getMessage());
                }
            }

            // 6. Assemble the fragments
            assembleFile(fragments, destinationPath);

            System.out.println("File downloaded successfully to: " + destinationPath);

        } catch (Exception e) {
            System.err.println("Download failed: " + e.getMessage());
        } finally {
            long endTime = System.currentTimeMillis(); // Record end time
            long downloadTime = endTime - startTime; // Calculate download time
            System.out.println("Download time: " + downloadTime + " ms"); // Print download time
        }
    }

    private long getFileSize(String daemonLocation, String fileName) {
        String[] parts = daemonLocation.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        try (
                Socket socket = new Socket(host, port);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        ) {
            oos.writeObject("GET_FILE_SIZE"); // Special request to get file size
            oos.writeObject(fileName);
            return (long) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error getting file size from " + daemonLocation + ": " + e.getMessage());
            return -1;
        }
    }

    private byte[] downloadFragment(String daemonLocation, String fileName, int fragmentNumber) {
        String[] parts = daemonLocation.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        try (
                Socket socket = new Socket(host, port);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        ) {
            oos.writeObject(fileName);
            oos.writeObject(fragmentNumber);
            return (byte[]) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error downloading fragment " + fragmentNumber + " from " + daemonLocation + ": " + e.getMessage());
            return null;
        }
    }

    private void assembleFile(byte[][] fragments, String destinationPath) {
        try (FileOutputStream fos = new FileOutputStream(destinationPath)) {
            for (byte[] fragment : fragments) {
                if (fragment != null) {
                    fos.write(fragment);
                } else {
                    System.err.println("Missing fragment, file may be incomplete!");
                }
            }
        } catch (IOException e) {
            System.err.println("Error assembling file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: DownloadClient <directoryHost> <fileName> <destinationPath>");
            System.exit(1);
        }

        String directoryHost = args[0];
        String fileName = args[1];
        String destinationPath = args[2];

        DownloadClient client = new DownloadClient(directoryHost);
        client.downloadFile(fileName, destinationPath);
    }
}