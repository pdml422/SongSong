import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectoryServiceImpl extends UnicastRemoteObject implements DirectoryService {

    private static final long serialVersionUID = 1L;
    private final Map<String, List<String>> fileLocations; // fileName -> list of daemon addresses:ports

    public DirectoryServiceImpl() throws RemoteException {
        super();
        fileLocations = new HashMap<>();
    }

    @Override
    public synchronized void registerFile(String fileName, String daemonAddress, int daemonPort) throws RemoteException {
        String location = daemonAddress + ":" + daemonPort;
        if (!fileLocations.containsKey(fileName)) {
            fileLocations.put(fileName, new ArrayList<>());
        }
        List<String> locations = fileLocations.get(fileName);
        if (!locations.contains(location)) {
            locations.add(location);
        }
        System.out.println("Registered " + fileName + " at " + location);
    }

    @Override
    public synchronized void unregisterFile(String fileName, String daemonAddress, int daemonPort) throws RemoteException {
        String location = daemonAddress + ":" + daemonPort;
        if (fileLocations.containsKey(fileName)) {
            List<String> locations = fileLocations.get(fileName);
            locations.remove(location);
            if (locations.isEmpty()) {
                fileLocations.remove(fileName);
            }
            System.out.println("Unregistered " + fileName + " at " + location);
        }
    }

    @Override
    public synchronized List<String> getDaemonLocations(String fileName) throws RemoteException {
        return fileLocations.getOrDefault(fileName, new ArrayList<>()); // Return an empty list if no daemons have the file
    }
}