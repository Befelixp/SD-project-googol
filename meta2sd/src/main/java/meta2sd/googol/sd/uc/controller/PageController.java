package meta2sd.googol.sd.uc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import meta2sd.googol.sd.uc.controller.model.WebClient;
import meta2sd.googol.sd.uc.service.HackerNewsService;
import com.google.gson.JsonObject;
import java.util.List;

@Controller
public class PageController {

    @Autowired
    private WebClient client;

    @Autowired
    private HackerNewsService hackerNewsService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/index")
    public String index() {
        return "index";
    }

    @PostMapping("/index")
    public String processIndex(@RequestParam("option") int option, Model model) {
        switch (option) {
            case 0:
                return "redirect:/add-url";
            case 1:
                return "redirect:/search-page";
            case 2:
                return "redirect:/search-page?type=url";
            case 3:
                return "redirect:/hacker-news-search";
            default:
                return "redirect:/";
        }
    }

    @GetMapping("/add-url")
    public String addUrl() {
        return "add-url";
    }

    @PostMapping("/add-url")
    public String processAddUrl(@RequestParam("url") String url, Model model) {
        boolean success = client.addURL(url);
        if (success) {
            model.addAttribute("message", "URL successfully submitted for indexing!");
            model.addAttribute("messageType", "success");
        } else {
            model.addAttribute("message", "Failed to submit URL for indexing. Please try again.");
            model.addAttribute("messageType", "error");
        }
        return "add-url";
    }

    @GetMapping("/search-page")
    public String searchPage(@RequestParam(value = "type", required = false) String type, Model model) {
        model.addAttribute("searchType", type);
        return "search-page";
    }

    @PostMapping("/search")
    public String search(@RequestParam("terms") String terms, Model model) {
        List<String> results = client.getPagesbyTerms(terms);
        model.addAttribute("searchTerms", terms);

        if (results != null) {
            model.addAttribute("results", results);
            model.addAttribute("totalResults", results.size());
            model.addAttribute("pageSize", 10);
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", (int) Math.ceil((double) results.size() / 10));

            if (results.isEmpty()) {
                model.addAttribute("message", "No results found for your search terms.");
                model.addAttribute("messageType", "info");
            }
        } else {
            model.addAttribute("message", "An error occurred while searching. Please try again.");
            model.addAttribute("messageType", "error");
        }
        return "search-results";
    }

    @PostMapping("/search-links")
    public String searchLinks(@RequestParam("url") String url, Model model) {
        List<String> results = client.getPagesbyUrl(url);
        model.addAttribute("searchUrl", url);

        if (results != null) {
            model.addAttribute("results", results);
            model.addAttribute("totalResults", results.size());
            model.addAttribute("pageSize", 10);
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", (int) Math.ceil((double) results.size() / 10));

            if (results.isEmpty()) {
                model.addAttribute("message", "No pages found linking to this URL.");
                model.addAttribute("messageType", "info");
            }
        } else {
            model.addAttribute("message", "An error occurred while searching for linked pages. Please try again.");
            model.addAttribute("messageType", "error");
        }
        return "search-results";
    }

    @GetMapping("/search-results")
    public String searchResults(
            @RequestParam(value = "terms", required = false) String terms,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model) {

        List<String> results;
        if (terms != null) {
            results = client.getPagesbyTerms(terms);
            model.addAttribute("searchTerms", terms);
        } else if (url != null) {
            results = client.getPagesbyUrl(url);
            model.addAttribute("searchUrl", url);
        } else {
            return "redirect:/search-page";
        }

        if (results != null) {
            int pageSize = 10;
            int totalPages = (int) Math.ceil((double) results.size() / pageSize);

            // Ensure page is within valid range
            page = Math.max(1, Math.min(page, totalPages));

            int startIndex = (page - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, results.size());

            List<String> pageResults = results.subList(startIndex, endIndex);

            model.addAttribute("results", pageResults);
            model.addAttribute("totalResults", results.size());
            model.addAttribute("pageSize", pageSize);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);

            if (results.isEmpty()) {
                model.addAttribute("message", terms != null ? "No results found for your search terms."
                        : "No pages found linking to this URL.");
                model.addAttribute("messageType", "info");
            }
        } else {
            model.addAttribute("message", "An error occurred while searching. Please try again.");
            model.addAttribute("messageType", "error");
        }
        return "search-results";
    }

    @GetMapping("/hacker-news-search")
    public String hackerNewsSearch(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        try {
            if (query != null && !query.trim().isEmpty()) {
                List<JsonObject> results = hackerNewsService.searchStories(query, page);
                int totalResults = hackerNewsService.getTotalResults(query);
                int totalPages = (int) Math.ceil((double) totalResults / 10);

                model.addAttribute("results", results);
                model.addAttribute("currentPage", page);
                model.addAttribute("query", query);
                model.addAttribute("totalPages", totalPages);
                model.addAttribute("hasNextPage", page < totalPages);
                model.addAttribute("hasPreviousPage", page > 1);
            }
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while searching Hacker News. Please try again later.");
            e.printStackTrace();
        }

        return "hacker-news-search";
    }
}
