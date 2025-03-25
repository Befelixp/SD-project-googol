package meta1sd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import com.google.gson.reflect.TypeToken;

/**
 * IndexStorageBarrel - Componente responsável por armazenar e indexar dados de
 * sites web.
 * Implementa a interface RMIIndexStorageBarrel para comunicação remota.
 */
public class IndexStorageBarrel extends UnicastRemoteObject implements RMIIndexStorageBarrel {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Identificador único da barrel
    private final int barrelId;

    // Locks para controle de concorrência
    private final ReadWriteLock indexLock = new ReentrantReadWriteLock();
    private final ReadWriteLock stateLock = new ReentrantReadWriteLock();

    // Referências para outras barrels no sistema - Thread-safe
    private final Map<Integer, RMIIndexStorageBarrel> barrels = new ConcurrentHashMap<>();

    // Estruturas para indexação e rastreamento - Thread-safe
    private final Map<String, Set<String>> invertedIndex = new ConcurrentHashMap<>(); // Palavras -> URLs
    private final Map<String, Integer> urlReferences = new ConcurrentHashMap<>(); // URL -> contagem de referências
    private final Map<String, String> urlTexts = new ConcurrentHashMap<>(); // URL -> Texto associado
    private final Map<String, List<String>> incomingLinks = new ConcurrentHashMap<>(); // URL -> Lista de URLs que
                                                                                       // apontam para ela

    // Conjunto de sites armazenados localmente - Sincronizado externamente
    private final Set<SiteData> siteDataSet = Collections.synchronizedSet(new HashSet<>());

    /**
     * Retorna o índice invertido (palavra -> conjunto de URLs).
     * 
     * @return Mapa contendo o índice invertido.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    @Override
    public Map<String, Set<String>> getInvertedIndex() throws RemoteException {
        indexLock.readLock().lock();
        try {
            // Retorna uma cópia para evitar modificações externas
            Map<String, Set<String>> result = new ConcurrentHashMap<>();
            for (Map.Entry<String, Set<String>> entry : invertedIndex.entrySet()) {
                result.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
            return result;
        } finally {
            indexLock.readLock().unlock();
        }
    }

    /**
     * Retorna o mapa de links de entrada (URL -> lista de URLs que apontam para
     * ela).
     * 
     * @return Mapa contendo os links de entrada.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    @Override
    public Map<String, List<String>> getIncomingLinksMap() throws RemoteException {
        indexLock.readLock().lock();
        try {
            // Retorna uma cópia para evitar modificações externas
            Map<String, List<String>> result = new ConcurrentHashMap<>();
            for (Map.Entry<String, List<String>> entry : incomingLinks.entrySet()) {
                result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            return result;
        } finally {
            indexLock.readLock().unlock();
        }
    }

    /**
     * Retorna o mapa de referências de URL (URL -> contagem de referências).
     * 
     * @return Mapa contendo as referências de URL.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    @Override
    public Map<String, Integer> getUrlReferences() throws RemoteException {
        indexLock.readLock().lock();
        try {
            return new ConcurrentHashMap<>(urlReferences);
        } finally {
            indexLock.readLock().unlock();
        }
    }

    /**
     * Retorna o mapa de textos de URL (URL -> texto associado).
     * 
     * @return Mapa contendo os textos de URL.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    @Override
    public Map<String, String> getUrlTexts() throws RemoteException {
        indexLock.readLock().lock();
        try {
            return new ConcurrentHashMap<>(urlTexts);
        } finally {
            indexLock.readLock().unlock();
        }
    }

    /**
     * Obtém o timestamp formatado para logs.
     * 
     * @return O timestamp formatado.
     */
    private String getTimestamp() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    /**
     * Construtor da barrel.
     * 
     * @param barrelId Identificador único da barrel.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public IndexStorageBarrel(int barrelId) throws RemoteException {
        this.barrelId = barrelId;
        System.out.println(getTimestamp() + " : 🚀 System " + barrelId + " is starting up");

        // Criar diretório de dados se não existir
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                System.out.println(getTimestamp() + " : 📁 Diretório de dados criado");
            }
        }

        // Carregar estado local primeiro
        carregarEstadoDeJSON("data/estado_barrel_" + barrelId + ".json");

        // A sincronização com outras barrels será feita após o registro no gateway
        // através do método syncWithExistingBarrels()
    }

    /**
     * Retorna o conjunto de dados do site armazenados.
     * 
     * @return Conjunto de dados do site.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    @Override
    public Set<SiteData> getSiteDataSet() throws RemoteException {
        synchronized (siteDataSet) {
            return new HashSet<>(siteDataSet);
        }
    }

    /**
     * Sincroniza com barrels existentes obtidas do gateway.
     * 
     * @param gateway Interface do gateway para obter as barrels registradas.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public synchronized void syncWithExistingBarrels(RMIGatewayIBSDownloader gateway) throws RemoteException {
        try {
            Map<Integer, RMIIndexStorageBarrel> existingBarrels = gateway.getBarrels();

            // Remover a si mesmo do mapa, caso esteja presente
            existingBarrels.remove(this.barrelId);

            if (existingBarrels.isEmpty()) {
                System.out.println(getTimestamp() + " : 📝 Não há outras barrels para sincronizar");
                return;
            }

            System.out.println(getTimestamp() + " : 🔄 Iniciando sincronização com barrels existentes...");
            System.out.println(getTimestamp() + " : 📊 Barrels disponíveis: " + existingBarrels.keySet());

            // Tentar sincronizar com a primeira barrel ativa disponível
            boolean syncSuccess = false;
            for (Map.Entry<Integer, RMIIndexStorageBarrel> entry : existingBarrels.entrySet()) {
                try {
                    int targetBarrelId = entry.getKey();
                    RMIIndexStorageBarrel existingBarrel = entry.getValue();

                    // Verificar se a barrel está ativa
                    System.out.println(
                            getTimestamp() + " : 🔍 Verificando se a barrel " + targetBarrelId + " está ativa...");
                    existingBarrel.gatewaypong("NewBarrel" + barrelId);

                    // Sincronizar estado
                    System.out.println(
                            getTimestamp() + " : 🔄 Iniciando sincronização com barrel " + targetBarrelId + "...");
                    syncFromExistingBarrel(existingBarrel);

                    System.out.println(getTimestamp() + " : ✅ Sincronizado com sucesso com a barrel " + targetBarrelId);
                    syncSuccess = true;
                    break;
                } catch (RemoteException e) {
                    System.out.println(getTimestamp() + " : ⚠️ Barrel " + entry.getKey()
                            + " não está respondendo, tentando próxima...");
                    continue;
                }
            }

            if (!syncSuccess && !existingBarrels.isEmpty()) {
                System.out.println(getTimestamp() + " : ⚠️ Não foi possível sincronizar com nenhuma barrel existente");
            }

            // Salvar o estado após a sincronização
            saveState("data/estado_barrel_" + barrelId + ".json");
            System.out.println(getTimestamp() + " : 💾 Estado salvo após sincronização.");

        } catch (Exception e) {
            System.err.println(getTimestamp() + " : ❌ Erro durante a tentativa de sincronização: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método otimizado para sincronizar dados de uma barrel existente.
     * Esta versão transfere os mapas inteiros de uma vez em vez de processar item
     * por item.
     * 
     * @param existingBarrel A barrel existente para sincronização.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public synchronized void syncFromExistingBarrel(RMIIndexStorageBarrel existingBarrel) throws RemoteException {
        System.out.println(getTimestamp() + " : 📥 Iniciando sincronização de dados completos...");

        try {
            long startTime = System.currentTimeMillis();
            int totalItemsSynced = 0;

            // 0. Primeiro sincroniza os SiteData originais para garantir que todos os
            // campos sejam preservados
            System.out.println(getTimestamp() + " : 🔄 Sincronizando SiteData originais...");
            Set<SiteData> existingSiteData = existingBarrel.getSiteDataSet();
            if (existingSiteData != null) {
                synchronized (siteDataSet) {
                    // Limpar dados existentes
                    siteDataSet.clear();

                    // Adicionar todos os SiteData da barrel existente
                    for (SiteData siteData : existingSiteData) {
                        siteData.setPropagated(true); // Marcar como já propagado
                        siteDataSet.add(siteData);
                        totalItemsSynced++;
                    }
                }
                System.out.println(
                        getTimestamp() + " : ✅ SiteData sincronizados - " + existingSiteData.size() + " itens");
            }

            // Usar write lock para atualização dos índices
            indexLock.writeLock().lock();
            try {
                // 1. Sincronizar o índice invertido (palavras -> URLs)
                System.out.println(getTimestamp() + " : 🔄 Sincronizando índice invertido...");
                Map<String, Set<String>> existingInvertedIndex = existingBarrel.getInvertedIndex();
                if (existingInvertedIndex != null) {
                    for (Map.Entry<String, Set<String>> entry : existingInvertedIndex.entrySet()) {
                        String word = entry.getKey();
                        Set<String> urls = entry.getValue();

                        // Criar ou unir conjuntos de URLs para cada palavra
                        Set<String> currentUrls = invertedIndex.computeIfAbsent(word,
                                k -> ConcurrentHashMap.newKeySet());
                        currentUrls.addAll(urls);
                    }
                    totalItemsSynced += existingInvertedIndex.size();
                    System.out.println(getTimestamp() + " : ✅ Índice invertido sincronizado - "
                            + existingInvertedIndex.size() + " palavras");
                }

                // 2. Sincronizar referências de URLs
                System.out.println(getTimestamp() + " : 🔄 Sincronizando referências de URLs...");
                Map<String, Integer> existingUrlReferences = existingBarrel.getUrlReferences();
                if (existingUrlReferences != null) {
                    existingUrlReferences.forEach(
                            (url, count) -> urlReferences.compute(url,
                                    (k, v) -> (v == null) ? count : Math.max(v, count)));
                    totalItemsSynced += existingUrlReferences.size();
                    System.out.println(getTimestamp() + " : ✅ Referências de URLs sincronizadas - "
                            + existingUrlReferences.size() + " URLs");
                }

                // 3. Sincronizar links de entrada
                System.out.println(getTimestamp() + " : 🔄 Sincronizando links de entrada...");
                Map<String, List<String>> existingIncomingLinks = existingBarrel.getIncomingLinksMap();
                if (existingIncomingLinks != null) {
                    for (Map.Entry<String, List<String>> entry : existingIncomingLinks.entrySet()) {
                        String url = entry.getKey();
                        List<String> links = entry.getValue();

                        // Criar ou atualizar lista de links para cada URL
                        incomingLinks.computeIfAbsent(url, k -> Collections.synchronizedList(new ArrayList<>()))
                                .addAll(links);

                        // Remover duplicados (eficiente mas em-lugar)
                        List<String> currentLinks = incomingLinks.get(url);
                        synchronized (currentLinks) {
                            Set<String> uniqueLinks = new HashSet<>(currentLinks);
                            currentLinks.clear();
                            currentLinks.addAll(uniqueLinks);
                        }
                    }
                    totalItemsSynced += existingIncomingLinks.size();
                    System.out.println(getTimestamp() + " : ✅ Links de entrada sincronizados - "
                            + existingIncomingLinks.size() + " URLs");
                }

                // 4. Sincronizar textos associados às URLs
                System.out.println(getTimestamp() + " : 🔄 Sincronizando textos de URLs...");
                Map<String, String> existingUrlTexts = existingBarrel.getUrlTexts();
                if (existingUrlTexts != null) {
                    // Apenas adiciona textos que ainda não existem localmente
                    existingUrlTexts.forEach((url, text) -> {
                        urlTexts.computeIfAbsent(url, k -> text);
                    });
                    totalItemsSynced += existingUrlTexts.size();
                    System.out.println(
                            getTimestamp() + " : ✅ Textos de URLs sincronizados - " + existingUrlTexts.size()
                                    + " URLs");
                }
            } finally {
                indexLock.writeLock().unlock();
            }

            // 5. Processar todos os SiteData para reconstruir os índices
            System.out.println(getTimestamp() + " : 🔄 Reconstruindo índices a partir dos SiteData...");
            synchronized (siteDataSet) {
                for (SiteData siteData : siteDataSet) {
                    processLocalUpdate(siteData);
                }
            }

            long endTime = System.currentTimeMillis();
            double seconds = (endTime - startTime) / 1000.0;

            System.out.println(getTimestamp() + " : ✅ Sincronização concluída em " + seconds + " segundos!");
            System.out.println(getTimestamp() + " : 📊 Total de itens sincronizados: " + totalItemsSynced);

            // Salvar o estado sincronizado no arquivo local
            saveState("data/estado_barrel_" + barrelId + ".json");

        } catch (Exception e) {
            System.err.println(getTimestamp() + " : ❌ Erro durante a sincronização: " + e.getMessage());
            e.printStackTrace();
            throw new RemoteException("Falha na sincronização", e);
        }
    }

    /**
     * Registra uma barrel na lista local de barrels.
     * 
     * @param id     Identificador da barrel a ser registrada.
     * @param barrel A barrel a ser registrada.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public synchronized void registeroneIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException {
        if (id != this.barrelId) {
            barrels.put(id, barrel);
            System.out.println(getTimestamp() + " : 📝 Guardando a barrel " + id);
        } else {
            System.out.println(getTimestamp() + " : ⚠️ Ignorando registro da própria barrel " + id);
        }
    }

    /**
     * Registra esta barrel em todas as outras barrels do sistema.
     * 
     * @param barrells Mapa de barrels existentes.
     * @param myid     Identificador da barrel atual.
     * @param mybarrel Referência para a barrel atual.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public synchronized void registerallIBS(Map<Integer, RMIIndexStorageBarrel> barrells, int myid,
            RMIIndexStorageBarrel mybarrel) throws RemoteException {
        System.out.println(getTimestamp() + " : 🔄 Registrando em outras barrels...");

        if (barrells.isEmpty()) {
            System.out.println(getTimestamp() + " : ℹ️ Não há outras barrels para registrar");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        // Copiar as entradas para evitar problemas de concorrência
        List<Map.Entry<Integer, RMIIndexStorageBarrel>> entries = new ArrayList<>(barrells.entrySet());

        for (Map.Entry<Integer, RMIIndexStorageBarrel> entry : entries) {
            int barid = entry.getKey();
            RMIIndexStorageBarrel barr = entry.getValue();

            try {
                if (barid != this.barrelId) {
                    this.registeroneIBS(barid, barr);
                    barr.registeroneIBS(myid, mybarrel);
                    barr.gatewaypong("Barrel" + myid);
                    System.out.println(getTimestamp() + " : ✅ Registrada na barrel " + barid);
                    successCount++;
                }
            } catch (RemoteException e) {
                System.err
                        .println(getTimestamp() + " : ❌ Falha ao registrar na barrel " + barid + ": " + e.getMessage());
                failCount++;
            }
        }

        System.out.println(
                getTimestamp() + " : 📊 Registro concluído - Sucesso: " + successCount + ", Falhas: " + failCount);
    }

    /**
     * Método usado para verificar se a barrel está ativa.
     * 
     * @param provider Nome do provedor que está verificando a atividade.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public void gatewaypong(String provider) throws RemoteException {
        System.out.println(getTimestamp() + " : 🔔 " + provider + ":Pong");
    }

    /**
     * Armazena dados de um site, atualizando os índices apropriados.
     * 
     * @param siteData Dados do site a serem armazenados.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    @Override
    public synchronized void storeSiteData(SiteData siteData) throws RemoteException {
        if (siteData == null || siteData.url == null || siteData.url.isEmpty()) {
            System.err.println(getTimestamp() + " : ⚠️ Tentativa de armazenar SiteData inválido");
            return;
        }

        // Se já foi propagado, apenas processa localmente
        if (siteData.isPropagated()) {
            processLocalUpdate(siteData);
            return;
        }

        // Processa localmente e propaga para outras barrels
        processLocalUpdate(siteData);

        // Marca como propagado antes de enviar para outras barrels
        siteData.setPropagated(true);
        propagateUpdate(siteData);
    }

    /**
     * Processa atualização local dos dados de um site.
     * 
     * @param siteData Dados do site a serem processados.
     */
    private void processLocalUpdate(SiteData siteData) {
        if (siteData == null || siteData.url == null || siteData.url.isEmpty()) {
            System.err.println(getTimestamp() + " : ⚠️ SiteData inválido para processamento local");
            return;
        }

        System.out.println(getTimestamp() + " : 📝 Processando atualização local para URL: " + siteData.url);

        // Adquirir write lock para atualizar os índices
        indexLock.writeLock().lock();
        try {
            // 1. Armazenar metadados básicos
            System.out.println(getTimestamp() + " : 🔍 Processando metadados para: " + siteData.url);

            // Armazenar texto da página se disponível
            if (siteData.text != null && !siteData.text.isEmpty()) {
                urlTexts.put(siteData.url, siteData.text);
                System.out.println(getTimestamp() + " : 🧾 Texto armazenado (" + siteData.text.length() + " chars)");
            }

            // 2. Processar tokens (palavras-chave)
            if (siteData.tokens != null && !siteData.tokens.isEmpty()) {
                System.out.println(getTimestamp() + " : 🔠 Indexando tokens...");
                indexTokens(siteData.tokens, siteData.url);
            } else {
                System.out.println(getTimestamp() + " : ℹ️ Nenhum token para indexar");
            }

            // 3. Processar links
            if (siteData.links != null && !siteData.links.isEmpty()) {
                System.out.println(getTimestamp() + " : 🔗 Processando links...");
                String[] links = siteData.links.split("\\s+");
                int newLinks = 0;

                for (String link : links) {
                    if (link.isEmpty())
                        continue;

                    // Atualizar contagem de referências
                    int newCount = urlReferences.compute(link, (k, v) -> (v == null) ? 1 : v + 1);
                    if (newCount == 1)
                        newLinks++;

                    // Atualizar links de entrada
                    List<String> incomingLinksList = incomingLinks.computeIfAbsent(link,
                            k -> Collections.synchronizedList(new ArrayList<>()));

                    synchronized (incomingLinksList) {
                        if (!incomingLinksList.contains(siteData.url)) {
                            incomingLinksList.add(siteData.url);
                        }
                    }
                }
                System.out.println(getTimestamp() + " : ➕ " + newLinks + " novos links de " + links.length + " totais");
            } else {
                System.out.println(getTimestamp() + " : ℹ️ Nenhum link para processar");
            }

            // 4. Atualizar conjunto principal de sites
            synchronized (siteDataSet) {
                // Remover versão anterior se existir
                boolean existed = siteDataSet.removeIf(site -> site.url.equals(siteData.url));
                siteDataSet.add(siteData);
                System.out.println(
                        getTimestamp() + " : " + (existed ? "🔄 Atualizado" : "🆕 Novo") + " SiteData adicionado");
            }

        } finally {
            indexLock.writeLock().unlock();
        }

        // 5. Salvar estado (com lock separado para evitar deadlocks)
        stateLock.writeLock().lock();
        try {
            System.out.println(getTimestamp() + " : 💾 Salvando estado...");
            saveState("data/estado_barrel_" + barrelId + ".json");
        } finally {
            stateLock.writeLock().unlock();
        }

        System.out.println(getTimestamp() + " : ✅ Atualização concluída para: " + siteData.url);
    }

    /**
     * Propaga atualização de dados para outras barrels.
     * 
     * @param siteData Dados do site a serem propagados.
     */
    private void propagateUpdate(SiteData siteData) {
        Map<Integer, RMIIndexStorageBarrel> barrelsSnapshot = new HashMap<>(barrels);

        if (barrelsSnapshot.isEmpty()) {
            System.out.println(getTimestamp() + " : ℹ️ Não há outras barrels para propagar a atualização");
            return;
        }

        System.out.println(getTimestamp() + " : 🔄 Propagando atualização para outras barrels...");
        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<Integer, RMIIndexStorageBarrel> entry : barrelsSnapshot.entrySet()) {
            int targetBarrelId = entry.getKey();
            RMIIndexStorageBarrel targetBarrel = entry.getValue();

            try {
                targetBarrel.storeSiteData(siteData);
                System.out.println(
                        getTimestamp() + " : ✅ Atualização propagada com sucesso para barrel " + targetBarrelId);
                successCount++;
            } catch (RemoteException e) {
                System.err.println(getTimestamp() + " : ❌ Falha ao propagar atualização para barrel " + targetBarrelId
                        + ": " + e.getMessage());
                failCount++;

                // Remover barrel inativa do mapa
                try {
                    targetBarrel.gatewaypong("Barrel" + targetBarrelId);
                } catch (RemoteException re) {
                    System.err.println(
                            getTimestamp() + " : ❌ Barrel " + targetBarrelId + " não responde. Removendo do registro.");
                    barrels.remove(targetBarrelId);
                }
            }
        }

        System.out.println(
                getTimestamp() + " : 📊 Propagação concluída - Sucesso: " + successCount + ", Falhas: " + failCount);
    }

    /**
     * Indexa tokens (palavras) de uma página.
     * 
     * @param tokens Tokens a serem indexados.
     * @param url    URL associada aos tokens.
     */
    private void indexTokens(String tokens, String url) {
        if (tokens == null || tokens.isEmpty() || url == null || url.isEmpty()) {
            return;
        }

        String[] tokenArray = tokens.split("\\s+");
        int tokenCount = 0;

        for (String token : tokenArray) {
            // Normaliza tokens: converte para minúsculas e remove caracteres não
            // alfanuméricos
            token = token.toLowerCase().replaceAll("[^a-z0-9]", "");
            if (token.isEmpty() || token.length() < 2) {
                continue; // Ignora tokens muito curtos ou vazios
            }

            // Adiciona URL ao conjunto para este token
            Set<String> urlSet = invertedIndex.computeIfAbsent(token, k -> ConcurrentHashMap.newKeySet());
            urlSet.add(url);
            tokenCount++;
        }

        if (tokenCount > 0) {
            System.out.println(getTimestamp() + " : 📊 Indexados " + tokenCount + " tokens para URL: " + url);
        }
    }

    /**
     * Pesquisa páginas que contêm todas as palavras especificadas.
     * 
     * @param words Conjunto de palavras a serem pesquisadas.
     * @return Lista de URLs que contêm todas as palavras especificadas.
     */
    public List<String> searchPagesByWords(Set<String> words) {
        if (words == null || words.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        Map<String, Integer> pageMatchCount = new HashMap<>();

        // Adquirir read lock para leitura dos índices
        indexLock.readLock().lock();
        try {
            for (String word : words) {
                word = word.toLowerCase().trim();
                Set<String> pages = invertedIndex.get(word);
                if (pages != null) {
                    for (String page : pages) {
                        pageMatchCount.put(page, pageMatchCount.getOrDefault(page, 0) + 1);
                    }
                }
            }

            // Filtrar apenas as páginas que contêm todas as palavras
            for (Map.Entry<String, Integer> entry : pageMatchCount.entrySet()) {
                if (entry.getValue() == words.size()) {
                    result.add(entry.getKey());
                }
            }
        } finally {
            indexLock.readLock().unlock();
        }

        System.out.println(
                getTimestamp() + " : 🔍 Pesquisa concluída - Palavras: " + words + ", Resultados: " + result.size());
        return result;
    }

    /**
     * Retorna a contagem de referências para uma URL.
     * 
     * @param url URL para a qual a contagem de referências deve ser retornada.
     * @return Contagem de referências para a URL especificada.
     */
    public int getUrlReferenceCount(String url) {
        indexLock.readLock().lock();
        try {
            return urlReferences.getOrDefault(url, 0);
        } finally {
            indexLock.readLock().unlock();
        }
    }

    /**
     * Retorna páginas ordenadas por número de links apontando para elas.
     * 
     * @return Lista de entradas de páginas ordenadas por contagem de links.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public List<Map.Entry<String, Integer>> getPagesOrderedByIncomingLinks() throws RemoteException {
        indexLock.readLock().lock();
        try {
            List<Map.Entry<String, Integer>> sortedPages = new ArrayList<>(urlReferences.entrySet());
            sortedPages.sort((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue()));
            return sortedPages;
        } finally {
            indexLock.readLock().unlock();
        }
    }

    /**
     * Retorna páginas que apontam para uma URL específica.
     * 
     * @param url URL para a qual as páginas que apontam devem ser retornadas.
     * @return Lista de URLs que apontam para a URL especificada.
     */
    public List<String> getPagesLinkingTo(String url) {
        indexLock.readLock().lock();
        try {
            List<String> result = incomingLinks.getOrDefault(url, new ArrayList<>());
            return new ArrayList<>(result); // Retorna uma cópia da lista
        } finally {
            indexLock.readLock().unlock();
        }
    }

    /**
     * Salva o estado atual da barrel em um arquivo JSON.
     * 
     * @param caminhoArquivo Caminho do arquivo onde o estado deve ser salvo.
     */
    public void saveState(String caminhoArquivo) {
        try {
            File file = new File(caminhoArquivo);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            Set<SiteData> siteDataCopy;
            synchronized (siteDataSet) {
                siteDataCopy = new HashSet<>(siteDataSet);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(siteDataCopy);

            try (FileWriter writer = new FileWriter(caminhoArquivo)) {
                writer.write(json);
            }

            System.out.println(getTimestamp() + " : 💾 Estado salvo com sucesso no ficheiro JSON: " + caminhoArquivo);

        } catch (Exception e) {
            System.err.println(getTimestamp() + " : ❌ Erro ao salvar estado no ficheiro JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carrega o estado da barrel a partir de um arquivo JSON.
     * 
     * @param caminhoArquivo Caminho do arquivo de onde o estado deve ser carregado.
     */
    public void carregarEstadoDeJSON(String caminhoArquivo) {
        File file = new File(caminhoArquivo);
        if (!file.exists()) {
            System.out.println(getTimestamp() + " : ℹ️ Nenhum estado salvo encontrado para esta barrel.");
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            System.out.println(getTimestamp() + " : 📂 Carregando estado do arquivo: " + caminhoArquivo);

            Gson gson = new Gson();
            Set<SiteData> loadedSiteData = gson.fromJson(reader, new TypeToken<Set<SiteData>>() {
            }.getType());

            if (loadedSiteData == null) {
                loadedSiteData = new HashSet<>();
            }

            // Adquirir write lock para atualização dos índices
            indexLock.writeLock().lock();
            try {
                // Limpar índices existentes
                invertedIndex.clear();
                urlReferences.clear();
                urlTexts.clear();
                incomingLinks.clear();

                synchronized (siteDataSet) {
                    siteDataSet.clear();
                    siteDataSet.addAll(loadedSiteData);
                }

                // Reindexa dados localmente
                for (SiteData siteData : loadedSiteData) {
                    // Processar sem propagar
                    siteData.setPropagated(true);
                    processLocalUpdate(siteData);
                }
            } finally {
                indexLock.writeLock().unlock();
            }

            System.out.println(getTimestamp() + " : 📊 Estado carregado - Entradas: " + loadedSiteData.size());

        } catch (Exception e) {
            System.err.println(getTimestamp() + " : ❌ Erro ao carregar JSON: " + e.getMessage());
            e.printStackTrace();
            synchronized (siteDataSet) {
                siteDataSet.clear();
            }
        }
    }

    /**
     * Retorna as URLs que apontam para uma URL específica.
     * 
     * @param url URL para a qual as URLs que apontam devem ser retornadas.
     * @return Lista de URLs que apontam para a URL especificada.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public List<String> getIncomingLinksForUrl(String url) throws RemoteException {
        indexLock.readLock().lock();
        try {
            if (url == null || url.isEmpty()) {
                return new ArrayList<>();
            }
            List<String> referenciadores = incomingLinks.getOrDefault(url, new ArrayList<>());
            return new ArrayList<>(referenciadores); // Retorna uma cópia da lista
        } finally {
            indexLock.readLock().unlock();
        }
    }

    /**
     * Método principal para iniciar a barrel.
     * 
     * @param args Argumentos da linha de comando, incluindo o ID da barrel e o
     *             arquivo de propriedades.
     */
    public static void main(String[] args) {
        String registryNibs;

        try {
            // Verificar argumentos
            if (args.length < 2) {
                System.err.println(
                        LocalDateTime.now() + " : ❌ Erro: Necessário fornecer ID da barrel e arquivo de propriedades");
                System.out.println(
                        LocalDateTime.now() + " : ℹ️ Uso: java IndexStorageBarrel <barrelId> <arquivo.properties>");
                System.exit(1);
            }

            // Carregar arquivo de propriedades
            Properties prop = new Properties();
            try (InputStream input = new FileInputStream(args[1])) {
                System.out.println(LocalDateTime.now() + " : 📝 Carregando arquivo de propriedades...");
                prop.load(input);
                System.out.println(LocalDateTime.now() + " : ✅ Arquivo de propriedades carregado com sucesso");
            } catch (Exception e) {
                System.err.println(
                        LocalDateTime.now() + " : ❌ Erro ao carregar arquivo de propriedades: " + e.getMessage());
                System.exit(1);
            }

            // Obter endereço do registry RMI
            registryNibs = prop.getProperty("registryNibs");
            if (registryNibs == null || registryNibs.isEmpty()) {
                System.err.println(LocalDateTime.now() + " : ❌ Erro: propriedade 'registryNibs' não encontrada");
                System.exit(1);
            }
            System.out.println(LocalDateTime.now() + " : 🔍 Registry: " + registryNibs);

            // Conectar ao gateway
            RMIGatewayIBSDownloader gateway;
            try {
                System.out.println(LocalDateTime.now() + " : 🔄 Conectando ao gateway: " + registryNibs);
                gateway = (RMIGatewayIBSDownloader) Naming.lookup(registryNibs);
                System.out.println(LocalDateTime.now() + " : ✅ Conexão estabelecida com o gateway");
            } catch (Exception e) {
                System.err.println(LocalDateTime.now() + " : ❌ Erro ao conectar ao Gateway: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
                return;
            }

            // Verificar se o ID da barrel já está em uso
            int barrelId = Integer.parseInt(args[0]);
            try {
                Map<Integer, RMIIndexStorageBarrel> existingBarrels = gateway.getBarrels();
                if (existingBarrels.containsKey(barrelId)) {
                    System.err.println(LocalDateTime.now() + " : ❌ ERRO: Já existe uma Barrel com ID " + barrelId);
                    System.err.println(LocalDateTime.now() + " : ⚠️  O sistema não permite IDs duplicados");
                    System.err.println(LocalDateTime.now() + " : 📋 IDs já registrados: " + existingBarrels.keySet());
                    System.err.println(LocalDateTime.now() + " : 🛑 O programa será encerrado");
                    System.exit(1);
                }
            } catch (RemoteException e) {
                System.err.println(LocalDateTime.now() + " : ❌ Erro ao verificar IDs existentes: " + e.getMessage());
                System.exit(1);
            }

            try {
                // Criar a nova barrel
                System.out.println(LocalDateTime.now() + " : 🚀 Criando IndexStorageBarrel com ID " + barrelId);
                IndexStorageBarrel barrel = new IndexStorageBarrel(barrelId);

                // Registrar a barrel no gateway
                System.out.println(LocalDateTime.now() + " : 🔄 Registrando barrel no gateway...");
                gateway.registerIBS(barrel.barrelId, barrel);
                System.out.println(LocalDateTime.now() + " : ✅ Barrel " + barrelId + " registrada com sucesso!");

                // Sincronizar com barrels existentes após o registro
                System.out.println(LocalDateTime.now() + " : 🔄 Iniciando sincronização com barrels existentes...");
                barrel.syncWithExistingBarrels(gateway);

                System.out.println(LocalDateTime.now() + " : 🎉 Barrel " + barrelId + " inicializada e pronta!");

            } catch (Exception e) {
                System.err.println(LocalDateTime.now() + " : ❌ Erro ao criar/registrar Barrel: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }

        } catch (NumberFormatException e) {
            System.err.println(LocalDateTime.now() + " : ❌ Erro: O ID da barrel deve ser um número válido");
            System.exit(1);
        } catch (Exception e) {
            System.err.println(LocalDateTime.now() + " : ❌ Erro inesperado: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}