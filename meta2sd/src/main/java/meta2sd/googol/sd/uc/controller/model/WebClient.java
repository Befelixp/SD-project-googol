package meta2sd.googol.sd.uc.controller.model;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import meta1sd.RMIClient;
import meta1sd.RMIGatewayClientInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The WebClient class represents a client in the distributed system.
 * It extends UnicastRemoteObject to support RMI communication.
 */
@Component
public class WebClient extends UnicastRemoteObject {

    private int id = 1; // Default ID
    private RMIGatewayClientInterface gateway;
    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY_MS = 2000;

    @Value("${characterLimit}")
    private int characterLimit;

    @Value("${registryName}")
    private String registryName;

    private static final Logger logger = LoggerFactory.getLogger(WebClient.class);

    /**
     * Default constructor for Spring bean initialization.
     * 
     * @throws RemoteException if an RMI error occurs
     */
    public WebClient() throws RemoteException {
        super();
    }

    /**
     * Initializes the WebClient after properties are injected.
     * This method is called after all properties are set.
     */
    @PostConstruct
    public void init() {
        if (registryName == null || registryName.trim().isEmpty()) {
            throw new IllegalStateException("registryName property is not set in application.properties");
        }
        try {
            connectToGateway();
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to connect to gateway", e);
        }
    }

    /**
     * Attempts to connect to the gateway with retry logic.
     * 
     * @throws RemoteException if an RMI error occurs
     */
    private void connectToGateway() throws RemoteException {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                System.out.println("Attempting to connect to gateway at: " + registryName);
                gateway = (RMIGatewayClientInterface) Naming.lookup(registryName);
                System.out.println("WebClient started: " + id);
                return;
            } catch (MalformedURLException | NotBoundException e) {
                retries++;
                if (retries == MAX_RETRIES) {
                    System.err.println("Failed to connect to gateway after " + MAX_RETRIES + " attempts");
                    e.printStackTrace();
                    return;
                }
                System.out
                        .println("Connection attempt " + retries + " failed. Retrying in " + RETRY_DELAY_MS + "ms...");
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Returns the ID of the client.
     * 
     * @return the ID of the client
     */
    public int getId() {
        return id;
    }

    /**
     * Adds a URL to the Queue in the gateway.
     * 
     * @param url the URL to add
     * @return true if the URL was added successfully, false otherwise
     */
    public boolean addURL(String url) {
        if (gateway == null) {
            System.err.println("Gateway not connected");
            return false;
        }
        try {
            gateway.clientIndexUrl(url);
            return true;
        } catch (Exception e) {
            System.err.println("Error adding URL: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Asks the gateway to search for the terms.
     * 
     * @param terms the terms to search for
     * @return the search results
     */
    public List<String> getPagesbyTerms(String terms) {
        try {
            return gateway.returnPagesbyWords(terms);
        } catch (RemoteException e) {
            logger.error("Error getting pages by terms: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Asks the gateway to consult the URL.
     * 
     * @param urlConsult the URL to consult
     * @return the URL data
     */
    public List<String> returnLinkedUrls(String urlConsult) {
        if (gateway == null) {
            System.err.println("Gateway not connected");
            return null;
        }
        try {
            List<String> result = gateway.returnLinkedUrls(urlConsult);
            return result;
        } catch (Exception e) {
            System.err.println("Error consulting URL: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<String> getPagesbyUrl(String url) {
        try {
            return gateway.returnLinkedUrls(url);
        } catch (RemoteException e) {
            logger.error("Error getting pages by URL: {}", e.getMessage());
            return null;
        }
    }
}
