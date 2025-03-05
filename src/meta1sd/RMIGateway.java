package meta1sd;

import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

public class RMIGateway {
    private LinkedBlockingQueue<String> urlQueue;
    private int urlSearchCount, urlSearchDepth;

    public RMIGateway() throws RemoteException {
        urlQueue = new LinkedBlockingQueue<>();
    }

    public synchronized void clientIndexUrl(String url) throws InterruptedException {
        if (urlQueue.contains(url)) {
            System.out.println(LocalDateTime.now() + " : URL (" + url + ") was already queued or indexed.");
            return;
        }

        urlQueue.put(url);
        System.out.println(LocalDateTime.now() + " : URL " + url + " added to the queue.");
        urlSearchCount = 0;
        return;
    }

    public synchronized void queueUrls(String url) throws InterruptedException {
        if (urlSearchCount > urlSearchDepth) {
            return;
        }
        if (urlQueue.contains(url)) {
            System.out.println(LocalDateTime.now() + " : URL (" + url + ") was already queued or indexed.");
            return;
        }
        urlQueue.put(url);
        System.out.println(LocalDateTime.now() + " : URL " + url + " added to the queue.");
        urlSearchCount++;
    }

    public static void main(String args[]) {
        int gatewayClientPort;
        String gatewayClientN;

        try {
            RMIGateway gateway = new RMIGateway();
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[0]);
            prop.load(input);

            gatewayClientPort = Integer.parseInt(prop.getProperty("gatewayClientPort"));
            gatewayClientN = prop.getProperty("gatewayClientsN");

            gateway.urlSearchDepth = Integer.parseInt(prop.getProperty("urlSearchDepth"));

        } catch (Exception e) {

        }
    }
}
