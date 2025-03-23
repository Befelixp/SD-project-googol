package meta1sd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import com.google.gson.reflect.TypeToken;

public class IndexStorageBarrel extends UnicastRemoteObject implements RMIIndexStorageBarrel {

    private int barrelId;
    private Map<Integer, RMIIndexStorageBarrel> barrels = new HashMap<>();

    // Estruturas para indexação e rastreamento
    private Map<String, Set<String>> invertedIndex = new HashMap<>(); // Palavras -> URLs
    private Map<String, Integer> urlReferences = new HashMap<>(); // URL -> contagem de referências
    private Map<String, String> urlTexts = new HashMap<>(); // URL -> Texto associado

    private Map<String, List<String>> incomingLinks = new HashMap<>();

    public IndexStorageBarrel(int barrelId) throws RemoteException {
        this.barrelId = barrelId;
        System.out.println(LocalDateTime.now() + " : System " + barrelId + " is starting up");
        carregarEstadoDeJSON("data/estado_barrel_" + barrelId + ".json");
    }

    public void registeroneIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException {
        // Não registra se for ela mesma
        if (id != this.barrelId) {
            barrels.put(id, barrel);
            System.out.println("Guardando a barrel " + id);
        } else {
            System.out.println("Ignorando registro da própria barrel " + id);
        }
    }

    public void registerallIBS(Map<Integer, RMIIndexStorageBarrel> barrells, int myid,
            RMIIndexStorageBarrel mybarrel) throws RemoteException {
        System.out.println("Registrando ibs");

        barrells.forEach((barid, barr) -> {
            try {
                if (barid != this.barrelId) {
                    this.registeroneIBS(barid, barr);
                    barr.registeroneIBS(myid, mybarrel);
                    barr.gatewaypong("Barrel" + myid);
                    System.out.println("Registrada na barrel " + barid);
                }
            } catch (RemoteException e) {
                System.err.println("Falha ao registrar na barrel " + barid + "!");
                e.printStackTrace();
            }
        });
    }

    public void gatewaypong(String provider) throws RemoteException {
        System.out.println(provider + ":Pong");
    }

    @Override
    public void storeSiteData(SiteData siteData) throws RemoteException {
        System.out.println("Recebendo SiteData para indexação: " + siteData.url);

        // Indexar tokens
        if (siteData.tokens != null && !siteData.tokens.isEmpty()) {
            indexTokens(siteData.tokens, siteData.url);
        }

        // Processar links e atualizar contagem de referências
        if (siteData.links != null && !siteData.links.isEmpty()) {
            String[] links = siteData.links.split("\\s+");
            for (String link : links) {
                urlReferences.put(link, urlReferences.getOrDefault(link, 0) + 1);
            }
        }
        // Processar links e atualizar contagem de referências
        if (siteData.links != null && !siteData.links.isEmpty()) {
            String[] links = siteData.links.split("\\s+");
            for (String link : links) {
                urlReferences.put(link, urlReferences.getOrDefault(link, 0) + 1);

                // Guardar quem está a apontar para o link
                incomingLinks.computeIfAbsent(link, k -> new ArrayList<>()).add(siteData.url);
            }
        }

        System.out.println("Indexação concluída para URL: " + siteData.url);
        saveState("data/estado_barrel_" + barrelId + ".json");

    }

    private void indexTokens(String tokens, String url) {
        String[] tokenArray = tokens.split("\\s+");
        for (String token : tokenArray) {
            token = token.toLowerCase().replaceAll("[^a-z0-9]", ""); // Normaliza tokens
            if (token.isEmpty())
                continue;

            invertedIndex.computeIfAbsent(token, k -> new HashSet<>()).add(url);
        }
    }

    // Métodos adicionais para consulta
    public List<String> searchPagesByWords(Set<String> words) {
        List<String> result = new ArrayList<>();
        Map<String, Integer> pageMatchCount = new HashMap<>();

        for (String word : words) {
            Set<String> pages = invertedIndex.get(word.toLowerCase());
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
        return result;
    }

    public int getUrlReferenceCount(String url) {
        return urlReferences.getOrDefault(url, 0);
    }

    public List<Map.Entry<String, Integer>> getPagesOrderedByIncomingLinks() throws RemoteException {
        List<Map.Entry<String, Integer>> sortedPages = new ArrayList<>(urlReferences.entrySet());
        sortedPages.sort((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue()));
        return sortedPages;
    }

    public List<String> getPagesLinkingTo(String url) {
        return incomingLinks.getOrDefault(url, new ArrayList<>());
    }

    public void saveState(String caminhoArquivo) {
        try {
            Map<String, Object> estado = new HashMap<>();
            estado.put("invertedIndex", invertedIndex);
            estado.put("urlReferences", urlReferences);
            estado.put("urlTexts", urlTexts);
            estado.put("incomingLinks", incomingLinks);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(estado);

            try (FileWriter writer = new FileWriter(caminhoArquivo)) {
                writer.write(json);
            }

            System.out.println("Estado salvo com sucesso no ficheiro JSON: " + caminhoArquivo);

        } catch (Exception e) {
            System.err.println("Erro ao salvar estado no ficheiro JSON:");
            e.printStackTrace();
        }
    }

    public void carregarEstadoDeJSON(String caminhoArquivo) {
        File file = new File(caminhoArquivo);
        if (!file.exists()) {
            System.out.println("Nenhum estado salvo encontrado para esta barrel.");
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            Map<String, Object> estado = gson.fromJson(reader, new TypeToken<Map<String, Object>>() {
            }.getType());

            // Restaurar cada mapa
            urlReferences = gson.fromJson(gson.toJson(estado.get("urlReferences")),
                    new TypeToken<Map<String, Integer>>() {
                    }.getType());

            invertedIndex = gson.fromJson(gson.toJson(estado.get("invertedIndex")),
                    new TypeToken<Map<String, Set<String>>>() {
                    }.getType());

            System.out.println("Estado carregado com sucesso do ficheiro: " + caminhoArquivo);

        } catch (Exception e) {
            System.err.println("Erro ao carregar JSON:");
            e.printStackTrace();
        }
    }

    /**
     * Retorna o URL e os URLs que o referenciaram (links de entrada).
     */
    public List<String> getIncomingLinksForUrl(String url) {
        List<String> referenciadores = incomingLinks.getOrDefault(url, new ArrayList<>());
        return referenciadores;
    }

    public static void main(String[] args) {
        String registryNibs;

        try {
            IndexStorageBarrel barrel = new IndexStorageBarrel(Integer.parseInt(args[0]));
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            System.out.println("Carregando arquivo de propriedades");
            prop.load(input);
            System.out.println("Arquivo de propriedades carregado");
            registryNibs = prop.getProperty("registryNibs");
            System.out.println("registryN: " + registryNibs);
            RMIGatewayIBSDownloader gateway = null;

            try {
                gateway = (RMIGatewayIBSDownloader) Naming.lookup(registryNibs);
                gateway.registerIBS(barrel.barrelId, barrel);

            } catch (RemoteException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
