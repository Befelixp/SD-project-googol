package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIGatewayDownloaderInterface extends Remote {

    public void queueUrls(String url) throws InterruptedException, RemoteException;

    public String popqueue() throws InterruptedException, RemoteException;
}
