package meta2sd.googol.sd.uc.controller.model;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import meta1sd.RMIClient;
import meta1sd.RMIGatewayClientInterface;

/**
 * The WebClient class represents a client in the distributed system.
 * It extends UnicastRemoteObject to support RMI communication.
 */
public class WebClient extends UnicastRemoteObject {

    private int id, characterLimit;
    private RMIGatewayClientInterface gateway;

    /**
     * Constructor for the WebClient class.
     * 
     * @param id             the ID of the client
     * @param registryName   the name of the RMI registry
     * @param characterLimit the character limit for the search results
     * @throws RemoteException       if an RMI error occurs
     * @throws MalformedURLException if the URL is invalid
     * @throws NotBoundException     if the RMI registry is not bound
     */
    public WebClient(int id, String registryName, int characterLimit)
            throws RemoteException, MalformedURLException, NotBoundException {
        this.id = id;
        this.characterLimit = characterLimit;

        gateway = (RMIGatewayClientInterface) Naming.lookup(registryName);
        System.out.println("WebClient started: " + id);
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
        try {
            gateway.clientIndexUrl(url);
            return true;
        } catch (Exception e) {
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
            List<String> result = gateway.returnPagesbyWords(terms);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
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
        try {
            List<String> result = gateway.returnLinkedUrls(urlConsult);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
