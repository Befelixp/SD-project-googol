package meta1sd;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.Naming;
import java.util.Properties;
import org.jsoup.Jsoup;

public class Downloader {

    public static void main(String[] args) {
        String registryN;

        try {
            Downloader downloader = new Downloader();
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            prop.load(input);
            registryN = prop.getProperty("registryN");
            try {
                // Parte do RMI
                RMIGatewayDownloaderInterface gateway = (RMIGatewayDownloaderInterface) Naming.lookup(registryN);
            } catch (Exception e) {
            }
        } catch (Exception e) {

        }
    }
}
