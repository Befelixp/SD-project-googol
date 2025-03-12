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
import java.io.IOException;

public class RMIClient {
    private Scanner sc = new Scanner(System.in);
    private int id, characterLimit;
    private RMIGatewayClientInterface gateway;

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

    // Adicionado InterruptedException à declaração do método
    public void opt(RMIGatewayClientInterface gateway, int option) throws RemoteException, InterruptedException {
        switch (option) {
            case 1:
                System.out.println("Enter the URL to index:");
                String url = sc.nextLine();
                gateway.clientIndexUrl(url);
                break;
            case 2:
                System.out.println("Enter search terms (separated by spaces):");
                String terms = sc.nextLine();
                // Assumindo que existe um método para buscar termos
                // gateway.searchTerms(terms);
                break;
            case 3:
                System.out.println("Enter the URL to find linked pages:");
                String pageUrl = sc.nextLine();
                // Assumindo que existe um método para listar páginas vinculadas
                // gateway.getLinkedPages(pageUrl);
                break;
            case 4:
                System.out.println("Administration page - functionality not implemented");
                break;
            case 5:
                System.out.println("Exiting...");
                break;
            default:
                System.out.println("Invalid option!");
        }
    }

    private static int isIntger(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.println("It must be an integer!");
            sc.next();
        }
        int opt = sc.nextInt();
        return opt;
    }

    private void initclient() {
        int option = 0;

        while (option != 5) {
            printmenu();
            option = isIntger(sc);
            sc.nextLine(); // Consumir a nova linha após ler o inteiro
            try {
                opt(this.gateway, option);
            } catch (RemoteException e) {
                System.out.println("Error communicating with server: " + e.getMessage());
            } catch (InterruptedException e) { // Adicionado tratamento para InterruptedException
                System.out.println("Operation was interrupted: " + e.getMessage());
                Thread.currentThread().interrupt(); // Preserva o status de interrupção
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Verificar argumentos
            if (args.length < 2) {
                System.out.println("Usage: java meta1sd.RMIClient <clientID> <propertiesFile>");
                System.exit(1);
            }

            RMIClient client = new RMIClient();
            client.id = Integer.parseInt(args[0]);

            // Carregar propriedades
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            prop.load(input);
            input.close();

            String registryN = prop.getProperty("registryN");
            client.characterLimit = Integer.parseInt(prop.getProperty("limChar"));

            // Conectar ao gateway
            client.gateway = (RMIGatewayClientInterface) Naming.lookup(registryN);
            System.out.println("Connected to gateway: " + registryN);

            // Iniciar o cliente
            client.initclient();

        } catch (Exception e) {
            System.out.println("Gateway Unreachable: " + e.getMessage());
            e.printStackTrace();
        }
    }
}