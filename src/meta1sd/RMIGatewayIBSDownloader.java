package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface RMIGatewayIBSDownloader extends Remote {

    public void registerIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException;

    public void registerDownloader(int id, RMIDownloaderIBSGateway downloader) throws RemoteException;

    public Map<Integer, RMIIndexStorageBarrel> getBarrels() throws RemoteException;
}
