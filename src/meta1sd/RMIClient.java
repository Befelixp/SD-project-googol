package meta1sd;

import java.io.*;
import java.rmi.*;
import java.util.*;

public class RMIClient {
    private Scanner sc = new Scanner(System.in);
    private int id, characterLimit;
    private RMIGatewayClientInterface gateway;
    private boolean connected;

    // Cores ANSI
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String PURPLE = "\u001B[35m";

    private void printSeparator() {
        System.out.println(PURPLE + "═══════════════════════════════════════════════════════════" + RESET);
    }

    private void printmenu() {
        clearScreen();
        printSeparator();
        System.out.println(CYAN + "                    SEARCH ENGINE CLIENT" + RESET);
        printSeparator();
        System.out.println(YELLOW + "                     Available Options" + RESET);
        System.out.println();
        System.out.println(GREEN + " [1] " + RESET + "Index a New URL");
        System.out.println(GREEN + " [2] " + RESET + "Search for Terms");
        System.out.println(GREEN + " [3] " + RESET + "List Linked Pages");
        System.out.println(GREEN + " [4] " + RESET + "Administration Panel");
        System.out.println(RED + " [5] " + RESET + "Exit Application");
        printSeparator();
        System.out.print(YELLOW + "Enter your choice: " + RESET);
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void opt(RMIGatewayClientInterface gateway, int option) throws RemoteException, InterruptedException {
        clearScreen();
        printSeparator();

        switch (option) {
            case 1:
                System.out.println(CYAN + "           INDEX NEW URL" + RESET);
                printSeparator();
                System.out.print(YELLOW + "Enter the URL to index: " + RESET);
                String url = sc.nextLine();
                System.out.println(PURPLE + "\nProcessing request..." + RESET);
                gateway.clientIndexUrl(url);
                System.out.println(GREEN + "\n✓ URL successfully submitted for indexing!" + RESET);
                break;

            case 2:
                System.out.println(CYAN + "           SEARCH TERMS" + RESET);
                printSeparator();
                System.out.print(YELLOW + "Enter search terms (separated by spaces): " + RESET);
                String terms = sc.nextLine();
                System.out.println(PURPLE + "\nSearching..." + RESET);
                // gateway.searchTerms(terms);
                break;

            case 3:
                System.out.println(CYAN + "           LINKED PAGES" + RESET);
                printSeparator();
                System.out.print(YELLOW + "Enter the URL to find linked pages: " + RESET);
                String pageUrl = sc.nextLine();
                System.out.println(PURPLE + "\nFetching linked pages..." + RESET);
                // gateway.getLinkedPages(pageUrl);
                break;

            case 4:
                System.out.println(CYAN + "           ADMINISTRATION PANEL" + RESET);
                printSeparator();
                System.out.println(RED + "⚠ This feature is currently not implemented." + RESET);
                break;

            case 5:
                System.out.println(CYAN + "           EXITING APPLICATION" + RESET);
                printSeparator();
                System.out.println(YELLOW + "Thank you for using the Search Engine Client!" + RESET);
                break;

            default:
                System.out.println(RED + "❌ Invalid option selected!" + RESET);
        }

        if (option != 5) {
            System.out.println("\nPress ENTER to continue...");
            sc.nextLine();
        }
    }

    private static int isIntger(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.println(RED + "❌ Please enter a valid number!" + RESET);
            System.out.print(YELLOW + "Enter your choice: " + RESET);
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
                System.out.println(RED + "\n❌ Error communicating with server: " + e.getMessage() +
                        "\n   Your indexation was not completed!" + RESET);
                this.connected = false;
                break;
            } catch (InterruptedException e) {
                System.out.println(RED + "\n❌ Operation was interrupted: " + e.getMessage() + RESET);
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        try {
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

            // Tentativas de conexão com a gateway
            int maxRetries = Integer.parseInt(prop.getProperty("maxRetries"));
            int currentRetry = 0;

            while (!client.connected) {
                try {
                    System.out.println("Tentativa " + (currentRetry + 1) + " de " + maxRetries +
                            " para conectar à gateway...");

                    client.gateway = (RMIGatewayClientInterface) Naming.lookup(registryN);
                    client.connected = true;
                    System.out.println("Conectado com sucesso à gateway: " + registryN);

                    // Reset do contador de tentativas após conexão bem-sucedida
                    currentRetry = 0;

                    // Iniciar o cliente
                    client.initclient();

                } catch (RemoteException e) {
                    currentRetry++;
                    if (currentRetry >= maxRetries) {
                        System.out.println("\nFalha ao conectar com a gateway após " + maxRetries + " tentativas.");
                        System.out.print("Deseja continuar tentando? (S/N): ");
                        String resposta = client.sc.nextLine().trim().toUpperCase();

                        if (resposta.equals("S")) {
                            currentRetry = 0; // Reset do contador de tentativas
                            System.out.println("Reiniciando tentativas de conexão...");
                            continue;
                        } else {
                            System.out.println("Encerrando o programa...");
                            System.exit(1);
                        }
                    }

                    System.out.println("Falha ao conectar com a gateway: " + e.getMessage());
                    System.out.println("Aguardando 5 segundos antes da próxima tentativa...");
                    Thread.sleep(5000); // Espera 5 segundos

                } catch (Exception e) {
                    System.out.println("Erro inesperado: " + e.getMessage());
                    e.printStackTrace();
                    System.out.print("Deseja continuar tentando? (S/N): ");
                    String resposta = client.sc.nextLine().trim().toUpperCase();

                    if (resposta.equals("S")) {
                        currentRetry = 0; // Reset do contador de tentativas
                        System.out.println("Reiniciando tentativas de conexão...");
                        continue;
                    } else {
                        System.out.println("Encerrando o programa...");
                        System.exit(1);
                    }
                }
            }

        } catch (

        Exception e) {
            System.out.println("Erro fatal: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
