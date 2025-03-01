import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface Daemon extends Remote {
    void registerFiles() throws RemoteException;
    void unregisterFiles(Set<String> files) throws RemoteException;
    byte[] getFileFragment(String fileName, int fragmentNumber) throws RemoteException;
    String getDaemonAddress() throws RemoteException;
    int getDaemonPort() throws RemoteException;
}