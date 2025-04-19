package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMIGatewayClientInterface - Interface remota para comunicação entre o cliente
 * e o gateway RMI, permitindo operações de indexação e busca.
 */
public interface RMIGatewayClientInterface extends Remote {

    /**
     * Adiciona uma URL à fila de indexação.
     * 
     * @param url A URL a ser indexada.
     * @throws InterruptedException Se a operação for interrompida.
     * @throws RemoteException      Se ocorrer um erro de comunicação remota.
     */
    public void clientIndexUrl(String url) throws InterruptedException, RemoteException;

    /**
     * Retorna uma lista de páginas que correspondem às palavras fornecidas.
     * 
     * @param words As palavras a serem pesquisadas.
     * @return Lista de URLs que correspondem às palavras.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public List<String> returnPagesbyWords(String words) throws RemoteException;

    /**
     * Retorna uma lista de URLs vinculadas a uma URL específica.
     * 
     * @param url A URL para a qual as URLs vinculadas devem ser retornadas.
     * @return Lista de URLs vinculadas.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public List<String> returnLinkedUrls(String url) throws RemoteException;
}
