package meta2sd.googol.sd.uc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import meta2sd.googol.sd.uc.controller.model.WebClient;
import meta2sd.googol.sd.uc.service.HackerNewsService;
import com.google.gson.JsonObject;
import java.util.List;
import meta1sd.SiteData;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import meta2sd.googol.sd.uc.service.GeminiService;

/**
 * Controlador responsável por gerenciar as páginas e requisições da aplicação
 * web.
 * Implementa as rotas para busca, indexação e visualização de resultados.
 * 
 * @author Bernardo Pedro nº2021231014 e João Matos nº2021222748
 * @version 1.0
 */
@Controller
public class PageController {

    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    @Autowired
    private WebClient client;

    @Autowired
    private HackerNewsService hackerNewsService;

    @Autowired
    private GeminiService geminiService;

    /**
     * Exibe a página inicial da aplicação.
     * 
     * @return Nome da view da página inicial
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }

    /**
     * Exibe a página inicial da aplicação (rota alternativa).
     * 
     * @return Nome da view da página inicial
     */
    @GetMapping("/index")
    public String index() {
        return "index";
    }

    /**
     * Processa a opção selecionada no menu principal e redireciona para a página
     * apropriada.
     * 
     * @param option Opção selecionada pelo usuário (0: indexar URL, 1: buscar
     *               termos, 2: buscar links, 3: buscar Hacker News)
     * @param model  Modelo para a view
     * @return Redirecionamento para a página correspondente à opção selecionada
     */
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

    /**
     * Exibe a página de adição de URL para indexação.
     * 
     * @return Nome da view da página de adição de URL
     */
    @GetMapping("/add-url")
    public String addUrl() {
        return "add-url";
    }

    /**
     * Processa a adição de uma nova URL para indexação.
     * 
     * @param url   URL a ser indexada
     * @param model Modelo para a view
     * @return Nome da view da página de adição de URL com mensagem de sucesso/erro
     */
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

    /**
     * Exibe a página de busca com base no tipo de busca selecionado.
     * 
     * @param type  Tipo de busca (termos ou URL)
     * @param model Modelo para a view
     * @return Nome da view da página de busca
     */
    @GetMapping("/search-page")
    public String searchPage(@RequestParam(value = "type", required = false) String type, Model model) {
        model.addAttribute("searchType", type);
        return "search-page";
    }

    /**
     * Processa a busca por termos e exibe os resultados na primeira página.
     * 
     * @param terms Termos de busca
     * @param model Modelo para a view
     * @return Nome da view da página de resultados
     */
    @PostMapping("/search")
    public String search(@RequestParam("terms") String terms, Model model) {
        logger.info("Searching for terms: {}", terms);
        List<SiteData> results = client.getPagesbyTerms(terms);
        model.addAttribute("searchTerms", terms);

        if (results != null) {
            // Remove resultados nulos e garante que os campos necessários não sejam nulos
            results.removeIf(Objects::isNull);
            results.forEach(result -> {
                if (result.title == null)
                    result.title = "";
                if (result.text == null)
                    result.text = "";
                if (result.url == null)
                    result.url = "";
            });

            int pageSize = 10;
            int totalPages = (int) Math.ceil((double) results.size() / pageSize);
            int currentPage = 1;

            // Calcular índices para a primeira página
            int startIndex = 0;
            int endIndex = Math.min(pageSize, results.size());

            // Obter resultados da página
            List<SiteData> pageResults = results.subList(startIndex, endIndex);

            model.addAttribute("results", pageResults);
            model.addAttribute("totalResults", results.size());
            model.addAttribute("pageSize", pageSize);
            model.addAttribute("currentPage", currentPage);
            model.addAttribute("totalPages", totalPages);

            if (results.isEmpty()) {
                model.addAttribute("message", "No results found for your search terms.");
                model.addAttribute("messageType", "info");
            } else {
                // Gerar análise com Gemini apenas na primeira página
                StringBuilder searchResultsText = new StringBuilder();
                for (SiteData result : results) {
                    searchResultsText.append("Título: ").append(result.title).append("\n");
                    searchResultsText.append("URL: ").append(result.url).append("\n");
                    searchResultsText.append("Texto: ").append(result.text).append("\n\n");
                }
                String analysis = geminiService.generateAnalysis(terms, searchResultsText.toString());
                logger.info("Generated analysis: {}", analysis);
                model.addAttribute("analysis", analysis);
            }
        } else {
            model.addAttribute("message", "An error occurred while searching. Please try again.");
            model.addAttribute("messageType", "error");
        }
        return "search-results";
    }

    /**
     * Processa a busca por páginas que linkam para uma URL específica.
     * 
     * @param url   URL para buscar páginas que a referenciam
     * @param model Modelo para a view
     * @return Nome da view da página de resultados
     */
    @PostMapping("/search-links")
    public String searchLinks(@RequestParam("url") String url, Model model) {
        logger.info("Searching for pages linking to: {}", url);
        List<String> results = client.getPagesbyUrl(url);
        model.addAttribute("searchUrl", url);

        if (results != null) {
            // Remove resultados nulos
            results.removeIf(Objects::isNull);
            int pageSize = 10;
            int totalPages = (int) Math.ceil((double) results.size() / pageSize);
            int currentPage = 1;

            // Calcular índices para a primeira página
            int startIndex = 0;
            int endIndex = Math.min(pageSize, results.size());

            // Obter resultados da página
            List<String> pageResults = results.subList(startIndex, endIndex);

            model.addAttribute("results", pageResults);
            model.addAttribute("totalResults", results.size());
            model.addAttribute("pageSize", pageSize);
            model.addAttribute("currentPage", currentPage);
            model.addAttribute("totalPages", totalPages);

            if (results.isEmpty()) {
                model.addAttribute("message", "No pages found linking to this URL.");
                model.addAttribute("messageType", "info");
            } else {
                // Gerar análise com Gemini apenas na primeira página
                StringBuilder searchResultsText = new StringBuilder();
                for (String result : results) {
                    searchResultsText.append("URL: ").append(result).append("\n");
                }
                String analysis = geminiService.generateAnalysis("Páginas que linkam para: " + url,
                        searchResultsText.toString());
                logger.info("Generated analysis: {}", analysis);
                model.addAttribute("analysis", analysis);
            }
        } else {
            model.addAttribute("message", "An error occurred while searching for linked pages. Please try again.");
            model.addAttribute("messageType", "error");
        }
        return "search-results";
    }

    /**
     * Exibe os resultados da busca com paginação.
     * A análise do Gemini é gerada apenas na primeira página.
     * 
     * @param terms Termos de busca (opcional)
     * @param url   URL para buscar páginas que a referenciam (opcional)
     * @param page  Número da página atual
     * @param model Modelo para a view
     * @return Nome da view da página de resultados
     */
    @GetMapping("/search-results")
    public String searchResults(
            @RequestParam(value = "terms", required = false) String terms,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model) {

        if (terms != null && !terms.isEmpty()) {
            logger.info("Searching for terms: {} (page {})", terms, page);
            List<SiteData> results = client.getPagesbyTerms(terms);
            model.addAttribute("searchTerms", terms);

            if (results != null) {
                // Remove resultados nulos e garante que os campos necessários não sejam nulos
                results.removeIf(Objects::isNull);
                results.forEach(result -> {
                    if (result.title == null)
                        result.title = "";
                    if (result.text == null)
                        result.text = "";
                    if (result.url == null)
                        result.url = "";
                });

                int pageSize = 10;
                int totalPages = (int) Math.ceil((double) results.size() / pageSize);

                page = Math.max(1, Math.min(page, totalPages));

                int startIndex = (page - 1) * pageSize;
                int endIndex = Math.min(startIndex + pageSize, results.size());

                List<SiteData> pageResults = results.subList(startIndex, endIndex);

                model.addAttribute("results", pageResults);
                model.addAttribute("totalResults", results.size());
                model.addAttribute("pageSize", pageSize);
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", totalPages);

                if (results.isEmpty()) {
                    model.addAttribute("message", "No results found for your search terms.");
                    model.addAttribute("messageType", "info");
                } else if (page == 1) {
                    // Gerar análise com Gemini apenas na primeira página
                    StringBuilder searchResultsText = new StringBuilder();
                    for (SiteData result : results) {
                        searchResultsText.append("Título: ").append(result.title).append("\n");
                        searchResultsText.append("URL: ").append(result.url).append("\n");
                        searchResultsText.append("Texto: ").append(result.text).append("\n\n");
                    }
                    String analysis = geminiService.generateAnalysis(terms, searchResultsText.toString());
                    logger.info("Generated analysis: {}", analysis);
                    model.addAttribute("analysis", analysis);
                }
            } else {
                model.addAttribute("message", "An error occurred while searching. Please try again.");
                model.addAttribute("messageType", "error");
            }
        } else if (url != null && !url.isEmpty()) {
            logger.info("Searching for pages linking to: {} (page {})", url, page);
            List<String> results = client.getPagesbyUrl(url);
            model.addAttribute("searchUrl", url);

            if (results != null) {
                // Remove resultados nulos
                results.removeIf(Objects::isNull);
                int pageSize = 10;
                int totalPages = (int) Math.ceil((double) results.size() / pageSize);

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
                    model.addAttribute("message", "No pages found linking to this URL.");
                    model.addAttribute("messageType", "info");
                } else if (page == 1) {
                    // Gerar análise com Gemini apenas na primeira página
                    StringBuilder searchResultsText = new StringBuilder();
                    for (String result : results) {
                        searchResultsText.append("URL: ").append(result).append("\n");
                    }
                    String analysis = geminiService.generateAnalysis("Páginas que linkam para: " + url,
                            searchResultsText.toString());
                    logger.info("Generated analysis: {}", analysis);
                    model.addAttribute("analysis", analysis);
                }
            } else {
                model.addAttribute("message", "An error occurred while searching for linked pages. Please try again.");
                model.addAttribute("messageType", "error");
            }
        }

        return "search-results";
    }

    /**
     * Exibe a página de busca do Hacker News e processa as buscas.
     * 
     * @param query Termos de busca (opcional)
     * @param page  Número da página atual
     * @param model Modelo para a view
     * @return Nome da view da página de busca do Hacker News
     */
    @GetMapping("/hacker-news-search")
    public String hackerNewsSearch(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        if (query != null && !query.trim().isEmpty()) {
            try {
                List<JsonObject> results = hackerNewsService.searchStories(query, page);
                int totalResults = hackerNewsService.getTotalResults(query);
                int totalPages = (int) Math.ceil((double) totalResults / 10);

                model.addAttribute("results", results);
                model.addAttribute("query", query);
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", totalPages);
                model.addAttribute("hasNextPage", page < totalPages);
                model.addAttribute("hasPreviousPage", page > 1);
            } catch (Exception e) {
                logger.error("Error searching Hacker News: {}", e.getMessage());
                model.addAttribute("message", "An error occurred while searching Hacker News. Please try again.");
                model.addAttribute("messageType", "error");
            }
        }
        return "hacker-news-search";
    }
}
