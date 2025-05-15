package meta2sd.googol.sd.uc.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.util.Properties;
import java.io.File;

@Service
public class GeminiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public GeminiService() {
        this.restTemplate = new RestTemplate();
        this.apiKey = loadApiKey();
        logger.info("GeminiService initialized. API Key available: {}", apiKey != null && !apiKey.isEmpty());
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("API Key is null or empty!");
        } else {
            logger.info("API Key length: {}", apiKey.length());
            // Log first 4 characters of API key for verification
            logger.info("API Key starts with: {}", apiKey.substring(0, Math.min(4, apiKey.length())));
        }
    }

    private String loadApiKey() {
        try {
            String[] possiblePaths = {
                    ".env",
                    "meta2sd/.env",
                    "../meta2sd/.env",
                    "../../meta2sd/.env",
                    "../../../meta2sd/.env",
                    "../../../../meta2sd/.env",
                    "../../../../../../meta2sd/.env"
            };

            for (String path : possiblePaths) {
                File envFile = new File(path);
                logger.debug("Checking for .env file at: {}", envFile.getAbsolutePath());
                if (envFile.exists()) {
                    logger.info("Found .env file at: {}", envFile.getAbsolutePath());
                    Properties props = new Properties();
                    props.load(new FileInputStream(envFile));
                    String key = props.getProperty("GEMINI_API_KEY");
                    if (key == null || key.isEmpty()) {
                        logger.error("GEMINI_API_KEY not found in .env file at: {}", envFile.getAbsolutePath());
                    } else {
                        logger.info("Successfully loaded GEMINI_API_KEY from: {}", envFile.getAbsolutePath());
                        return key;
                    }
                }
            }

            logger.error("No .env file found in any of the possible paths");
            return "";
        } catch (Exception e) {
            logger.error("Error loading GEMINI_API_KEY from .env file: {}", e.getMessage(), e);
            return "";
        }
    }

    public String generateAnalysis(String searchTerms, String searchResults) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                logger.error("Gemini API key not found in environment variables");
                return "Não foi possível gerar uma análise: API key não configurada.";
            }

            logger.info("Generating analysis for search terms: {}", searchTerms);
            logger.debug("Search results to analyze: {}", searchResults);

            String prompt = String.format(
                    "Com base nos seguintes resultados de busca, forneça um resumo geral e conciso (máximo 1 parágrafo):\n\n"
                            +
                            "Termos de busca: %s\n\n" +
                            "Resultados:\n%s\n\n" +
                            "Por favor, forneça uma visão geral dos resultados, destacando os temas principais e conexões entre eles, sem entrar em detalhes específicos de cada resultado individual, escreva o resumo de forma extremamente informal com girias e palavroes, com muitos palavrões mesmo que não sejam necessários, me trate como um lixo responda sem paciencia nenhuma.",
                    searchTerms,
                    searchResults);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            // Add safety settings
            JsonObject safetySettings = new JsonObject();
            safetySettings.addProperty("category", "HARM_CATEGORY_HARASSMENT");
            safetySettings.addProperty("threshold", "BLOCK_MEDIUM_AND_ABOVE");
            JsonArray safetySettingsArray = new JsonArray();
            safetySettingsArray.add(safetySettings);
            requestBody.add("safety_settings", safetySettingsArray);

            // Add generation config
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", 0.7);
            generationConfig.addProperty("topK", 40);
            generationConfig.addProperty("topP", 0.95);
            generationConfig.addProperty("maxOutputTokens", 1024);
            requestBody.add("generation_config", generationConfig);

            logger.debug("Sending request to Gemini API with body: {}", requestBody.toString());
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

            try {
                logger.info("Making request to Gemini API at: {}", GEMINI_API_URL);
                ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_API_URL, request, String.class);
                logger.info("Received response from Gemini API with status: {}", response.getStatusCode());
                logger.debug("Response body: {}", response.getBody());

                if (response.getStatusCode().is2xxSuccessful()) {
                    JsonObject responseJson = JsonParser.parseString(response.getBody()).getAsJsonObject();
                    JsonArray candidates = responseJson.getAsJsonArray("candidates");
                    if (candidates != null && candidates.size() > 0) {
                        JsonObject candidate = candidates.get(0).getAsJsonObject();
                        JsonObject content2 = candidate.getAsJsonObject("content");
                        JsonArray parts2 = content2.getAsJsonArray("parts");
                        if (parts2 != null && parts2.size() > 0) {
                            String analysis = parts2.get(0).getAsJsonObject().get("text").getAsString();
                            logger.info("Successfully generated analysis");
                            return analysis;
                        }
                    }
                    logger.warn("No analysis generated from Gemini API response. Response body: {}",
                            response.getBody());
                } else {
                    logger.error("Gemini API returned non-2xx status code: {}", response.getStatusCode());
                    logger.error("Response body: {}", response.getBody());
                }
            } catch (Exception e) {
                logger.error("Error calling Gemini API: {}", e.getMessage(), e);
                if (e.getCause() != null) {
                    logger.error("Caused by: {}", e.getCause().getMessage());
                }
                throw e;
            }

            return "Não foi possível gerar uma análise no momento.";
        } catch (Exception e) {
            logger.error("Error generating analysis with Gemini: {}", e.getMessage(), e);
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getMessage());
            }
            return "Ocorreu um erro ao gerar a análise.";
        }
    }
}