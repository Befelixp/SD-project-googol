package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMIGatewayDownloaderInterface - Interface remota para operações de
 * gerenciamento de URLs no gateway RMI, permitindo enfileirar e desenfileirar
 * URLs.
 */
public interface RMIGatewayDownloaderInterface extends Remote {

    /**
     * Adiciona uma URL à fila de URLs.
     * 
     * @param url A URL a ser adicionada à fila.
     * @throws InterruptedException Se a operação for interrompida.
     * @throws RemoteException      Se ocorrer um erro de comunicação remota.
     */
    public void queueUrls(String url) throws InterruptedException, RemoteException;

    /**
     * Remove e retorna a próxima URL da fila.
     * 
     * @return A próxima URL da fila.
     * @throws InterruptedException Se a operação for interrompida.
     * @throws RemoteException      Se ocorrer um erro de comunicação remota.
     */
    public String popqueue() throws InterruptedException, RemoteException;
}