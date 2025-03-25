package meta1sd;

import java.io.*;
import java.rmi.*;
import java.util.*;

/**
 * RMIClient - Classe responsável por interagir com o gateway RMI para indexação
 * de URLs e busca de termos.
 */
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

    /**
     * Imprime um separador estilizado no console.
     */
    private void printSeparator() {
        System.out.println(PURPLE + "═══════════════════════════════════════════════════════════" + RESET);
    }

    /**
     * Imprime o menu de opções disponíveis para o usuário.
     */
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

    /**
     * Limpa a tela do console.
     */
    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Executa a opção selecionada pelo usuário.
     * 
     * @param gateway Interface do gateway RMI.
     * @param option  Opção selecionada pelo usuário.
     * @throws RemoteException      Se ocorrer um erro de comunicação remota.
     * @throws InterruptedException Se a operação for interrompida.
     */
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
                String terms = sc.nextLine().trim();

                if (terms.isEmpty()) {
                    System.out.println(RED + "\n⚠️ Please enter at least one search term!" + RESET);
                    break;
                }

                try {
                    System.out.println(PURPLE + "\nSearching..." + RESET);

                    // Usar diretamente a string de termos
                    List<String> results = new ArrayList<>();

                    try {
                        results = gateway.returnPagesbyWords(terms); // Passar a string diretamente
                    } catch (Exception e) {
                        // Log do erro internamente (pode ser salvo em um arquivo de log)
                        System.err.println("Search error: " + e.getMessage());
                        results = new ArrayList<>(); // Garantir que results não seja null
                    }

                    if (results == null || results.isEmpty()) {
                        System.out.println(YELLOW + "\nℹ️ No results found for your search." + RESET);
                        System.out.println(YELLOW + "Tips:" + RESET);
                        System.out.println("• Check if all words are spelled correctly");
                        System.out.println("• Try using fewer or different keywords");
                        System.out.println("• Try more general terms");
                        break;
                    }

                    int totalResults = results.size();
                    int pageSize = 10;
                    int totalPages = (int) Math.ceil((double) totalResults / pageSize);
                    int currentPage = 1;

                    while (true) {
                        try {
                            clearScreen();
                            printSeparator();
                            System.out.println(CYAN + "           SEARCH RESULTS" + RESET);
                            printSeparator();
                            System.out.println(YELLOW + "Search terms: " + RESET + terms);
                            System.out.println(YELLOW + "Total results: " + RESET + totalResults);
                            System.out.printf(YELLOW + "Page " + RESET + "%d of %d\n", currentPage, totalPages);
                            printSeparator();

                            // Calcular índices para a página atual
                            int startIndex = (currentPage - 1) * pageSize;
                            int endIndex = Math.min(startIndex + pageSize, totalResults);

                            // Mostrar resultados da página atual
                            for (int i = startIndex; i < endIndex; i++) {
                                System.out.printf(GREEN + "[%2d]" + RESET + " %s\n", (i + 1), results.get(i));
                            }

                            printSeparator();
                            System.out.println(YELLOW + "Navigation:" + RESET);
                            System.out.println(GREEN + "[N]" + RESET + "ext page    " +
                                    GREEN + "[P]" + RESET + "revious page    " +
                                    GREEN + "[G]" + RESET + "o to page    " +
                                    RED + "[Q]" + RESET + "uit");
                            System.out.print("\nEnter your choice: ");

                            String choice = sc.nextLine().trim().toUpperCase();

                            switch (choice) {
                                case "N":
                                    if (currentPage < totalPages) {
                                        currentPage++;
                                    } else {
                                        System.out.println(YELLOW + "\nℹ️ Already on the last page!" + RESET);
                                        Thread.sleep(1000);
                                    }
                                    break;

                                case "P":
                                    if (currentPage > 1) {
                                        currentPage--;
                                    } else {
                                        System.out.println(YELLOW + "\nℹ️ Already on the first page!" + RESET);
                                        Thread.sleep(1000);
                                    }
                                    break;

                                case "G":
                                    System.out.print("\nEnter page number (1-" + totalPages + "): ");
                                    try {
                                        int pageNum = Integer.parseInt(sc.nextLine());
                                        if (pageNum >= 1 && pageNum <= totalPages) {
                                            currentPage = pageNum;
                                        } else {
                                            System.out.println(YELLOW + "\nℹ️ Please enter a number between 1 and "
                                                    + totalPages + RESET);
                                            Thread.sleep(1000);
                                        }
                                    } catch (NumberFormatException e) {
                                        System.out.println(YELLOW + "\nℹ️ Please enter a valid number!" + RESET);
                                        Thread.sleep(1000);
                                    }
                                    break;

                                case "Q":
                                    return;

                                default:
                                    System.out.println(YELLOW + "\nℹ️ Invalid option!" + RESET);
                            }
                        } catch (Exception e) {
                            System.out.println(
                                    YELLOW + "\nℹ️ An error occurred while displaying results. Returning to main menu."
                                            + RESET);
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println(YELLOW
                            + "\nℹ️ Unable to complete the search at this time. Please try again later." + RESET);
                }
                break;

            case 3:
                System.out.println(CYAN + "           LINKED PAGES" + RESET);
                printSeparator();
                System.out.print(YELLOW + "Enter the URL to find linked pages: " + RESET);
                String pageUrl = sc.nextLine();
                System.out.println(PURPLE + "\nFetching linked pages..." + RESET);

                try {
                    List<String> linkedUrls = gateway.returnLinkedUrls(pageUrl);

                    if (linkedUrls == null || linkedUrls.isEmpty()) {
                        System.out.println(RED + "\n❌ No linked pages found for this URL." + RESET);
                    } else {
                        int totalResults = linkedUrls.size();
                        int pageSize = 10;
                        int totalPages = (int) Math.ceil((double) totalResults / pageSize);
                        int currentPage = 1;

                        while (true) {
                            clearScreen();
                            printSeparator();
                            System.out.println(CYAN + "           LINKED PAGES RESULTS" + RESET);
                            printSeparator();
                            System.out.println(YELLOW + "Source URL: " + RESET + pageUrl);
                            System.out.println(YELLOW + "Total linked pages: " + RESET + totalResults);
                            System.out.printf(YELLOW + "Page " + RESET + "%d of %d\n", currentPage, totalPages);
                            printSeparator();

                            // Calcular índices para a página atual
                            int startIndex = (currentPage - 1) * pageSize;
                            int endIndex = Math.min(startIndex + pageSize, totalResults);

                            // Mostrar resultados da página atual
                            for (int i = startIndex; i < endIndex; i++) {
                                System.out.printf(GREEN + "[%2d]" + RESET + " %s\n", (i + 1), linkedUrls.get(i));
                            }

                            printSeparator();
                            System.out.println(YELLOW + "Navigation:" + RESET);
                            System.out.println(GREEN + "[N]" + RESET + "ext page    " +
                                    GREEN + "[P]" + RESET + "revious page    " +
                                    GREEN + "[G]" + RESET + "o to page    " +
                                    RED + "[Q]" + RESET + "uit");
                            System.out.print("\nEnter your choice: ");

                            String choice = sc.nextLine().trim().toUpperCase();

                            switch (choice) {
                                case "N":
                                    if (currentPage < totalPages) {
                                        currentPage++;
                                    } else {
                                        System.out.println(RED + "\n⚠ Already on the last page!" + RESET);
                                        Thread.sleep(1500);
                                    }
                                    break;

                                case "P":
                                    if (currentPage > 1) {
                                        currentPage--;
                                    } else {
                                        System.out.println(RED + "\n⚠ Already on the first page!" + RESET);
                                        Thread.sleep(1500);
                                    }
                                    break;

                                case "G":
                                    System.out.print("\nEnter page number (1-" + totalPages + "): ");
                                    try {
                                        int pageNum = Integer.parseInt(sc.nextLine());
                                        if (pageNum >= 1 && pageNum <= totalPages) {
                                            currentPage = pageNum;
                                        } else {
                                            System.out.println(RED + "\n❌ Invalid page number!" + RESET);
                                            Thread.sleep(1500);
                                        }
                                    } catch (NumberFormatException e) {
                                        System.out.println(RED + "\n❌ Please enter a valid number!" + RESET);
                                        Thread.sleep(1500);
                                    }
                                    break;

                                case "Q":
                                    return;

                                default:
                                    System.out.println(RED + "\n❌ Invalid option!" + RESET);
                            }
                        }
                    }
                } catch (RemoteException e) {
                    System.out.println(RED + "\n❌ Error while fetching linked pages: " + e.getMessage() + RESET);
                }
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

    /**
     * Verifica se a entrada do usuário é um número inteiro.
     * 
     * @param sc Scanner para ler a entrada do usuário.
     * @return O número inteiro lido.
     */
    private static int isIntger(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.println(RED + "❌ Please enter a valid number!" + RESET);
            System.out.print(YELLOW + "Enter your choice: " + RESET);
            sc.next();
        }
        int opt = sc.nextInt();
        return opt;
    }

    /**
     * Inicializa o cliente e exibe o menu principal.
     */
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

    /**
     * Método principal para executar o cliente RMI.
     * 
     * @param args Argumentos da linha de comando, incluindo o ID do cliente e o
     *             arquivo de propriedades.
     */
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

        } catch (Exception e) {
            System.out.println("Erro fatal: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}