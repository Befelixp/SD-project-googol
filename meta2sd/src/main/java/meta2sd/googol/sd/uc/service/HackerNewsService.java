package meta2sd.googol.sd.uc.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class HackerNewsService {
    private static final Logger logger = LoggerFactory.getLogger(HackerNewsService.class);
    private static final String HN_API_BASE = "https://hacker-news.firebaseio.com/v0";
    private static final int STORIES_PER_PAGE = 10;
    private final RestTemplate restTemplate;

    public HackerNewsService() {
        this.restTemplate = new RestTemplate();
    }

    public List<JsonObject> searchStories(String query, int page) {
        try {
            // Get top stories
            String topStoriesUrl = HN_API_BASE + "/topstories.json";
            String response = restTemplate.getForObject(topStoriesUrl, String.class);
            JsonArray storyIds = JsonParser.parseString(response).getAsJsonArray();

            List<JsonObject> results = new ArrayList<>();
            int startIndex = (page - 1) * STORIES_PER_PAGE;
            int endIndex = Math.min(startIndex + STORIES_PER_PAGE, storyIds.size());

            for (int i = startIndex; i < endIndex; i++) {
                int storyId = storyIds.get(i).getAsInt();
                String storyUrl = HN_API_BASE + "/item/" + storyId + ".json";
                String storyResponse = restTemplate.getForObject(storyUrl, String.class);
                JsonObject story = JsonParser.parseString(storyResponse).getAsJsonObject();

                // Check if story title contains the search query
                if (story.has("title")
                        && story.get("title").getAsString().toLowerCase().contains(query.toLowerCase())) {
                    results.add(story);
                }
            }

            return results;
        } catch (Exception e) {
            logger.error("Error searching Hacker News: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public int getTotalResults(String query) {
        try {
            String topStoriesUrl = HN_API_BASE + "/topstories.json";
            String response = restTemplate.getForObject(topStoriesUrl, String.class);
            JsonArray storyIds = JsonParser.parseString(response).getAsJsonArray();

            int count = 0;
            for (int i = 0; i < storyIds.size(); i++) {
                int storyId = storyIds.get(i).getAsInt();
                String storyUrl = HN_API_BASE + "/item/" + storyId + ".json";
                String storyResponse = restTemplate.getForObject(storyUrl, String.class);
                JsonObject story = JsonParser.parseString(storyResponse).getAsJsonObject();

                if (story.has("title")
                        && story.get("title").getAsString().toLowerCase().contains(query.toLowerCase())) {
                    count++;
                }
            }

            return count;
        } catch (Exception e) {
            logger.error("Error getting total results from Hacker News: {}", e.getMessage());
            return 0;
        }
    }
}