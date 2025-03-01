import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class DirectoryServer {
    public static void main(String[] args) {
        try {
            DirectoryService directoryService = new DirectoryServiceImpl();
            Registry registry = LocateRegistry.createRegistry(4000);
            registry.bind("directory_service", directoryService);
            System.out.println("Directory server started.");
        } catch (Exception e) {
            System.err.println("Directory server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}