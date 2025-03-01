import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectoryServiceImpl extends UnicastRemoteObject implements DirectoryService {

    private final Map<String, List<Daemon>> fileLocations; // fileName -> list of DaemonInterface

    public DirectoryServiceImpl() throws RemoteException {
        fileLocations = new HashMap<>();
    }

    @Override
    public synchronized void registerFile(String fileName, Daemon daemon) throws RemoteException {
        if (!fileLocations.containsKey(fileName)) {
            fileLocations.put(fileName, new ArrayList<>());
        }
        List<Daemon> daemons = fileLocations.get(fileName);
        if (!daemons.contains(daemon)) {
            daemons.add(daemon);
        }
        System.out.println("Registered " + fileName + " at " + daemon.getDaemonAddress() + ":" + daemon.getDaemonPort());
    }

    @Override
    public synchronized void unregisterFile(String fileName, Daemon daemon) throws RemoteException {
        if (fileLocations.containsKey(fileName)) {
            List<Daemon> daemons = fileLocations.get(fileName);
            daemons.remove(daemon);
            if (daemons.isEmpty()) {
                fileLocations.remove(fileName);
            }
            System.out.println("Unregistered " + fileName + " at " + daemon.getDaemonAddress() + ":" + daemon.getDaemonPort());
        }
    }

    @Override
    public synchronized List<Daemon> getDaemonLocations(String fileName) throws RemoteException {
        return fileLocations.getOrDefault(fileName, new ArrayList<>()); // Return an empty list if no daemons have the file
    }
}