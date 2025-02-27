import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface DirectoryService extends Remote {
    void registerFile(String fileName, String daemonAddress, int daemonPort) throws RemoteException;
    List<String> getDaemonLocations(String fileName) throws RemoteException;
    void unregisterFile(String fileName, String daemonAddress, int daemonPort) throws RemoteException;

}