package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GatewayDownloaderInterface extends Remote {

    public void queueUrls(String url) throws InterruptedException, RemoteException;

}
