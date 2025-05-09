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

                    // Sincronizar estado (fazendo merge)
                    System.out.println(
                            getTimestamp() + " : 🔄 Iniciando sincronização (merge) com barrel " + targetBarrelId
                                    + "...");
                    syncFromExistingBarrel(existingBarrel);

                    System.out.println(
                            getTimestamp() + " : ✅ Sincronizado (merge) com sucesso com a barrel " + targetBarrelId);
                    syncSuccess = true;
                    break; // Sincroniza apenas com a primeira ativa
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
     * Método otimizado para sincronizar dados de uma barrel existente, fazendo
     * MERGE dos dados.
     *
     * @param existingBarrel A barrel existente para sincronização.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    /**
     * Método otimizado para sincronizar dados de uma barrel existente, fazendo
     * MERGE dos dados e evitando duplicatas.
     *
     * @param existingBarrel A barrel existente para sincronização.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public synchronized void syncFromExistingBarrel(RMIIndexStorageBarrel existingBarrel) throws RemoteException {
        System.out.println(getTimestamp() + " : 📥 Iniciando sincronização (merge) de dados completos com Barrel "
                + existingBarrel.hashCode() + "..."); // Usar ID seria melhor se disponível

        try {
            long startTime = System.currentTimeMillis();
            int totalItemsProcessed = 0; // Renomeado para clareza

            // 0. Obter dados da barrel existente
            System.out.println(getTimestamp() + " : 🔄 Obtendo dados da barrel remota...");
            Set<SiteData> existingSiteData = existingBarrel.getSiteDataSet();
            Map<String, Set<String>> existingInvertedIndex = existingBarrel.getInvertedIndex();
            Map<String, Integer> existingUrlReferences = existingBarrel.getUrlReferences();
            Map<String, List<String>> existingIncomingLinks = existingBarrel.getIncomingLinksMap();
            Map<String, String> existingUrlTexts = existingBarrel.getUrlTexts();
            System.out.println(getTimestamp() + " : ✅ Dados remotos obtidos.");

            // 1. Fazer MERGE do siteDataSet (remoto sobrepõe local em caso de conflito de
            // URL)
            System.out.println(getTimestamp() + " : 🔄 Fazendo merge do SiteData...");
            if (existingSiteData != null) {
                synchronized (siteDataSet) {
                    for (SiteData remoteSiteData : existingSiteData) {
                        remoteSiteData.setPropagated(true); // Marcar como já propagado
                        // Remove duplicata local se existir, para usar a versão remota
                        boolean removed = siteDataSet.removeIf(localSite -> localSite.url.equals(remoteSiteData.url));
                        siteDataSet.add(remoteSiteData); // Adiciona a versão remota
                        if (!removed) {
                            totalItemsProcessed++; // Conta como novo item se não existia localmente
                        }
                    }
                }
                System.out.println(
                        getTimestamp() + " : ✅ SiteData merge concluído - " + existingSiteData.size()
                                + " itens remotos processados. Tamanho atual: " + siteDataSet.size());
            }

            // Usar write lock para atualização dos índices
            indexLock.writeLock().lock();
            try {
                // 2. Fazer MERGE do índice invertido (palavras -> URLs)
                System.out.println(getTimestamp() + " : 🔄 Fazendo merge do índice invertido...");
                if (existingInvertedIndex != null) {
                    for (Map.Entry<String, Set<String>> entry : existingInvertedIndex.entrySet()) {
                        String word = entry.getKey();
                        Set<String> remoteUrls = entry.getValue();
                        if (remoteUrls == null || remoteUrls.isEmpty())
                            continue;

                        // Merge: adiciona URLs remotas ao conjunto local (Set lida com duplicatas)
                        Set<String> localUrls = invertedIndex.computeIfAbsent(word,
                                k -> ConcurrentHashMap.newKeySet()); // Usa set thread-safe
                        localUrls.addAll(remoteUrls);
                    }
                    totalItemsProcessed += existingInvertedIndex.size(); // Conta palavras processadas
                    System.out.println(getTimestamp() + " : ✅ Índice invertido merge concluído - "
                            + existingInvertedIndex.size() + " palavras remotas processadas.");
                }

                // 3. Fazer MERGE das referências de URLs (usando contagem máxima)
                System.out.println(getTimestamp() + " : 🔄 Fazendo merge das referências de URLs...");
                if (existingUrlReferences != null) {
                    existingUrlReferences.forEach(
                            (url, remoteCount) -> urlReferences.compute(url,
                                    // Usa o maior valor entre o local e o remoto
                                    (k, localCount) -> (localCount == null) ? remoteCount
                                            : Math.max(localCount, remoteCount)));
                    totalItemsProcessed += existingUrlReferences.size(); // Conta URLs processadas
                    System.out.println(getTimestamp() + " : ✅ Referências de URLs merge concluído - "
                            + existingUrlReferences.size() + " URLs remotas processadas.");
                }

                // 4. Fazer MERGE dos links de entrada (adicionando apenas links não existentes)
                System.out.println(getTimestamp() + " : 🔄 Fazendo merge dos links de entrada...");
                if (existingIncomingLinks != null) {
                    for (Map.Entry<String, List<String>> entry : existingIncomingLinks.entrySet()) {
                        String targetUrl = entry.getKey();
                        List<String> remoteLinks = entry.getValue();
                        if (remoteLinks == null || remoteLinks.isEmpty())
                            continue;

                        // Merge: adiciona links remotos que não existem localmente
                        List<String> localLinks = incomingLinks.computeIfAbsent(targetUrl,
                                k -> Collections.synchronizedList(new ArrayList<>())); // Usa lista thread-safe
                        synchronized (localLinks) { // Sincroniza a lista específica para a verificação/adição
                            for (String remoteLink : remoteLinks) {
                                if (!localLinks.contains(remoteLink)) {
                                    localLinks.add(remoteLink);
                                }
                            }
                        }
                    }
                    totalItemsProcessed += existingIncomingLinks.size(); // Conta URLs alvo processadas
                    System.out.println(getTimestamp() + " : ✅ Links de entrada merge concluído - "
                            + existingIncomingLinks.size() + " URLs alvo remotas processadas.");
                }

                // 5. Fazer MERGE dos textos associados às URLs (mantendo texto local se
                // existir)
                System.out.println(getTimestamp() + " : 🔄 Fazendo merge dos textos de URLs...");
                if (existingUrlTexts != null) {
                    // Adiciona textos remotos apenas se a URL não existe localmente
                    existingUrlTexts.forEach((url, remoteText) -> {
                        urlTexts.putIfAbsent(url, remoteText); // putIfAbsent faz o merge (prioriza local)
                    });
                    totalItemsProcessed += existingUrlTexts.size(); // Conta URLs processadas
                    System.out.println(
                            getTimestamp() + " : ✅ Textos de URLs merge concluído - " + existingUrlTexts.size()
                                    + " URLs remotas processadas.");
                }
            } finally {
                indexLock.writeLock().unlock();
            }

            // ETAPA 6 REMOVIDA - A RECONSTRUÇÃO NÃO É MAIS NECESSÁRIA APÓS O MERGE DIRETO

            long endTime = System.currentTimeMillis();
            double seconds = (endTime - startTime) / 1000.0;

            System.out.println(getTimestamp() + " : ✅ Sincronização (merge) concluída em " + seconds + " segundos!");
            System.out.println(
                    getTimestamp() + " : 📊 Total de itens remotos processados (aproximado): " + totalItemsProcessed);
            System.out.println(getTimestamp() + " : 📊 Estado final local - Sites: " + siteDataSet.size()
                    + ", Palavras: " + invertedIndex.size() + ", Refs: " + urlReferences.size());

            // Salvar o estado merged no arquivo local
            saveState("data/estado_barrel_" + barrelId + ".json");

        } catch (RemoteException re) {
            System.err.println(getTimestamp() + " : ❌ Erro RMI durante a sincronização (merge): " + re.getMessage());
            re.printStackTrace();
            throw re; // Re-lança a exceção RMI
        } catch (Exception e) {
            System.err.println(getTimestamp() + " : ❌ Erro geral durante a sincronização (merge): " + e.getMessage());
            e.printStackTrace();
            // Considerar lançar uma RemoteException encapsulada ou tratar de outra forma
            throw new RemoteException("Falha na sincronização (merge) devido a erro interno", e);
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
                    this.registeroneIBS(barid, barr); // Registra a outra em mim
                    barr.registeroneIBS(myid, mybarrel); // Registra a mim na outra
                    barr.gatewaypong("Barrel" + myid); // Verifica se a outra está ativa
                    System.out.println(getTimestamp() + " : ✅ Registrada na barrel " + barid);
                    successCount++;
                }
            } catch (RemoteException e) {
                System.err
                        .println(getTimestamp() + " : ❌ Falha ao registrar na barrel " + barid + ": " + e.getMessage());
                failCount++;
                // Considerar remover a barrel inativa daqui também
                // barrels.remove(barid);
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
     * Este método NÃO é mais synchronized para evitar deadlocks em RMI.
     *
     * @param siteData Dados do site a serem armazenados.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    @Override
    public void storeSiteData(SiteData siteData) throws RemoteException {
        System.out.println(getTimestamp() + " : [Barrel " + barrelId + "] Recebendo storeSiteData para " + siteData.url
                + " | isPropagated=" + siteData.isPropagated());

        if (siteData == null || siteData.url == null || siteData.url.isEmpty()) {
            System.err.println(getTimestamp() + " : ⚠️ Tentativa de armazenar SiteData inválido");
            return;
        }

        // Se já foi propagado, apenas processa localmente
        if (siteData.isPropagated()) {
            processLocalUpdate(siteData);
            return;
        }

        // Processa localmente
        processLocalUpdate(siteData);

        // Cria uma CÓPIA para propagar, marcando como propagado
        SiteData copyToPropagate = new SiteData(siteData.url, siteData.tokens, siteData.links);
        copyToPropagate.text = siteData.text;
        copyToPropagate.title = siteData.title;
        copyToPropagate.setPropagated(true); // Marca a CÓPIA como propagada

        // Propaga a CÓPIA para outras barrels (sem lock aqui)
        propagateUpdate(copyToPropagate);
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
                // System.out.println(getTimestamp() + " : 🧾 Texto armazenado (" +
                // siteData.text.length() + " chars)");
            }

            // 2. Processar tokens (palavras-chave)
            if (siteData.tokens != null && !siteData.tokens.isEmpty()) {
                // System.out.println(getTimestamp() + " : 🔠 Indexando tokens...");
                indexTokens(siteData.tokens, siteData.url);
            } else {
                // System.out.println(getTimestamp() + " : ℹ️ Nenhum token para indexar");
            }

            // 3. Processar links
            if (siteData.links != null && !siteData.links.isEmpty()) {
                // System.out.println(getTimestamp() + " : 🔗 Processando links...");
                String[] links = siteData.links.split("\\s+");
                int newLinks = 0;

                for (String link : links) {
                    if (link.isEmpty())
                        continue;

                    // Atualizar contagem de referências
                    int newCount = urlReferences.compute(link, (k, v) -> (v == null) ? 1 : v + 1);
                    // System.out.println(getTimestamp() + " : 🔗 Link encontrado: " + link
                    // + " (contagem atual: " + newCount + ")");
                    if (newCount == 1) {
                        newLinks++;
                        // System.out.println(getTimestamp() + " : 🔗 Novo link encontrado: " + link);
                    }

                    // Atualizar links de entrada
                    List<String> incomingLinksList = incomingLinks.computeIfAbsent(link,
                            k -> Collections.synchronizedList(new ArrayList<>()));

                    synchronized (incomingLinksList) {
                        if (!incomingLinksList.contains(siteData.url)) {
                            incomingLinksList.add(siteData.url);
                        }
                    }
                }
                // System.out.println(getTimestamp() + " : ➕ " + newLinks + " novos links de " +
                // links.length + " totais");
            } else {
                // System.out.println(getTimestamp() + " : ℹ️ Nenhum link para processar");
            }

            // 4. Atualizar conjunto principal de sites
            synchronized (siteDataSet) {
                // Remover versão anterior se existir
                boolean existed = siteDataSet.removeIf(site -> site.url.equals(siteData.url));
                siteDataSet.add(siteData); // Adiciona a versão atual (pode ser a mesma ou nova)
                // System.out.println(
                // getTimestamp() + " : " + (existed ? "🔄 Atualizado" : "🆕 Novo") + " SiteData
                // adicionado/atualizado");
            }

        } finally {
            indexLock.writeLock().unlock();
        }

        // 5. Salvar estado (com lock separado para evitar deadlocks)
        // Considerar salvar estado com menos frequência para performance
        stateLock.writeLock().lock();
        try {
            // System.out.println(getTimestamp() + " : 💾 Salvando estado...");
            saveState("data/estado_barrel_" + barrelId + ".json");
        } finally {
            stateLock.writeLock().unlock();
        }

        // System.out.println(getTimestamp() + " : ✅ Atualização local concluída para: "
        // + siteData.url);
    }

    /**
     * Propaga atualização de dados para outras barrels.
     *
     * @param siteData Dados do site a serem propagados (DEVE SER UMA CÓPIA MARCADA
     *                 COMO PROPAGADA).
     */
    public void propagateUpdate(SiteData siteData) throws RemoteException {
        // Garante que estamos propagando um objeto marcado
        if (!siteData.isPropagated()) {
            System.err.println(getTimestamp() + " : ⚠️ ERRO INTERNO: Tentando propagar SiteData não marcado!");
            siteData.setPropagated(true); // Tenta corrigir
        }

        Map<Integer, RMIIndexStorageBarrel> barrelsSnapshot = new HashMap<>(barrels); // Copia para iterar

        if (barrelsSnapshot.isEmpty()) {
            // System.out.println(getTimestamp() + " : ℹ️ Não há outras barrels para
            // propagar a atualização");
            return;
        }

        System.out.println(
                getTimestamp() + " : 📤 Propagando atualização para " + barrelsSnapshot.size() + " outras barrels...");
        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<Integer, RMIIndexStorageBarrel> entry : barrelsSnapshot.entrySet()) {
            int targetBarrelId = entry.getKey();
            RMIIndexStorageBarrel targetBarrel = entry.getValue();

            try {
                System.out.println(getTimestamp() + " : 📤 Enviando para barrel " + targetBarrelId + "...");
                targetBarrel.storeSiteData(siteData); // Envia a cópia marcada
                System.out.println(
                        getTimestamp() + " : ✅ Atualização propagada com sucesso para barrel " + targetBarrelId);
                successCount++;
            } catch (RemoteException e) {
                System.err.println(getTimestamp() + " : ❌ Falha ao propagar atualização para barrel " + targetBarrelId
                        + ": " + e.getMessage());
                failCount++;

                // Tenta verificar se a barrel está realmente inativa antes de remover
                try {
                    targetBarrel.gatewaypong("PropagateCheck" + barrelId);
                } catch (RemoteException re) {
                    System.err.println(
                            getTimestamp() + " : ❌ Barrel " + targetBarrelId
                                    + " não responde ao pong. Removendo do registro local.");
                    barrels.remove(targetBarrelId); // Remove do mapa original
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

        // if (tokenCount > 0) {
        // System.out.println(getTimestamp() + " : 📊 Indexados " + tokenCount + "
        // tokens para URL: " + url);
        // }
    }

    /**
     * Pesquisa páginas que contêm todas as palavras especificadas, retornando-as
     * ordenadas pelo número de links que apontam para elas.
     *
     * @param words Conjunto de palavras a serem pesquisadas.
     * @return Lista de URLs que contêm todas as palavras especificadas, ordenadas.
     */
    public List<String> searchPagesByWords(Set<String> words) throws RemoteException { // Adicionado throws
                                                                                       // RemoteException
        if (words == null || words.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Integer> pageMatchCount = new HashMap<>();
        indexLock.readLock().lock();
        try {
            // Itera sobre as palavras e conta correspondências
            for (String word : words) {
                word = word.toLowerCase().trim();
                Set<String> pages = invertedIndex.get(word);
                if (pages != null) {
                    for (String page : pages) {
                        pageMatchCount.put(page, pageMatchCount.getOrDefault(page, 0) + 1);
                    }
                }
            }
        } finally {
            indexLock.readLock().unlock();
        }

        // Filtra as páginas que contêm todas as palavras
        List<String> matchingPages = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : pageMatchCount.entrySet()) {
            if (entry.getValue() == words.size()) {
                matchingPages.add(entry.getKey());
            }
        }

        // Ordena as páginas correspondentes pelo número de links de entrada
        List<Map.Entry<String, Integer>> sortedAllPages = getPagesOrderedByIncomingLinks(); // Ordenação global

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> sortedEntry : sortedAllPages) {
            if (matchingPages.contains(sortedEntry.getKey())) {
                result.add(sortedEntry.getKey());
            }
        }

        System.out.println(
                getTimestamp() + " : 🔍 Pesquisa concluída - Palavras: " + words + ", Resultados ordenados: "
                        + result.size());
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
            // Ordena decrescente pelo valor (contagem de links)
            sortedPages.sort((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue()));
            // Log removido para não poluir
            // for (Map.Entry<String, Integer> entry : sortedPages) {
            // System.out.println(getTimestamp() + " : 📊 Página: " + entry.getKey() + ",
            // Links: " + entry.getValue());
            // }
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
            List<String> result = incomingLinks.getOrDefault(url, Collections.emptyList()); // Usa lista vazia imutável
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
        // Adquire o lock de estado para garantir que não haja salvamento concorrente
        stateLock.writeLock().lock();
        try {
            File file = new File(caminhoArquivo);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            Set<SiteData> siteDataCopy;
            // Sincroniza o acesso ao siteDataSet para criar a cópia
            synchronized (siteDataSet) {
                siteDataCopy = new HashSet<>(siteDataSet);
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(siteDataCopy);

            try (FileWriter writer = new FileWriter(caminhoArquivo)) {
                writer.write(json);
            }

            System.out.println(
                    getTimestamp() + " : 💾 Estado (" + siteDataCopy.size() + " sites) salvo em: " + caminhoArquivo);

        } catch (Exception e) {
            System.err.println(getTimestamp() + " : ❌ Erro ao salvar estado no ficheiro JSON: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stateLock.writeLock().unlock();
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
            System.out.println(getTimestamp() + " : ℹ️ Nenhum estado salvo encontrado em: " + caminhoArquivo);
            return;
        }

        // Adquire locks para garantir exclusividade durante o carregamento
        stateLock.writeLock().lock();
        indexLock.writeLock().lock();
        try (FileReader reader = new FileReader(file)) {
            System.out.println(getTimestamp() + " : 📂 Carregando estado do arquivo: " + caminhoArquivo);

            Gson gson = new Gson();
            Set<SiteData> loadedSiteData = gson.fromJson(reader, new TypeToken<Set<SiteData>>() {
            }.getType());

            if (loadedSiteData == null) {
                loadedSiteData = new HashSet<>();
            }

            // Limpar estruturas atuais ANTES de carregar e reindexar
            invertedIndex.clear();
            urlReferences.clear();
            urlTexts.clear();
            incomingLinks.clear();
            synchronized (siteDataSet) {
                siteDataSet.clear();
                siteDataSet.addAll(loadedSiteData); // Adiciona os dados carregados
            }

            System.out.println(getTimestamp() + " : 🔄 Reindexando dados carregados...");
            // Reindexa dados localmente a partir do JSON carregado
            for (SiteData siteData : loadedSiteData) {
                // Processar sem propagar, pois já está no estado salvo
                siteData.setPropagated(true);
                processLocalUpdate(siteData); // Chama o método que atualiza os índices
            }

            System.out.println(
                    getTimestamp() + " : 📊 Estado carregado e reindexado - Entradas: " + loadedSiteData.size());

        } catch (Exception e) {
            System.err.println(getTimestamp() + " : ❌ Erro ao carregar/reindexar estado do JSON: " + e.getMessage());
            e.printStackTrace();
            // Limpa tudo em caso de erro grave no carregamento
            invertedIndex.clear();
            urlReferences.clear();
            urlTexts.clear();
            incomingLinks.clear();
            synchronized (siteDataSet) {
                siteDataSet.clear();
            }
        } finally {
            indexLock.writeLock().unlock();
            stateLock.writeLock().unlock();
        }
    }

    /**
     * Retorna as URLs que apontam para uma URL específica, ordenadas pelo número
     * de links que apontam para elas.
     *
     * @param url URL para a qual as URLs que apontam devem ser retornadas.
     * @return Lista de URLs que apontam para a URL especificada, ordenadas.
     * @throws RemoteException Se ocorrer um erro de comunicação remota.
     */
    public List<String> getIncomingLinksForUrl(String url) throws RemoteException {
        indexLock.readLock().lock();
        try {
            if (url == null || url.isEmpty()) {
                return new ArrayList<>();
            }

            // Obtém os links que apontam para a URL
            List<String> referenciadores = incomingLinks.getOrDefault(url, Collections.emptyList());
            if (referenciadores.isEmpty()) {
                return new ArrayList<>();
            }

            // Obtém todas as páginas ordenadas por número de links
            List<Map.Entry<String, Integer>> sortedPages = getPagesOrderedByIncomingLinks();

            // Filtra os referenciadores com base na ordenação global
            List<String> ordenados = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : sortedPages) {
                // Verifica se a página ordenada está na lista de referenciadores
                if (referenciadores.contains(entry.getKey())) {
                    ordenados.add(entry.getKey());
                }
            }

            return ordenados;
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
    /**
     * Método principal para iniciar a barrel.
     *
     * @param args Argumentos da linha de comando, incluindo o ID da barrel e o
     *             arquivo de propriedades.
     */
    public static void main(String[] args) {
        String registryNibs;
        String myIP = null; // Variável para armazenar o IP
        RMIGatewayIBSDownloader gateway = null; // Declarar fora do try para acesso no catch final se necessário
        IndexStorageBarrel barrel = null; // Declarar fora do try para acesso no catch final se necessário
        int barrelId = -1; // Inicializar com valor inválido

        try {
            // Verificar argumentos
            if (args.length < 2) {
                System.err.println(
                        LocalDateTime.now() + " : ❌ Erro: Necessário fornecer ID da barrel e arquivo de propriedades");
                System.out.println(
                        LocalDateTime.now()
                                + " : ℹ️ Uso: java IndexStorageBarrel <barrelId> <arquivo.properties> [seu_ip_opcional]");
                System.exit(1);
            }

            // Validar e obter ID da barrel primeiro
            try {
                barrelId = Integer.parseInt(args[0]);
                if (barrelId < 0) { // Adicionar verificação básica de ID
                    System.err.println(LocalDateTime.now() + " : ❌ Erro: O ID da barrel (" + args[0]
                            + ") deve ser um número não negativo.");
                    System.exit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println(LocalDateTime.now() + " : ❌ Erro: O ID da barrel (" + args[0]
                        + ") deve ser um número inteiro válido.");
                System.exit(1);
            }

            // Tenta obter o IP do terceiro argumento, se fornecido
            if (args.length >= 3) {
                myIP = args[2];
                System.out.println(LocalDateTime.now() + " : ℹ️ Usando IP fornecido: " + myIP);
            }

            // Carregar arquivo de propriedades
            Properties prop = new Properties();
            try (InputStream input = new FileInputStream(args[1])) {
                System.out.println(LocalDateTime.now() + " : 📝 Carregando arquivo de propriedades: " + args[1]);
                prop.load(input);
                System.out.println(LocalDateTime.now() + " : ✅ Arquivo de propriedades carregado com sucesso");

                // Tenta obter o IP do arquivo de propriedades se não foi passado por argumento
                if (myIP == null) {
                    myIP = prop.getProperty("java.rmi.server.hostname");
                    if (myIP != null && !myIP.isEmpty()) {
                        System.out.println(LocalDateTime.now() + " : ℹ️ Usando IP do arquivo de propriedades: " + myIP);
                    } else {
                        System.out.println(LocalDateTime.now()
                                + " : ⚠️ IP não fornecido por argumento nem no arquivo de propriedades. RMI pode usar IP padrão.");
                    }
                }

                // Define a propriedade RMI hostname se um IP foi obtido
                if (myIP != null && !myIP.isEmpty()) {
                    System.setProperty("java.rmi.server.hostname", myIP);
                    System.out.println(LocalDateTime.now() + " : ⚙️ Definido java.rmi.server.hostname=" + myIP);
                }

            } catch (Exception e) {
                System.err.println(
                        LocalDateTime.now() + " : ❌ Erro ao carregar arquivo de propriedades '" + args[1] + "': "
                                + e.getMessage());
                System.exit(1);
            }

            // Obter endereço do registry RMI
            registryNibs = prop.getProperty("registryNibs");
            if (registryNibs == null || registryNibs.isEmpty()) {
                System.err.println(LocalDateTime.now()
                        + " : ❌ Erro: propriedade 'registryNibs' não encontrada no arquivo " + args[1]);
                System.exit(1);
            }
            System.out.println(LocalDateTime.now() + " : 🔍 Endereço do Gateway RMI: " + registryNibs);

            // Conectar ao gateway
            try {
                System.out.println(LocalDateTime.now() + " : 🔄 Conectando ao gateway em " + registryNibs + "...");
                gateway = (RMIGatewayIBSDownloader) Naming.lookup(registryNibs);
                System.out.println(LocalDateTime.now() + " : ✅ Conexão estabelecida com o gateway");
            } catch (Exception e) {
                System.err.println(LocalDateTime.now() + " : ❌ Erro fatal ao conectar ao Gateway RMI em " + registryNibs
                        + ": " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
                // return; // Não é mais necessário devido ao System.exit(1)
            }

            // Verificar se o ID da barrel já está em uso (já temos barrelId validado)
            try {
                System.out.println(LocalDateTime.now() + " : 🔍 Verificando se ID " + barrelId + " já está em uso...");
                Map<Integer, RMIIndexStorageBarrel> existingBarrels = gateway.getBarrels();
                if (existingBarrels.containsKey(barrelId)) {
                    System.err.println(LocalDateTime.now() + " : ❌ ERRO FATAL: Já existe uma Barrel registrada com ID "
                            + barrelId);
                    System.err.println(LocalDateTime.now() + " : ⚠️ O sistema não permite IDs duplicados.");
                    System.err.println(LocalDateTime.now() + " : 📋 IDs já registrados: " + existingBarrels.keySet());
                    System.err.println(LocalDateTime.now() + " : 🛑 O programa será encerrado.");
                    System.exit(1);
                }
                System.out.println(LocalDateTime.now() + " : ✅ ID " + barrelId + " está disponível.");
            } catch (RemoteException e) {
                System.err.println(
                        LocalDateTime.now() + " : ❌ Erro ao verificar IDs existentes no gateway: " + e.getMessage());
                System.exit(1);
            }

            // --- Bloco principal de criação, registro e sincronização ---
            try {
                // Criar a nova barrel (carrega estado local no construtor)
                System.out.println(LocalDateTime.now() + " : 🚀 Criando IndexStorageBarrel com ID " + barrelId);
                barrel = new IndexStorageBarrel(barrelId); // Atribui à variável declarada fora

                // Registrar a barrel no gateway
                System.out.println(LocalDateTime.now() + " : 🔄 Registrando barrel " + barrelId + " no gateway...");
                gateway.registerIBS(barrel.barrelId, barrel);
                System.out.println(
                        LocalDateTime.now() + " : ✅ Barrel " + barrelId + " registrada com sucesso no gateway!");

                // Sincronizar com barrels existentes após o registro (fazendo merge)
                System.out.println(
                        LocalDateTime.now() + " : 🔄 Iniciando sincronização (merge) com barrels existentes...");
                barrel.syncWithExistingBarrels(gateway); // O estado local agora contém o merge

                // Registrar esta barrel nas outras barrels existentes (e vice-versa)
                System.out.println(LocalDateTime.now() + " : 🔄 Registrando esta barrel nas outras existentes...");
                Map<Integer, RMIIndexStorageBarrel> allBarrels = gateway.getBarrels(); // Pega a lista atualizada
                barrel.registerallIBS(allBarrels, barrel.barrelId, barrel);

                System.out.println(
                        LocalDateTime.now() + " : 🎉 Barrel " + barrelId + " inicializada e sincronizada!");

                // ---> INÍCIO DA NOVA LÓGICA: Propagar o estado final da nova barrel para as
                // outras <---
                System.out.println(LocalDateTime.now() + " : 📤 Iniciando propagação do estado final da Barrel "
                        + barrelId + " para as outras...");
                Set<SiteData> finalLocalState;
                synchronized (barrel.siteDataSet) { // Acessa o set sincronizado da barrel
                    finalLocalState = new HashSet<>(barrel.siteDataSet); // Cria uma cópia segura para iterar
                }

                int propagatedCount = 0;
                if (!finalLocalState.isEmpty()) {
                    System.out.println(LocalDateTime.now() + " : 📤 Propagando " + finalLocalState.size()
                            + " itens do estado final...");
                    for (SiteData siteDataToPropagate : finalLocalState) {
                        try {
                            // Cria uma CÓPIA para propagar, garantindo que está marcada como propagada
                            SiteData copyToPropagate = new SiteData(siteDataToPropagate.url, siteDataToPropagate.tokens,
                                    siteDataToPropagate.links);
                            copyToPropagate.text = siteDataToPropagate.text;
                            copyToPropagate.title = siteDataToPropagate.title;
                            copyToPropagate.setPropagated(true); // ESSENCIAL: Marca a cópia como propagada

                            // Chama o método que envia para todas as outras barrels conhecidas pela
                            // instância 'barrel'
                            barrel.propagateUpdate(copyToPropagate);
                            propagatedCount++;

                        } catch (RemoteException e) {
                            System.err.println(LocalDateTime.now() + " : ⚠️ Erro RMI ao propagar SiteData para URL "
                                    + siteDataToPropagate.url + ": " + e.getMessage());
                            // Continua com o próximo item
                        } catch (Exception e) {
                            System.err.println(
                                    LocalDateTime.now() + " : ❌ Erro inesperado ao preparar/propagar SiteData para URL "
                                            + siteDataToPropagate.url + ": " + e.getMessage());
                            e.printStackTrace(); // Log mais detalhado para erros inesperados
                        }
                    }
                    System.out.println(LocalDateTime.now() + " : 📤 Propagação do estado final concluída. "
                            + propagatedCount + " itens enviados para outras barrels.");
                } else {
                    System.out.println(
                            LocalDateTime.now() + " : ℹ️ Nenhum SiteData local para propagar após sincronização.");
                }
                // ---> FIM DA NOVA LÓGICA <---

                System.out.println(
                        LocalDateTime.now() + " : ✅ Barrel " + barrelId + " totalmente operacional.");

            } catch (Exception e) {
                // Este catch lida com erros durante a criação, registro, sync ou propagação
                // final
                System.err.println(LocalDateTime.now()
                        + " : ❌ Erro fatal durante a fase de inicialização/sincronização/propagação da Barrel "
                        + barrelId + ": " + e.getMessage());
                e.printStackTrace();
                // Tentar desregistrar do gateway se já foi registrado
                if (gateway != null && barrel != null && barrelId != -1) {
                    try {
                        System.out.println(LocalDateTime.now() + " : 🔄 Tentando desregistrar Barrel " + barrelId
                                + " do gateway devido a erro...");
                        gateway.unsubscribeIBS(barrelId);
                        System.out.println(
                                LocalDateTime.now() + " : ✅ Barrel " + barrelId + " desregistrada do gateway.");
                    } catch (RemoteException re) {
                        System.err.println(LocalDateTime.now() + " : ⚠️ Falha ao desregistrar Barrel " + barrelId
                                + " do gateway: " + re.getMessage());
                    }
                }
                System.exit(1);
            }

            // Catch para erros que podem ocorrer antes da conexão com o gateway ou
            // validação de ID
        } catch (Exception e) {
            System.err.println(LocalDateTime.now()
                    + " : ❌ Erro inesperado e fatal na inicialização (antes da criação da barrel): " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Se chegou aqui, a barrel está rodando. O programa principal (main thread)
        // pode terminar,
        // mas o objeto RMI (barrel) continuará vivo por causa do UnicastRemoteObject.
        System.out.println(LocalDateTime.now() + " : ▶️ Thread principal (main) da Barrel " + barrelId
                + " concluída. Objeto RMI permanece ativo.");

    }
}