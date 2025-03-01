import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface DirectoryService extends Remote {
    void registerFile(String fileName, Daemon daemon) throws RemoteException;
    List<Daemon> getDaemonLocations(String fileName) throws RemoteException;
    void unregisterFile(String fileName, Daemon daemon) throws RemoteException;
}