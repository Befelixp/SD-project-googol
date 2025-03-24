package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RMIGatewayClientInterface extends Remote {

    public void clientIndexUrl(String url) throws InterruptedException, RemoteException;

    public List<String> returnPagesbyWords(String words) throws RemoteException;

    public List<String> returnLinkedUrls(String url) throws RemoteException;
    
}
