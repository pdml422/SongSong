import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.*;
import java.util.*;

public class DirectoryImpl extends UnicastRemoteObject implements Directory {
    private final Map<String, List<String>> fileLocations; // fileName -> List of "address:port"

    public DirectoryImpl() throws RemoteException {
        super();
        fileLocations = new HashMap<>();
    }

    @Override
    public void registerFile(String fileName, String clientAddress, int clientPort) throws RemoteException {
        String location = clientAddress + ":" + clientPort;
        fileLocations.computeIfAbsent(fileName, k -> new ArrayList<>()).add(location);
        System.out.println("Registered file '" + fileName + "' from client at " + location);
    }

    @Override
    public List<String> getClientsForFile(String fileName) throws RemoteException {
        return fileLocations.getOrDefault(fileName, new ArrayList<>());
    }

    @Override
    public Set<String> getAvailableFiles() throws RemoteException {
        return fileLocations.keySet();
    }

    public static void main(String[] args) {
        try {
            DirectoryImpl directory = new DirectoryImpl();
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("FileDirectory", directory);
            System.out.println("Directory server started on port " + 1099);
        } catch (Exception e) {
            System.err.println("Directory server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
