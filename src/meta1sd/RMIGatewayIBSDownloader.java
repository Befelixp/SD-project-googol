package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RMIGatewayIBSDownloader extends Remote {

    public void registerIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException;

    public Map<Integer, RMIIndexStorageBarrel> getBarrels() throws RemoteException;

    public RMIIndexStorageBarrel getRandomBarrel() throws RemoteException;
}
