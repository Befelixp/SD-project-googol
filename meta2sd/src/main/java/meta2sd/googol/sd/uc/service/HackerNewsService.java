package meta2sd.googol.sd.uc.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Serviço responsável por interagir com a API do Hacker News.
 * Esta classe gerencia a busca e cache de histórias do Hacker News,
 * incluindo paginação e atualização periódica do cache.
 * 
 * @author Bernardo Pedro nº2021231014 e João Matos nº2021222748
 * @version 1.0
 */
@Service
public class HackerNewsService {
    /** Número de histórias por página */
    private static final int STORIES_PER_PAGE = 10;

    /** URL para obter as histórias mais populares */
    private static final String TOP_STORIES_URL = "https://hacker-news.firebaseio.com/v0/topstories.json";

    /** URL base para obter detalhes de uma história específica */
    private static final String ITEM_URL = "https://hacker-news.firebaseio.com/v0/item/%d.json";

    /** Instância do Gson para manipulação de JSON */
    private final Gson gson = new Gson();

    /** Cache de resultados de busca */
    private final Map<String, List<JsonObject>> searchCache = new ConcurrentHashMap<>();

    /** Cache de histórias individuais */
    private final Map<Integer, JsonObject> storyCache = new ConcurrentHashMap<>();

    /** IDs das histórias em cache */
    private int[] cachedStoryIds;

    /** Timestamp da última atualização do cache */
    private long lastCacheUpdate;

    /**
     * Busca histórias do Hacker News com base em uma query e página específica.
     * 
     * @param query Termos de busca
     * @param page  Número da página desejada
     * @return Lista de histórias que correspondem à busca, paginadas
     */
    public List<JsonObject> searchStories(String query, int page) {
        try {
            // Atualiza o cache se necessário (a cada 5 minutos)
            updateCacheIfNeeded();

            // Obtém histórias correspondentes do cache
            List<JsonObject> matchingStories = searchCache.computeIfAbsent(query.toLowerCase(),
                    this::findMatchingStories);

            // Calcula paginação
            int startIndex = (page - 1) * STORIES_PER_PAGE;
            int endIndex = Math.min(startIndex + STORIES_PER_PAGE, matchingStories.size());

            // Retorna as histórias da página atual
            if (startIndex < matchingStories.size()) {
                return matchingStories.subList(startIndex, endIndex);
            }

            return new ArrayList<>();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Obtém o número total de resultados para uma query específica.
     * 
     * @param query Termos de busca
     * @return Número total de histórias que correspondem à busca
     */
    public int getTotalResults(String query) {
        try {
            // Atualiza o cache se necessário
            updateCacheIfNeeded();

            // Obtém histórias correspondentes do cache
            List<JsonObject> matchingStories = searchCache.computeIfAbsent(query.toLowerCase(),
                    this::findMatchingStories);
            return matchingStories.size();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Atualiza o cache de histórias se necessário.
     * O cache é atualizado a cada 5 minutos.
     * 
     * @throws Exception se houver erro ao atualizar o cache
     */
    private void updateCacheIfNeeded() throws Exception {
        long currentTime = System.currentTimeMillis();
        if (cachedStoryIds == null || currentTime - lastCacheUpdate > 300000) { // 5 minutos
            String topStoriesJson = fetchUrl(TOP_STORIES_URL);
            cachedStoryIds = gson.fromJson(topStoriesJson, int[].class);
            storyCache.clear();
            searchCache.clear();
            lastCacheUpdate = currentTime;
        }
    }

    /**
     * Encontra histórias que correspondem aos termos de busca.
     * 
     * @param query Termos de busca
     * @return Lista de histórias que correspondem aos termos
     */
    private List<JsonObject> findMatchingStories(String query) {
        String[] terms = query.split("\\s+");
        return Arrays.stream(cachedStoryIds)
                .parallel()
                .mapToObj(this::getStoryFromCache)
                .filter(Objects::nonNull)
                .filter(story -> matchesTerms(story, terms))
                .collect(Collectors.toList());
    }

    /**
     * Obtém uma história do cache ou busca da API se necessário.
     * 
     * @param id ID da história
     * @return Objeto JSON contendo os detalhes da história
     */
    private JsonObject getStoryFromCache(int id) {
        return storyCache.computeIfAbsent(id, this::fetchStory);
    }

    /**
     * Busca uma história específica da API do Hacker News.
     * 
     * @param id ID da história
     * @return Objeto JSON contendo os detalhes da história, ou null se houver erro
     */
    private JsonObject fetchStory(int id) {
        try {
            String storyJson = fetchUrl(String.format(ITEM_URL, id));
            return gson.fromJson(storyJson, JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Verifica se uma história corresponde aos termos de busca.
     * 
     * @param story História a ser verificada
     * @param terms Termos de busca
     * @return true se a história corresponder a todos os termos, false caso
     *         contrário
     */
    private boolean matchesTerms(JsonObject story, String[] terms) {
        if (story == null || !story.has("title") || !story.has("url")) {
            return false;
        }

        String title = story.get("title").getAsString().toLowerCase();
        for (String term : terms) {
            if (!title.contains(term)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Faz uma requisição HTTP GET para uma URL específica.
     * 
     * @param urlString URL para fazer a requisição
     * @return Resposta da requisição como String
     * @throws Exception se houver erro na requisição
     */
    private String fetchUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}