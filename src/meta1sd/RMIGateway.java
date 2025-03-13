package meta1sd;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

public class RMIGateway extends UnicastRemoteObject
        implements RMIGatewayClientInterface, RMIGatewayDownloaderInterface {
    private LinkedBlockingQueue<String> urlQueue;
    private int urlSearchCount, urlSearchDepth;

    public RMIGateway() throws RemoteException {
        urlQueue = new LinkedBlockingQueue<>();
    }

    // Função para o cliente colocar uma URL na URLQueue
    public void clientIndexUrl(String url) throws InterruptedException, RemoteException {
        if (urlQueue.contains(url)) {
            System.out.println(LocalDateTime.now() + " : URL (" + url + ") was already queued or indexed.");
            return;
        }
        urlQueue.offer(url);
        System.out.println(LocalDateTime.now() + " : URL " + url + " added to the queue.");
        urlSearchCount = 0;
        return;
    }

    // Função pro crawler colocar URLs encontradas na URLQueue
    public synchronized void queueUrls(String url) throws InterruptedException {
        if (urlSearchCount > urlSearchDepth) {
            System.out.println("URLSearch depth has reached the limit!");
            return;
        }
        if (urlQueue.contains(url)) {
            System.out.println(LocalDateTime.now() + " : URL (" + url + ") was already queued or indexed.");
            return;
        }
        urlQueue.offer(url);
        System.out.println(LocalDateTime.now() + " : URL " + url + " added to the queue.");
        urlSearchCount++;
    }

    public String popqueue() throws InterruptedException {
        return urlQueue.take();
    }

    public static void main(String args[]) {
        int gatewayClientPort, gatewayDownloaderPort;
        String gatewayClientN, gatewayDownloaderN;

        try {
            RMIGateway gateway = new RMIGateway();
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[0]);
            prop.load(input);

            gatewayClientPort = Integer.parseInt(prop.getProperty("gatewayClientPort"));
            gatewayClientN = prop.getProperty("gatewayClientN");

            gatewayDownloaderPort = Integer.parseInt(prop.getProperty("gatewayDownloaderPort"));
            gatewayDownloaderN = prop.getProperty("gatewayDownloaderN");

            gateway.urlSearchDepth = Integer.parseInt(prop.getProperty("urlSearchDepth"));

            try {
                java.rmi.registry.LocateRegistry.createRegistry(gatewayClientPort);
                System.out.println("RMI Registry started on port " + gatewayClientPort);

                // Registrar o objeto remoto no registro RMI
                java.rmi.Naming.rebind("rmi://localhost:" + gatewayClientPort + "/" + gatewayClientN, gateway);
                System.out.println("Gateway registered as '" + gatewayClientN + "' on port " + gatewayClientPort);

                java.rmi.registry.LocateRegistry.createRegistry(gatewayDownloaderPort);
                System.out.println("RMI Registry started on port " + gatewayDownloaderPort);

                java.rmi.Naming.rebind("rmi://localhost:" + gatewayDownloaderPort + "/" + gatewayDownloaderN, gateway);
                System.out
                        .println("Gateway registered as '" + gatewayDownloaderN + "' on port " + gatewayDownloaderPort);

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
