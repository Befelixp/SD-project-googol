package meta1sd;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class RMIGateway extends UnicastRemoteObject
        implements RMIGatewayClientInterface, RMIGatewayDownloaderInterface, RMIGatewayIBSDownloader {
    private LinkedBlockingQueue<String> urlQueue;
    private int urlSearchCount, urlSearchDepth;
    private HashSet<String> isqueued;

    private Map<Integer, RMIIndexStorageBarrel> barrels = new HashMap<>();

    public RMIGateway() throws RemoteException {
        urlQueue = new LinkedBlockingQueue<>();
        isqueued = new HashSet<>();
    }

    // Função para o cliente colocar uma URL na URLQueue
    public void clientIndexUrl(String url) throws InterruptedException, RemoteException {
        if (urlQueue.contains(url) || isqueued.contains(url)) {
            System.out.println(LocalDateTime.now() + " : URL (" + url + ") was already queued or indexed.");
            return;
        }
        urlQueue.offer(url);
        System.out.println(LocalDateTime.now() + " : URL " + url + " added to the queue.");
        isqueued.add(url);
        urlSearchCount = 0;
        return;
    }

    // Função pro crawler colocar URLs encontradas na URLQueue
    public synchronized void queueUrls(String url) throws InterruptedException {
        if (urlSearchCount > urlSearchDepth) {
            // System.out.println("URLSearch depth has reached the limit!");
            return;
        }
        if (urlQueue.contains(url) || isqueued.contains(url)) {
            System.out.println(LocalDateTime.now() + " : URL (" + url + ") was already queued or indexed.");
            return;
        }
        urlQueue.offer(url);
        System.out.println(LocalDateTime.now() + " : URL " + url + " added to the queue.");
        isqueued.add(url);
        urlSearchCount++;
    }

    public String popqueue() throws InterruptedException {
        return urlQueue.take();
    }

    public void registerIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException {
        barrels.put(id, barrel);
        System.out.println("Ping");
        barrels.get(id).gatewaypong();
    }

    public static void main(String args[]) {
        int gatewayClientPort, gatewayDownloaderPort, gatewayIBSDownloaderPort;
        String gatewayClientN, gatewayDownloaderN, gatewayIBSDownloaderN, gatewayClientRegistry,
                gatewayDownloaderRegistry, gatewayIBSDownloaderRegistry;

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
