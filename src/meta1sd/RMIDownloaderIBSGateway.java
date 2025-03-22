package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface RMIDownloaderIBSGateway extends Remote {

    public void registerIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException;

    public void registerExistingIBS(Map<Integer, RMIIndexStorageBarrel> barrells) throws RemoteException;

}
