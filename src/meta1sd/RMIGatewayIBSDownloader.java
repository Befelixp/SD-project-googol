package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIGatewayIBSDownloader extends Remote {

    public void registerIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException;

}
