package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RMIIndexStorageBarrel extends Remote {

    public void gatewaypong(String provider) throws RemoteException;

    public void storeSiteData(SiteData siteData) throws RemoteException;

    public List<String> searchPagesByWords(Set<String> words) throws RemoteException;

    public void registerallIBS(Map<Integer, RMIIndexStorageBarrel> barrells, int myid,
            RMIIndexStorageBarrel mybarrel) throws RemoteException;

    public void registeroneIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException;

    public Map<String, List<String>> getIncomingLinksForUrl(String url);
}
