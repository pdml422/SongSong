import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

public interface Directory extends Remote {
    void registerFile(String fileName, String clientAddress, int clientPort) throws RemoteException;
    List<String> getClientsForFile(String fileName) throws RemoteException;
    Set<String> getAvailableFiles() throws RemoteException;
}
