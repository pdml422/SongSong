import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectoryServiceImpl extends UnicastRemoteObject implements DirectoryService {

    // Map<filename, list<daemon_addresses>>
    private final Map<String, List<String>> fileLocations;

    public DirectoryServiceImpl() throws RemoteException {
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
    public synchronized List<String> getDaemonLocations(String fileName) throws RemoteException {
        // Return a list according to fileName or an empty one in 404 case
        return fileLocations.getOrDefault(fileName, new ArrayList<>());
    }
}