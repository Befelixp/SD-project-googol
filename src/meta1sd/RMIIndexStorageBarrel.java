package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface para operações remotas da IndexStorageBarrel
 * Define métodos para armazenamento, busca e sincronização de dados entre
 * barrels
 */
public interface RMIIndexStorageBarrel extends Remote {

    /**
     * Verifica se a barrel está ativa
     */
    public void gatewaypong(String provider) throws RemoteException;

    /**
     * Armazena dados de um site, atualizando os índices
     */
    public void storeSiteData(SiteData siteData) throws RemoteException;

    /**
     * Pesquisa páginas que contêm todas as palavras especificadas
     */
    public List<String> searchPagesByWords(Set<String> words) throws RemoteException;

    /**
     * Registra esta barrel em outras barrels e vice-versa
     */
    public void registerallIBS(Map<Integer, RMIIndexStorageBarrel> barrells, int myid,
            RMIIndexStorageBarrel mybarrel) throws RemoteException;

    /**
     * Registra uma barrel na lista local de barrels
     */
    public void registeroneIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException;

    /**
     * Retorna as URLs que apontam para uma URL específica
     */
    public List<String> getIncomingLinksForUrl(String url) throws RemoteException;

    /**
     * Retorna o índice invertido (palavra -> conjunto de URLs)
     */
    public Map<String, Set<String>> getInvertedIndex() throws RemoteException;

    /**
     * Retorna o mapa de links de entrada (URL -> lista de URLs que apontam para
     * ela)
     */
    public Map<String, List<String>> getIncomingLinksMap() throws RemoteException;

    /**
     * Retorna o mapa de referências de URL (URL -> contagem de referências)
     */
    public Map<String, Integer> getUrlReferences() throws RemoteException;

    /**
     * Retorna o mapa de textos de URL (URL -> texto associado)
     */
    public Map<String, String> getUrlTexts() throws RemoteException;

    /**
     * Retorna páginas ordenadas por número de links apontando para elas
     */
    public List<Map.Entry<String, Integer>> getPagesOrderedByIncomingLinks() throws RemoteException;
}