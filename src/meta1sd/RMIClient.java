package meta1sd;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;

public class RMIClient {
    private Scanner sc = new Scanner(System.in);
    private int id, characterLimit;
    private GatewayClientInterface gateway;

    private void printmenu() {
        System.out.println("--------Menu--------");
        System.out.println("1) Index URL");
        System.out.println("2) Search for terms");
        System.out.println("3) List pages linked to a specific page");
        System.out.println("4) Administration page");
        System.out.println("5) Exit");
        System.out.println("--------------------");
        System.out.println("Choose an option:");
    }

    public void opt(GatewayClientInterface gateway, int option) throws RemoteException {

    }

    private static int isIntger(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.println("It must be an integer!");
            sc.next();
        }
        int opt = sc.nextInt();
        return opt;
    }

    public static void main(String[] args) {
        String registryN;
        try {
            RMIClient client = new RMIClient();
            client.id = Integer.parseInt(args[0]);

            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            prop.load(input);

            registryN = prop.getProperty("registryN");
            client.characterLimit = Integer.parseInt(prop.getProperty("limChar"));

            client.gateway = (GatewayClientInterface) Naming.lookup(registryN);

        } catch (Exception e) {
            System.out.println("Gateway Unreachable");
        }

    }
}
