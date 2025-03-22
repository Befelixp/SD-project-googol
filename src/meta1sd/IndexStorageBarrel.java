package meta1sd;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;

public class IndexStorageBarrel {

    public static void main(String[] args) {
        String registryNibs;

        try {
            IndexStorageBarrel barrel = new IndexStorageBarrel();
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            System.out.println("Carregando arquivo de propriedades");
            prop.load(input);
            System.out.println("Arquivo de propriedades carregado");
            registryNibs = prop.getProperty("registryNibs");
            System.out.println("registryN: " + registryNibs);

            RMIGatewayDownloaderInterface gateway = null;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
