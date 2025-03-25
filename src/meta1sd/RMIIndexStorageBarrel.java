package meta1sd;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RMIIndexStorageBarrel - Interface para operações remotas da
 * IndexStorageBarrel.
 * Define métodos para armazenamento, busca e sincronização de dados entre
 * barrels.
 */
public interface RMIIndexStorageBarrel extends Remote {

    /**
     * Verifica se a barrel está ativa.
     * 
     * @param provider O nome do provedor que está verificando a atividade.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public void gatewaypong(String provider) throws RemoteException;

    /**
     * Armazena dados de um site, atualizando os índices.
     * 
     * @param siteData Os dados do site a serem armazenados.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public void storeSiteData(SiteData siteData) throws RemoteException;

    /**
     * Pesquisa páginas que contêm todas as palavras especificadas.
     * 
     * @param words Conjunto de palavras a serem pesquisadas.
     * @return Lista de URLs que correspondem às palavras.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public List<String> searchPagesByWords(Set<String> words) throws RemoteException;

    /**
     * Registra esta barrel em outras barrels e vice-versa.
     * 
     * @param barrells Mapa de barrels existentes.
     * @param myid     Identificador da barrel atual.
     * @param mybarrel Referência para a barrel atual.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public void registerallIBS(Map<Integer, RMIIndexStorageBarrel> barrells, int myid,
            RMIIndexStorageBarrel mybarrel) throws RemoteException;

    /**
     * Registra uma barrel na lista local de barrels.
     * 
     * @param id     Identificador da barrel a ser registrada.
     * @param barrel A barrel a ser registrada.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public void registeroneIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException;

    /**
     * Retorna as URLs que apontam para uma URL específica.
     * 
     * @param url A URL para a qual as URLs vinculadas devem ser retornadas.
     * @return Lista de URLs que apontam para a URL especificada.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public List<String> getIncomingLinksForUrl(String url) throws RemoteException;

    /**
     * Retorna o índice invertido (palavra -> conjunto de URLs).
     * 
     * @return Mapa contendo o índice invertido.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public Map<String, Set<String>> getInvertedIndex() throws RemoteException;

    /**
     * Retorna o mapa de links de entrada (URL -> lista de URLs que apontam para
     * ela).
     * 
     * @return Mapa contendo os links de entrada.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public Map<String, List<String>> getIncomingLinksMap() throws RemoteException;

    /**
     * Retorna o mapa de referências de URL (URL -> contagem de referências).
     * 
     * @return Mapa contendo as referências de URL.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public Map<String, Integer> getUrlReferences() throws RemoteException;

    /**
     * Retorna o mapa de textos de URL (URL -> texto associado).
     * 
     * @return Mapa contendo os textos de URL.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public Map<String, String> getUrlTexts() throws RemoteException;

    /**
     * Retorna páginas ordenadas por número de links apontando para elas.
     * 
     * @return Lista de entradas de páginas ordenadas por contagem de links.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public List<Map.Entry<String, Integer>> getPagesOrderedByIncomingLinks() throws RemoteException;

    /**
     * Retorna o conjunto de dados do site armazenados.
     * 
     * @return Conjunto de dados do site.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public Set<SiteData> getSiteDataSet() throws RemoteException;
}