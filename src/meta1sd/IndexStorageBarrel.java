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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
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
    private int barrelId;

    // Referências para outras barrels no sistema
    private Map<Integer, RMIIndexStorageBarrel> barrels = new HashMap<>();

    // Estruturas para indexação e rastreamento
    private Map<String, Set<String>> invertedIndex = new HashMap<>(); // Palavras -> URLs
    private Map<String, Integer> urlReferences = new HashMap<>(); // URL -> contagem de referências
    private Map<String, String> urlTexts = new HashMap<>(); // URL -> Texto associado
    private Map<String, List<String>> incomingLinks = new HashMap<>(); // URL -> Lista de URLs que apontam para ela

    // Conjunto de sites armazenados localmente
    private Set<SiteData> siteDataSet = new HashSet<>();

    /**
     * Retorna o índice invertido (palavra -> conjunto de URLs)
     */
    @Override
    public Map<String, Set<String>> getInvertedIndex() throws RemoteException {
        return new HashMap<>(invertedIndex);
    }

    /**
     * Retorna o mapa de links de entrada (URL -> lista de URLs que apontam para
     * ela)
     */
    @Override
    public Map<String, List<String>> getIncomingLinksMap() throws RemoteException {
        return new HashMap<>(incomingLinks);
    }

    /**
     * Retorna o mapa de referências de URL (URL -> contagem de referências)
     */
    @Override
    public Map<String, Integer> getUrlReferences() throws RemoteException {
        return new HashMap<>(urlReferences);
    }

    /**
     * Retorna o mapa de textos de URL (URL -> texto associado)
     */
    @Override
    public Map<String, String> getUrlTexts() throws RemoteException {
        return new HashMap<>(urlTexts);
    }

    /**
     * Obtém o timestamp formatado para logs
     */
    private String getTimestamp() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    /**
     * Construtor da barrel
     * 
     * @param barrelId Identificador único da barrel
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
     * Sincroniza com barrels existentes obtidas do gateway
     * 
     * @param gateway Interface do gateway para obter as barrels registradas
     */
    public void syncWithExistingBarrels(RMIGatewayIBSDownloader gateway) throws RemoteException {
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

        } catch (Exception e) {
            System.err.println(getTimestamp() + " : ❌ Erro durante a tentativa de sincronização: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método otimizado para sincronizar dados de uma barrel existente
     * Esta versão transfere os mapas inteiros de uma vez em vez de processar item
     * por item
     */
    public void syncFromExistingBarrel(RMIIndexStorageBarrel existingBarrel) throws RemoteException {
        System.out.println(getTimestamp() + " : 📥 Iniciando sincronização de dados completos...");

        try {
            long startTime = System.currentTimeMillis();
            int totalItemsSynced = 0;

            // 1. Sincronizar o índice invertido (palavras -> URLs)
            System.out.println(getTimestamp() + " : 🔄 Sincronizando índice invertido...");
            Map<String, Set<String>> existingInvertedIndex = existingBarrel.getInvertedIndex();
            if (existingInvertedIndex != null) {
                for (Map.Entry<String, Set<String>> entry : existingInvertedIndex.entrySet()) {
                    String word = entry.getKey();
                    Set<String> urls = entry.getValue();

                    // Criar ou unir conjuntos de URLs para cada palavra
                    invertedIndex.computeIfAbsent(word, k -> new HashSet<>()).addAll(urls);
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
                        (url, count) -> urlReferences.put(url, Math.max(urlReferences.getOrDefault(url, 0), count)));
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
                    List<String> currentLinks = incomingLinks.computeIfAbsent(url, k -> new ArrayList<>());

                    // Adicionar apenas links não duplicados
                    for (String link : links) {
                        if (!currentLinks.contains(link)) {
                            currentLinks.add(link);
                        }
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
                    if (!urlTexts.containsKey(url) || urlTexts.get(url).isEmpty()) {
                        urlTexts.put(url, text);
                    }
                });
                totalItemsSynced += existingUrlTexts.size();
                System.out.println(
                        getTimestamp() + " : ✅ Textos de URLs sincronizados - " + existingUrlTexts.size() + " URLs");
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
     * Registra uma barrel na lista local de barrels
     */
    public void registeroneIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException {
        if (id != this.barrelId) {
            barrels.put(id, barrel);
            System.out.println(getTimestamp() + " : 📝 Guardando a barrel " + id);
        } else {
            System.out.println(getTimestamp() + " : ⚠️ Ignorando registro da própria barrel " + id);
        }
    }

    /**
     * Registra esta barrel em todas as outras barrels do sistema
     */
    public void registerallIBS(Map<Integer, RMIIndexStorageBarrel> barrells, int myid,
            RMIIndexStorageBarrel mybarrel) throws RemoteException {
        System.out.println(getTimestamp() + " : 🔄 Registrando em outras barrels...");

        if (barrells.isEmpty()) {
            System.out.println(getTimestamp() + " : ℹ️ Não há outras barrels para registrar");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<Integer, RMIIndexStorageBarrel> entry : barrells.entrySet()) {
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
     * Método usado para verificar se a barrel está ativa
     */
    public void gatewaypong(String provider) throws RemoteException {
        System.out.println(getTimestamp() + " : 🔔 " + provider + ":Pong");
    }

    /**
     * Armazena dados de um site, atualizando os índices apropriados
     */
    @Override
    public void storeSiteData(SiteData siteData) throws RemoteException {
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
     * Processa atualização local dos dados de um site
     */
    private void processLocalUpdate(SiteData siteData) {
        if (siteData == null || siteData.url == null || siteData.url.isEmpty()) {
            System.err.println(getTimestamp() + " : ⚠️ SiteData inválido para processamento local");
            return;
        }

        System.out.println(getTimestamp() + " : 📝 Processando atualização local para URL: " + siteData.url);

        // Armazenar texto da página se disponível
        if (siteData.text != null && !siteData.text.isEmpty()) {
            urlTexts.put(siteData.url, siteData.text);
            System.out.println(getTimestamp() + " : 🧾 Texto armazenado para URL: " + siteData.url);
            System.out.println(getTimestamp() + " : 🔍 Conteúdo: " + siteData.text);
        }

        // Indexar tokens (palavras-chave)
        if (siteData.tokens != null && !siteData.tokens.isEmpty()) {
            indexTokens(siteData.tokens, siteData.url);
        }

        // Processar links e atualizar contagem de referências
        if (siteData.links != null && !siteData.links.isEmpty()) {
            String[] links = siteData.links.split("\\s+");
            for (String link : links) {
                if (link.isEmpty())
                    continue;

                // Incrementar contador de referências para o link
                urlReferences.put(link, urlReferences.getOrDefault(link, 0) + 1);

                // Adicionar URL atual à lista de páginas que apontam para o link
                incomingLinks.computeIfAbsent(link, k -> new ArrayList<>());
                if (!incomingLinks.get(link).contains(siteData.url)) {
                    incomingLinks.get(link).add(siteData.url);
                }
            }
        }

        // Adiciona à lista de sites para armazenar em json
        siteDataSet.add(siteData);

        // Salvar estado após a atualização
        saveState("data/estado_barrel_" + barrelId + ".json");
        System.out.println(getTimestamp() + " : ✅ Atualização local concluída para URL: " + siteData.url);
    }

    /**
     * Propaga atualização de dados para outras barrels
     */
    private void propagateUpdate(SiteData siteData) {
        if (barrels.isEmpty()) {
            System.out.println(getTimestamp() + " : ℹ️ Não há outras barrels para propagar a atualização");
            return;
        }

        System.out.println(getTimestamp() + " : 🔄 Propagando atualização para outras barrels...");
        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<Integer, RMIIndexStorageBarrel> entry : barrels.entrySet()) {
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
     * Indexa tokens (palavras) de uma página
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
            invertedIndex.computeIfAbsent(token, k -> new HashSet<>()).add(url);
            tokenCount++;
        }

        if (tokenCount > 0) {
            System.out.println(getTimestamp() + " : 📊 Indexados " + tokenCount + " tokens para URL: " + url);
        }
    }

    /**
     * Pesquisa páginas que contêm todas as palavras especificadas
     */
    public List<String> searchPagesByWords(Set<String> words) {
        if (words == null || words.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        Map<String, Integer> pageMatchCount = new HashMap<>();

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

        System.out.println(
                getTimestamp() + " : 🔍 Pesquisa concluída - Palavras: " + words + ", Resultados: " + result.size());
        return result;
    }

    /**
     * Retorna a contagem de referências para uma URL
     */
    public int getUrlReferenceCount(String url) {
        return urlReferences.getOrDefault(url, 0);
    }

    /**
     * Retorna páginas ordenadas por número de links apontando para elas
     */
    public List<Map.Entry<String, Integer>> getPagesOrderedByIncomingLinks() throws RemoteException {
        List<Map.Entry<String, Integer>> sortedPages = new ArrayList<>(urlReferences.entrySet());
        sortedPages.sort((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue()));
        return sortedPages;
    }

    /**
     * Retorna páginas que apontam para uma URL específica
     */
    public List<String> getPagesLinkingTo(String url) {
        return incomingLinks.getOrDefault(url, new ArrayList<>());
    }

    /**
     * Salva o estado atual da barrel em um arquivo JSON
     */
    public void saveState(String caminhoArquivo) {
        try {
            File file = new File(caminhoArquivo);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(siteDataSet);

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
     * Carrega o estado da barrel a partir de um arquivo JSON
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
            siteDataSet = gson.fromJson(reader, new TypeToken<Set<SiteData>>() {
            }.getType());

            if (siteDataSet == null) {
                siteDataSet = new HashSet<>();
            }

            // Reindexa dados localmente
            for (SiteData siteData : siteDataSet) {
                processLocalUpdate(siteData);
            }

            System.out.println(getTimestamp() + " : 📊 Estado carregado - Entradas: " + siteDataSet.size());

        } catch (Exception e) {
            System.err.println(getTimestamp() + " : ❌ Erro ao carregar JSON: " + e.getMessage());
            e.printStackTrace();
            siteDataSet = new HashSet<>();
        }
    }

    /**
     * Retorna as URLs que apontam para uma URL específica
     */
    public List<String> getIncomingLinksForUrl(String url) throws RemoteException {
        if (url == null || url.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> referenciadores = incomingLinks.getOrDefault(url, new ArrayList<>());
        return new ArrayList<>(referenciadores); // Retorna uma cópia da lista para evitar modificações externas
    }

    /**
     * Método principal para iniciar a barrel
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