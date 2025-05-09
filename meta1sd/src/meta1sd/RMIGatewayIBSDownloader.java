package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * RMIGatewayIBSDownloader - Interface remota para operações relacionadas ao
 * gerenciamento de barrels de armazenamento no gateway RMI.
 */
public interface RMIGatewayIBSDownloader extends Remote {

    /**
     * Registra uma barrel no gateway.
     * 
     * @param id     O ID da barrel a ser registrada.
     * @param barrel A barrel a ser registrada.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public void registerIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException;

    /**
     * Retorna um mapa de barrels registradas.
     * 
     * @return Mapa de barrels registradas, onde a chave é o ID da barrel e o
     *         valor é a referência da barrel.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public Map<Integer, RMIIndexStorageBarrel> getBarrels() throws RemoteException;

    /**
     * Obtém uma barrel aleatória registrada.
     * 
     * @return Uma barrel aleatória ou null se não houver barrels disponíveis.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public RMIIndexStorageBarrel getRandomBarrel() throws RemoteException;

    /**
     * Remove uma barrel do registro.
     * 
     * @param id O ID da barrel a ser removida.
     * @return true se a barrel foi removida com sucesso, false caso contrário.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public boolean unsubscribeIBS(int id) throws RemoteException;
}