package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GatewayClientInterface extends Remote {

    public void clientIndexUrl(String url) throws InterruptedException;

}
    