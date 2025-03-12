package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIClientInterface extends Remote {

    public void updateAdminConsole(String message) throws RemoteException;
}
