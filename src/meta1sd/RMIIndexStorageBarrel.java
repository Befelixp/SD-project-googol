package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public interface RMIIndexStorageBarrel extends Remote {

    public void gatewaypong(String provider) throws RemoteException;

    public void registerallIBS(Map<Integer, RMIIndexStorageBarrel> barrells, int myid,
            RMIIndexStorageBarrel mybarrel) throws RemoteException;

    public void registeroneIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException;
}
