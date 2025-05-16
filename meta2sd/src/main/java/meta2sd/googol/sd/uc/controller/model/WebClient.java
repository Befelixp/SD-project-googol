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
import meta1sd.RMIGatewayClientInterface;
import meta1sd.SiteData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cliente web que se comunica com o gateway RMI para realizar operações de
 * busca e indexação.
 * Implementa a interface UnicastRemoteObject para suportar comunicação RMI.
 * 
 * @author Bernardo Pedro nº2021231014 e João Matos nº2021222748
 * @version 1.0
 */
@Component
public class WebClient extends UnicastRemoteObject {

    private int id = 1; // ID padrão do cliente
    private RMIGatewayClientInterface gateway;
    private static final int MAX_RETRIES = 5; // Número máximo de tentativas de conexão
    private static final int RETRY_DELAY_MS = 2000; // Delay entre tentativas em milissegundos

    @Value("${characterLimit}")
    private int characterLimit;

    @Value("${registryName}")
    private String registryName;

    private static final Logger logger = LoggerFactory.getLogger(WebClient.class);

    /**
     * Construtor padrão para inicialização do bean Spring.
     * 
     * @throws RemoteException se ocorrer um erro RMI
     */
    public WebClient() throws RemoteException {
        super();
    }

    /**
     * Inicializa o WebClient após a injeção das propriedades.
     * Este método é chamado após todas as propriedades serem definidas.
     */
    @PostConstruct
    public void init() {
        if (registryName == null || registryName.trim().isEmpty()) {
            logger.error("registryName property is not set in application.properties");
            return;
        }
        logger.info("Initializing WebClient with registry name: {}", registryName);
        try {
            connectToGateway();
        } catch (RemoteException e) {
            logger.error("Failed to connect to gateway: {}", e.getMessage());
        }
    }

    /**
     * Tenta conectar ao gateway com lógica de retry.
     * 
     * @throws RemoteException se ocorrer um erro RMI
     */
    private void connectToGateway() throws RemoteException {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                logger.info("Attempting to connect to gateway at: {}", registryName);
                gateway = (RMIGatewayClientInterface) Naming.lookup(registryName);
                logger.info("Successfully connected to gateway. WebClient started with ID: {}", id);
                return;
            } catch (MalformedURLException e) {
                logger.error("Invalid RMI URL format: {}", registryName);
                return;
            } catch (NotBoundException e) {
                retries++;
                if (retries == MAX_RETRIES) {
                    logger.error(
                            "Gateway not found in RMI registry after {} attempts. Make sure the gateway is running and registered with name 'Clients_Gateway'",
                            MAX_RETRIES);
                    return;
                }
                logger.info("Connection attempt {} failed. Gateway not found. Retrying in {}ms...", retries,
                        RETRY_DELAY_MS);
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } catch (RemoteException e) {
                retries++;
                if (retries == MAX_RETRIES) {
                    logger.error(
                            "Failed to connect to RMI registry after {} attempts. Make sure the RMI registry is running on port 1092",
                            MAX_RETRIES);
                    return;
                }
                logger.info("Connection attempt {} failed. RMI registry not available. Retrying in {}ms...", retries,
                        RETRY_DELAY_MS);
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
     * Retorna o ID do cliente.
     * 
     * @return ID do cliente
     */
    public int getId() {
        return id;
    }

    /**
     * Adiciona uma URL à fila de indexação no gateway.
     * 
     * @param url URL a ser indexada
     * @return true se a URL foi adicionada com sucesso, false caso contrário
     */
    public boolean addURL(String url) {
        if (gateway == null) {
            logger.error("Cannot add URL: Gateway not connected");
            return false;
        }
        try {
            logger.info("Adding URL to index queue: {}", url);
            gateway.clientIndexUrl(url);
            logger.info("URL successfully added to index queue: {}", url);
            return true;
        } catch (Exception e) {
            logger.error("Error adding URL to index queue: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Solicita ao gateway para buscar os termos.
     * 
     * @param terms termos a serem buscados
     * @return lista de SiteData com os resultados da busca
     */
    public List<SiteData> getPagesbyTerms(String terms) {
        if (gateway == null) {
            logger.error("Cannot search terms: Gateway not connected");
            return null;
        }
        try {
            logger.info("Searching for terms: {}", terms);
            List<SiteData> results = gateway.returnPagesbyWords(terms);
            logger.info("Found {} results for terms: {}", results != null ? results.size() : 0, terms);
            return results;
        } catch (RemoteException e) {
            logger.error("Error searching for terms: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Solicita ao gateway para consultar uma URL.
     * 
     * @param urlConsult URL a ser consultada
     * @return lista de URLs que linkam para a URL consultada
     */
    public List<String> returnLinkedUrls(String urlConsult) {
        if (gateway == null) {
            logger.error("Cannot search linked URLs: Gateway not connected");
            return null;
        }
        try {
            logger.info("Searching for URLs linking to: {}", urlConsult);
            List<String> results = gateway.returnLinkedUrls(urlConsult);
            logger.info("Found {} URLs linking to: {}", results != null ? results.size() : 0, urlConsult);
            return results;
        } catch (Exception e) {
            logger.error("Error searching for linked URLs: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Obtém páginas que linkam para uma URL específica.
     * 
     * @param url URL para buscar páginas que a referenciam
     * @return lista de URLs que linkam para a URL especificada
     */
    public List<String> getPagesbyUrl(String url) {
        if (gateway == null) {
            logger.error("Cannot search pages by URL: Gateway not connected");
            return null;
        }
        try {
            logger.info("Searching for pages linking to: {}", url);
            List<String> results = gateway.returnLinkedUrls(url);
            logger.info("Found {} pages linking to: {}", results != null ? results.size() : 0, url);
            return results;
        } catch (RemoteException e) {
            logger.error("Error searching for pages by URL: {}", e.getMessage());
            return null;
        }
    }
}
