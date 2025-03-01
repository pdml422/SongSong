import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface DirectoryService extends Remote {
    void registerFile(String fileName, String daemonAddress, int daemonPort) throws RemoteException;
    List<String> getDaemonLocations(String fileName) throws RemoteException;
}