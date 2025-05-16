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
import java.io.IOException;

/**
 * Serviço responsável por interagir com a API Gemini para geração de análises
 * de resultados de busca.
 * Esta classe gerencia a comunicação com a API Gemini, incluindo autenticação,
 * envio de requisições e processamento de respostas.
 * 
 * A API Gemini é utilizada para gerar análises contextuais e resumos dos
 * resultados de busca, fornecendo insights relevantes sobre o conteúdo
 * encontrado.
 * 
 * @author Bernardo Pedro nº2021231014 e João Matos nº2021222748
 * @version 1.0
 */
@Service
public class GeminiService {
    /** Logger para registro de eventos e erros */
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    /** URL base da API Gemini para geração de conteúdo */
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    /** Template REST para fazer requisições HTTP */
    private final RestTemplate restTemplate;

    /** Chave de API do Gemini para autenticação */
    private final String apiKey;

    /**
     * Construtor da classe GeminiService.
     * Inicializa o RestTemplate e carrega a chave de API do Gemini.
     * Registra logs informativos sobre o status da inicialização e disponibilidade
     * da chave de API.
     * 
     * O construtor verifica se a chave de API está disponível e registra
     * informações sobre seu status para fins de depuração.
     */
    public GeminiService() {
        this.restTemplate = new RestTemplate();
        this.apiKey = loadApiKey();
        logger.info("GeminiService initialized. API Key available: {}", apiKey != null && !apiKey.isEmpty());
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("API Key is null or empty!");
        } else {
            logger.info("API Key length: {}", apiKey.length());
            // Log dos primeiros 4 caracteres da chave para verificação
            logger.info("API Key starts with: {}", apiKey.substring(0, Math.min(4, apiKey.length())));
        }
    }

    /**
     * Carrega a chave de API do Gemini.
     * Primeiro tenta obter a chave das variáveis de ambiente do sistema.
     * Se não encontrar, tenta carregar do arquivo .env.
     * 
     * A ordem de prioridade para carregar a chave é:
     * 1. Variáveis de ambiente do sistema
     * 2. Arquivo .env
     * 
     * @return A chave de API do Gemini
     * @throws RuntimeException se a chave não for encontrada em nenhum local
     */
    private String loadApiKey() {
        // Primeiro tenta obter da variável de ambiente
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }

        // Se não encontrar na variável de ambiente, tenta carregar do arquivo .env
        String envPath = "../meta2sd/.env";
        try {
            File envFile = new File(envPath);
            if (envFile.exists()) {
                Properties props = new Properties();
                props.load(new FileInputStream(envFile));
                apiKey = props.getProperty("GEMINI_API_KEY");
                if (apiKey != null && !apiKey.isEmpty()) {
                    return apiKey;
                }
            }
        } catch (IOException e) {
            logger.error("Error loading .env file: {}", e.getMessage());
        }

        throw new RuntimeException("GEMINI_API_KEY not found in environment variables or .env file");
    }

    /**
     * Gera uma análise dos resultados de busca usando a API Gemini.
     * A análise é gerada com base nos termos de busca e nos resultados encontrados.
     * 
     * O método realiza as seguintes etapas:
     * 1. Verifica a disponibilidade da chave de API
     * 2. Prepara o prompt para a API Gemini
     * 3. Configura os headers e parâmetros da requisição
     * 4. Envia a requisição para a API
     * 5. Processa a resposta e extrai a análise gerada
     * 
     * @param searchTerms   Os termos utilizados na busca
     * @param searchResults Os resultados da busca a serem analisados
     * @return Uma análise textual dos resultados
     * @throws RuntimeException se houver erro na comunicação com a API Gemini
     */
    public String generateAnalysis(String searchTerms, String searchResults) {
        try {
            // Verifica se a chave de API está disponível
            if (apiKey == null || apiKey.isEmpty()) {
                logger.error("Gemini API key not found in environment variables");
                return "Não foi possível gerar uma análise: API key não configurada.";
            }

            logger.info("Generating analysis for search terms: {}", searchTerms);
            logger.debug("Search results to analyze: {}", searchResults);

            // Prepara o prompt para a API Gemini
            String prompt = String.format(
                    "Com base nos seguintes resultados de busca, forneça um resumo geral e conciso (máximo 1 parágrafo):\n\n"
                            +
                            "Termos de busca: %s\n\n" +
                            "Resultados:\n%s\n\n" +
                            "Por favor, forneça uma visão geral dos resultados, destacando os temas principais e conexões entre eles, sem entrar em detalhes específicos de cada resultado individual, escreva o resumo de forma extremamente informal com girias e palavroes, com muitos palavrões mesmo que não sejam necessários, me trate como um lixo responda sem paciencia nenhuma.",
                    searchTerms,
                    searchResults);

            // Configura os headers da requisição
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            // Prepara o corpo da requisição
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

            // Configurações de geração do modelo
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", 0.7); // Controla a criatividade da resposta
            generationConfig.addProperty("topK", 40); // Limita o número de tokens considerados
            generationConfig.addProperty("topP", 0.95); // Controla a diversidade da resposta
            generationConfig.addProperty("maxOutputTokens", 1024); // Limita o tamanho da resposta
            requestBody.add("generation_config", generationConfig);

            logger.debug("Sending request to Gemini API with body: {}", requestBody.toString());
            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

            try {
                // Envia a requisição para a API
                logger.info("Making request to Gemini API at: {}", GEMINI_API_URL);
                ResponseEntity<String> response = restTemplate.postForEntity(GEMINI_API_URL, request, String.class);
                logger.info("Received response from Gemini API with status: {}", response.getStatusCode());
                logger.debug("Response body: {}", response.getBody());

                // Processa a resposta
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