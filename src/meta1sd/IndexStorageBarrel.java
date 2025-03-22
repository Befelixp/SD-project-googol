package meta1sd;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.Properties;

public class IndexStorageBarrel extends UnicastRemoteObject implements RMIIndexStorageBarrel {
    private int barrelId;

    public IndexStorageBarrel(int barrelId) throws RemoteException {
        this.barrelId = barrelId;
        System.out.println(LocalDateTime.now() + " : System is starting up");
    }

    public void gatewaypong() throws RemoteException {
        System.out.println("Pong");
    }

    public static void main(String[] args) {
        String registryNibs;

        try {
            IndexStorageBarrel barrel = new IndexStorageBarrel(Integer.parseInt(args[0]));
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            System.out.println("Carregando arquivo de propriedades");
            prop.load(input);
            System.out.println("Arquivo de propriedades carregado");
            registryNibs = prop.getProperty("registryNibs");
            System.out.println("registryN: " + registryNibs);
            RMIGatewayIBSDownloader gateway = null;

            try {
                gateway = (RMIGatewayIBSDownloader) Naming.lookup(registryNibs);
                gateway.registerIBS(barrel.barrelId, barrel);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
