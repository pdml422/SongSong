import java.io.*;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Downloader {
    private final String directoryHost;
    private static final int FRAGMENT_SIZE = 1024 * 1024; // 1MB

    public Downloader(String directoryHost) {
        this.directoryHost = directoryHost;
    }

    public void downloadFileParallel(String fileName, String destinationPath) {
        try {
            Registry registry = LocateRegistry.getRegistry(directoryHost, 1099);
            Directory directory = (Directory) registry.lookup("FileDirectory");

            List<String> clientLocations = directory.getClientsForFile(fileName);
            if (clientLocations.isEmpty()) {
                System.out.println("No clients found for file: " + fileName);
                return;
            }

            System.out.println("Found clients for '" + fileName + "': " + clientLocations);

            // For simplicity, let's use a fixed number of sources (e.g., up to 3) or all available
            int numSources = Math.min(clientLocations.size(), 3); // Limit sources for testing
            List<String> downloadSources = clientLocations.subList(0, numSources);

            long startTime = System.currentTimeMillis();

            ExecutorService executorService = Executors.newFixedThreadPool(numSources);
            List<Future<?>> futures = new ArrayList<>();

            for (String sourceLocation : downloadSources) {
                futures.add(executorService.submit(() -> downloadFragment(fileName, sourceLocation, destinationPath)));
            }

            for (Future<?> future : futures) {
                future.get(); // Wait for all download tasks to complete
            }

            executorService.shutdown();

            long endTime = System.currentTimeMillis();
            System.out.println("Parallel download of '" + fileName + "' completed in " + (endTime - startTime) + "ms from " + numSources + " sources.");


        } catch (Exception e) {
            System.err.println("Downloader exception: " + e.toString());
            e.printStackTrace();
        }
    }

    private void downloadFragment(String fileName, String sourceLocation, String destinationPath) {
        String[] parts = sourceLocation.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        try (Socket socket = new Socket(host, port);
             OutputStream fileOutputStream = new FileOutputStream(destinationPath + "_" + sourceLocation.replace(":", "_") + ".part"); // Save fragments separately for now
             InputStream inputStream = socket.getInputStream()) {

            java.io.PrintWriter writer = new java.io.PrintWriter(socket.getOutputStream(), true);
            writer.println(fileName); // Send file request

            byte[] buffer = new byte[FRAGMENT_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            System.out.println("Downloaded fragment from " + sourceLocation + " for file '" + fileName + "'");


        } catch (IOException e) {
            System.err.println("Error downloading from " + sourceLocation + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: DownloadClient <directoryHost> <fileName> <destinationPath>");
            return;
        }
        String directoryHost = args[0];
        String fileName = args[1];
        String destinationPath = args[2];

        Downloader downloader = new Downloader(directoryHost);
        downloader.downloadFileParallel(fileName, destinationPath);
        System.out.println("Download process initiated for '" + fileName + "' to '" + destinationPath + "'");
    }
}