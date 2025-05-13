package meta2sd.googol.sd.uc.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class HackerNewsService {
    private static final int STORIES_PER_PAGE = 10;
    private static final String TOP_STORIES_URL = "https://hacker-news.firebaseio.com/v0/topstories.json";
    private static final String ITEM_URL = "https://hacker-news.firebaseio.com/v0/item/%d.json";
    private final Gson gson = new Gson();
    private final Map<String, List<JsonObject>> searchCache = new ConcurrentHashMap<>();
    private final Map<Integer, JsonObject> storyCache = new ConcurrentHashMap<>();
    private int[] cachedStoryIds;
    private long lastCacheUpdate;

    public List<JsonObject> searchStories(String query, int page) {
        try {
            // Update cache if needed (every 5 minutes)
            updateCacheIfNeeded();

            // Get matching stories from cache
            List<JsonObject> matchingStories = searchCache.computeIfAbsent(query.toLowerCase(),
                    this::findMatchingStories);

            // Calculate pagination
            int startIndex = (page - 1) * STORIES_PER_PAGE;
            int endIndex = Math.min(startIndex + STORIES_PER_PAGE, matchingStories.size());

            // Return the stories for the current page
            if (startIndex < matchingStories.size()) {
                return matchingStories.subList(startIndex, endIndex);
            }

            return new ArrayList<>();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public int getTotalResults(String query) {
        try {
            // Update cache if needed
            updateCacheIfNeeded();

            // Get matching stories from cache
            List<JsonObject> matchingStories = searchCache.computeIfAbsent(query.toLowerCase(),
                    this::findMatchingStories);
            return matchingStories.size();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void updateCacheIfNeeded() throws Exception {
        long currentTime = System.currentTimeMillis();
        if (cachedStoryIds == null || currentTime - lastCacheUpdate > 300000) { // 5 minutes
            String topStoriesJson = fetchUrl(TOP_STORIES_URL);
            cachedStoryIds = gson.fromJson(topStoriesJson, int[].class);
            storyCache.clear();
            searchCache.clear();
            lastCacheUpdate = currentTime;
        }
    }

    private List<JsonObject> findMatchingStories(String query) {
        String[] terms = query.split("\\s+");
        return Arrays.stream(cachedStoryIds)
                .parallel()
                .mapToObj(this::getStoryFromCache)
                .filter(Objects::nonNull)
                .filter(story -> matchesTerms(story, terms))
                .collect(Collectors.toList());
    }

    private JsonObject getStoryFromCache(int id) {
        return storyCache.computeIfAbsent(id, this::fetchStory);
    }

    private JsonObject fetchStory(int id) {
        try {
            String storyJson = fetchUrl(String.format(ITEM_URL, id));
            return gson.fromJson(storyJson, JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

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