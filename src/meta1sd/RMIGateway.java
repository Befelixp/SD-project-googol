package meta1sd;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;

public class RMIGateway extends UnicastRemoteObject
        implements RMIGatewayClientInterface, RMIGatewayDownloaderInterface, RMIGatewayIBSDownloader {
    private LinkedBlockingQueue<String> urlQueue;
    private int urlSearchCount, urlSearchDepth;
    private HashSet<String> isqueued;
    private Map<Integer, RMIIndexStorageBarrel> barrels = new HashMap<>();
    private Random random = new Random();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public RMIGateway() throws RemoteException {
        urlQueue = new LinkedBlockingQueue<>();
        isqueued = new HashSet<>();
    }

    private String getTimestamp() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    public RMIIndexStorageBarrel getRandomBarrel() throws RemoteException {
        if (barrels.isEmpty()) {
            System.out.println(
                    getTimestamp() + " : ⚠️ Tentativa de obter barrel aleatória, mas não há barrels registradas");
            return null;
        }

        // Criar uma cópia das chaves para evitar ConcurrentModificationException
        List<Integer> barrelIds = new ArrayList<>(barrels.keySet());

        // Tentar até encontrar uma barrel funcional ou esgotar todas as opções
        while (!barrelIds.isEmpty()) {
            // Selecionar um ID aleatório
            int randomIndex = random.nextInt(barrelIds.size());
            int randomId = barrelIds.get(randomIndex);
            RMIIndexStorageBarrel selectedBarrel = barrels.get(randomId);

            // Testar se a barrel está funcionando
            try {
                // Teste simples: chamar um método que não altera estado
                selectedBarrel.gatewaypong("Gateway");
                System.out.println(getTimestamp() + " : 🎲 Barrel aleatória selecionada e testada: " + randomId);
                return selectedBarrel;
            } catch (RemoteException e) {
                // A barrel não está respondendo, remover do registro
                System.out.println(
                        getTimestamp() + " : ⚠️ Barrel " + randomId + " não está respondendo. Removendo do registro.");
                barrels.remove(randomId);
                barrelIds.remove(randomIndex);

                // Registrar o erro para diagnóstico
                System.err.println(getTimestamp() + " : ❌ Erro ao testar barrel " + randomId + ": " + e.getMessage());
            }
        }

        // Se chegou aqui, não encontrou nenhuma barrel funcional
        System.out.println(getTimestamp() + " : ❌ Não foi possível encontrar uma barrel funcional");
        return null;
    }

    // Função para o cliente colocar uma URL na URLQueue
    public void clientIndexUrl(String url) throws InterruptedException, RemoteException {
        if (urlQueue.contains(url) || isqueued.contains(url)) {
            System.out.println(getTimestamp() + " : URL (" + url + ") was already queued or indexed.");
            return;
        }
        urlQueue.offer(url);
        System.out.println(getTimestamp() + " : URL " + url + " added to the queue.");
        isqueued.add(url);
        urlSearchCount = 0;
        return;
    }

    public List<String> returnPagesbyWords(String words) throws RemoteException {
        RMIIndexStorageBarrel barrel = getRandomBarrel();
        if (barrel == null) {
            System.out.println(getTimestamp() + " : ⚠️ Não há barrels disponíveis para pesquisa de palavras");
            return new ArrayList<>();
        }

        Set<String> wordsSet = new HashSet<>();
        String[] wordsArray = words.split(" ");
        for (String word : wordsArray) {
            wordsSet.add(word);
        }
        return barrel.searchPagesByWords(wordsSet);
    }

    public List<String> returnLinkedUrls(String url) throws RemoteException {
        RMIIndexStorageBarrel barrel = getRandomBarrel();
        if (barrel == null) {
            System.out.println(getTimestamp() + " : ⚠️ Não há barrels disponíveis para consulta de URLs vinculadas");
            return new ArrayList<>();
        }

        return barrel.getIncomingLinksForUrl(url);
    }

    // Função pro crawler colocar URLs encontradas na URLQueue
    public synchronized void queueUrls(String url) throws InterruptedException {
        if (urlSearchCount > urlSearchDepth) {
            // System.out.println("URLSearch depth has reached the limit!");
            return;
        }
        if (urlQueue.contains(url) || isqueued.contains(url)) {
            System.out.println(getTimestamp() + " : URL (" + url + ") was already queued or indexed.");
            return;
        }
        urlQueue.offer(url);
        System.out.println(getTimestamp() + " : URL " + url + " added to the queue.");
        isqueued.add(url);
        urlSearchCount++;
    }

    public String popqueue() throws InterruptedException {
        return urlQueue.take();
    }

    public void registerIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException {
        barrels.put(id, barrel);
        System.out.println(getTimestamp() + " : 📝 Barrel" + id + " registrada!");
        barrels.get(id).gatewaypong("Gateway");
        barrels.get(id).registerallIBS(barrels, id, barrel);
        // Não precisamos mais notificar os downloaders sobre novas barrels
        // Eles obterão barrels através do método getRandomBarrel()
    }

    public boolean unsubscribeIBS(int id) throws RemoteException {
        if (barrels.containsKey(id)) {
            barrels.remove(id);
            System.out.println(getTimestamp() + " : Barrel " + id + " removida do registro");
            return true;
        } else {
            System.out.println(getTimestamp() + " : Barrel " + id + " não encontrada no registro");
            return false;
        }
    }

    public Map<Integer, RMIIndexStorageBarrel> getBarrels() throws RemoteException {
        return barrels;
    }

    public static void main(String args[]) {
        int gatewayClientPort, gatewayDownloaderPort, gatewayIBSDownloaderPort;
        String gatewayClientN, gatewayDownloaderN, gatewayIBSDownloaderN;

        try {
            RMIGateway gateway = new RMIGateway();
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[0]);
            prop.load(input);

            gatewayClientPort = Integer.parseInt(prop.getProperty("gatewayClientPort"));
            gatewayClientN = prop.getProperty("gatewayClientN");

            gatewayDownloaderPort = Integer.parseInt(prop.getProperty("gatewayDownloaderPort"));
            gatewayDownloaderN = prop.getProperty("gatewayDownloaderN");

            gatewayIBSDownloaderPort = Integer.parseInt(prop.getProperty("gatewayIBSDownloaderPort"));
            gatewayIBSDownloaderN = prop.getProperty("gatewayIBSDownloaderN");

            gateway.urlSearchDepth = Integer.parseInt(prop.getProperty("urlSearchDepth"));

            try {
                java.rmi.registry.LocateRegistry.createRegistry(gatewayClientPort).rebind(gatewayClientN, gateway);
                System.out.println("RMI Registry started on port " + gatewayClientPort);
                System.out.println("Gateway registered as '" + gatewayClientN + "' on port " + gatewayClientPort);

                java.rmi.registry.LocateRegistry.createRegistry(gatewayDownloaderPort).rebind(gatewayDownloaderN,
                        gateway);
                System.out
                        .println("Gateway registered as '" + gatewayDownloaderN + "' on port " + gatewayDownloaderPort);

                java.rmi.registry.LocateRegistry.createRegistry(gatewayIBSDownloaderPort).rebind(gatewayIBSDownloaderN,
                        gateway);
                System.out.println(
                        "Gateway registered as '" + gatewayIBSDownloaderN + "' on port " + gatewayIBSDownloaderPort);

            } catch (Exception e) {
                System.out.println("Error registering gateway: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("Error initializing gateway: " + e.getMessage());
            e.printStackTrace();
        }
    }
}