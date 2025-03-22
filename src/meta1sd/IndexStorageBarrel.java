package meta1sd;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class IndexStorageBarrel extends UnicastRemoteObject implements RMIIndexStorageBarrel {
    private int barrelId;
    private Map<Integer, RMIIndexStorageBarrel> barrels = new HashMap<>();

    public IndexStorageBarrel(int barrelId) throws RemoteException {
        this.barrelId = barrelId;
        System.out.println(LocalDateTime.now() + " : System " + barrelId + " is starting up");
    }

    public void registeroneIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException {
        // Não registra se for ela mesma
        if (id != this.barrelId) {
            barrels.put(id, barrel);
            System.out.println("Guardando a barrel " + id);
        } else {
            System.out.println("Ignorando registro da própria barrel " + id);
        }
    }

    public void registerallIBS(Map<Integer, RMIIndexStorageBarrel> barrells, int myid,
            RMIIndexStorageBarrel mybarrel) throws RemoteException {
        System.out.println("Registrando ibs");

        barrells.forEach((barid, barr) -> {
            try {
                if (barid != this.barrelId) {
                    this.registeroneIBS(barid, barr);
                    barr.registeroneIBS(myid, mybarrel);
                    barr.gatewaypong("Barrel" + myid);
                    System.out.println("Registrada na barrel " + barid);
                }
            } catch (RemoteException e) {
                System.err.println("Falha ao registrar na barrel " + barid + "!");
                e.printStackTrace();
            }
        });
    }

    public void gatewaypong(String provider) throws RemoteException {
        System.out.println(provider + ":Pong");
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
