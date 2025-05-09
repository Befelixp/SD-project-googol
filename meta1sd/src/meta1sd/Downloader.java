package meta1sd;

import org.jsoup.HttpStatusException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * Classe responsável por baixar e processar dados de sites.
 * Conecta-se a um gateway RMI para recuperar URLs, busca seu conteúdo
 * e envia os dados processados para um barrel de armazenamento.
 */
public class Downloader {
    private static final int RETRY_DELAY = 5000; // Atraso em milissegundos antes de tentar reconectar
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private RMIGatewayIBSDownloader gatewayibs; // Gateway RMI para armazenar dados

    /**
     * Construtor padrão da classe Downloader.
     */
    public Downloader() {
        // Construtor simples sem parâmetros
    }

    /**
     * Configura o gateway RMI para armazenamento de dados.
     * 
     * @param gateway O gateway RMI a ser configurado.
     */
    public void setGateway(RMIGatewayIBSDownloader gateway) {
        this.gatewayibs = gateway;
        System.out.println(getTimestamp() + " : Gateway configurado com sucesso");
    }

    /**
     * Obtém o timestamp atual formatado como uma string.
     * 
     * @return O timestamp formatado.
     */
    private String getTimestamp() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    /**
     * Envia os dados do site processados para um barrel de armazenamento.
     * Implementa lógica de repetição em caso de falha.
     * 
     * @param siteData Os dados a serem enviados.
     * @return Verdadeiro se os dados foram enviados com sucesso, falso caso
     *         contrário.
     */
    private boolean sendToBarrels(SiteData siteData) {
        if (gatewayibs == null) {
            System.err.println(getTimestamp() + " : ❌ Erro: Gateway não configurado. Não é possível enviar dados.");
            return false;
        }

        final int MAX_RETRIES = 3; // Número máximo de tentativas
        int waitTime = 500; // Tempo de espera inicial em milissegundos

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Solicita um barrel aleatório do gateway
                System.out.printf(
                        "[%s] 🔄 Tentativa %d de %d: Solicitando barrel aleatória do gateway...%n",
                        getTimestamp(),
                        attempt, MAX_RETRIES);

                RMIIndexStorageBarrel barrel = gatewayibs.getRandomBarrel();

                if (barrel == null) {
                    System.out.printf(
                            "[%s] ⚠️ Nenhuma barrel disponível no momento. Aguardando %dms antes de tentar novamente...%n",
                            getTimestamp(),
                            waitTime);

                    // Aguardar antes de tentar novamente com backoff exponencial
                    Thread.sleep(waitTime);
                    waitTime *= 2; // Dobra o tempo de espera para a próxima tentativa
                    continue;
                }

                // Tenta enviar os dados do site para o barrel obtido
                barrel.storeSiteData(siteData);

                System.out.printf(
                        "[%s] ✅ Sucesso: SiteData enviado para barrel - URL: %s%n",
                        getTimestamp(),
                        siteData.url);

                return true;

            } catch (RemoteException e) {
                System.err.printf(
                        "[%s] ❌ Erro: Falha ao enviar SiteData - URL: %s - Erro: %s%n",
                        getTimestamp(),
                        siteData.url,
                        e.getMessage());

                // A exceção já foi tratada no gateway, que deve ter removido o barrel
                // problemático
                // Apenas aguardamos um pouco para a próxima tentativa
                try {
                    Thread.sleep(waitTime);
                    waitTime *= 2; // Backoff exponencial
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        System.err.printf(
                "[%s] ❌ Falha: Não foi possível enviar SiteData após %d tentativas - URL: %s%n",
                getTimestamp(),
                MAX_RETRIES,
                siteData.url);

        return false;
    }

    /**
     * Método principal para iniciar a aplicação Downloader.
     * Inicializa as propriedades, conecta-se ao gateway RMI
     * e processa URLs em um loop.
     * 
     * @param args Argumentos da linha de comando, esperando um arquivo de
     *             propriedades.
     */
    public static void main(String[] args) {
        String registryN, registryNibs;
        int maxSizeText, maxSizeTitle, maxSizeTokens;

        try {
            // Verifica se pelo menos um argumento (arquivo de propriedades) é fornecido
            if (args.length < 1) {
                System.out.println(LocalDateTime.now() + " : Erro: Arquivo de propriedades não especificado");
                System.out.println("Uso: java meta1sd.Downloader <arquivo_propriedades>");
                return;
            }

            Downloader downloader = new Downloader();
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[0]);

            System.out.println(downloader.getTimestamp() + " : Iniciando Downloader");
            System.out.println(downloader.getTimestamp() + " : Carregando arquivo de propriedades");
            prop.load(input);
            System.out.println(downloader.getTimestamp() + " : Arquivo de propriedades carregado");

            // Carrega as propriedades do arquivo
            registryN = prop.getProperty("registryN");
            registryNibs = prop.getProperty("registryNibs");

            System.out.println(downloader.getTimestamp() + " : registryN: " + registryN);
            System.out.println(downloader.getTimestamp() + " : registryNibs: " + registryNibs);

            maxSizeText = Integer.parseInt(prop.getProperty("maxSizeText"));
            maxSizeTitle = Integer.parseInt(prop.getProperty("maxSizeTitle"));
            maxSizeTokens = Integer.parseInt(prop.getProperty("maxSizeTokens"));

            RMIGatewayDownloaderInterface gateway = null;
            RMIGatewayIBSDownloader gatewayibs = null;

            while (true) { // Loop principal de processamento
                try {
                    // Se o gateway é null, tenta conectar
                    if (gateway == null || gatewayibs == null) {
                        System.out.println(downloader.getTimestamp() + " : Tentando conectar ao RMI...");
                        gateway = (RMIGatewayDownloaderInterface) Naming.lookup(registryN);
                        gatewayibs = (RMIGatewayIBSDownloader) Naming.lookup(registryNibs);

                        // IMPORTANTE: Configurar o gateway no downloader
                        downloader.setGateway(gatewayibs);

                        System.out.println(downloader.getTimestamp() + " : Conexão RMI estabelecida com sucesso");
                    }

                    // Loop de processamento de URLs
                    while (true) {
                        SiteData siteData = new SiteData();
                        try {
                            siteData.url = gateway.popqueue(); // Recupera uma URL da fila
                            System.out.println(downloader.getTimestamp() + " : Tentando pegar queue: " + siteData.url);

                            if (siteData.url == null) {
                                Thread.sleep(1000); // Aguardar se nenhuma URL estiver disponível
                                continue;
                            }

                            // Valida a URL antes de tentar conectar
                            if (!siteData.url.startsWith("http://") && !siteData.url.startsWith("https://")) {
                                System.out.println(
                                        downloader.getTimestamp() + " : URL inválida ignorada: " + siteData.url);
                                continue;
                            }

                            try {
                                // Conecta-se à URL e busca o documento
                                Document doc = Jsoup.connect(siteData.url)
                                        .timeout(10000) // Timeout de 10 segundos
                                        .userAgent("Mozilla/5.0") // User agent para evitar bloqueios
                                        .get();

                                // Processa o título
                                try {
                                    String title = doc.title();
                                    byte[] size = title.getBytes();
                                    int lim = Math.min(maxSizeTitle, size.length);
                                    title = new String(size, 0, lim);
                                    siteData.title = title.toLowerCase().replace("\n", " ");
                                    System.out.println(downloader.getTimestamp() + " : Title: " + siteData.title);
                                } catch (Exception e) {
                                    System.out.println(
                                            downloader.getTimestamp() + " : Erro ao processar título: "
                                                    + e.getMessage());
                                    siteData.title = ""; // Define o título como vazio em caso de erro
                                }

                                // Processa o conteúdo de texto
                                try {
                                    Elements paragraphs = doc.select("p");
                                    byte[] size = paragraphs.text().getBytes();
                                    int lim = Math.min(maxSizeText, size.length);
                                    String textCit = new String(size, 0, lim);
                                    siteData.text = textCit.toLowerCase().replace("\n", " ");
                                    System.out.println(downloader.getTimestamp() + " : Text processado");
                                } catch (Exception e) {
                                    System.out.println(
                                            downloader.getTimestamp() + " : Erro ao processar texto: "
                                                    + e.getMessage());
                                    siteData.text = ""; // Define o texto como vazio em caso de erro
                                }

                                // Processa tokens
                                try {
                                    doc.select("button, .slide").remove(); // Remove elementos indesejados
                                    String token = doc.text();
                                    byte[] size = token.getBytes();
                                    int lim = Math.min(maxSizeTokens, size.length);
                                    token = new String(size, 0, lim);
                                    siteData.tokens = token.toLowerCase().replace("\n", " ");
                                    System.out.println(downloader.getTimestamp() + " : Tokens processados");
                                } catch (Exception e) {
                                    System.out.println(
                                            downloader.getTimestamp() + " : Erro ao processar tokens: "
                                                    + e.getMessage());
                                    siteData.tokens = ""; // Define os tokens como vazios em caso de erro
                                }

                                // Processa links
                                try {
                                    Elements links = doc.select("a[href]");
                                    StringBuilder coupleLinks = new StringBuilder();
                                    for (Element link : links) {
                                        String href = link.attr("abs:href");
                                        if (href != null && !href.isEmpty() &&
                                                (href.startsWith("http://") || href.startsWith("https://"))) {
                                            coupleLinks.append(href).append(" ");
                                            try {
                                                gateway.queueUrls(href); // Adiciona a URL encontrada à fila
                                            } catch (Exception e) {
                                                System.out.println(downloader.getTimestamp()
                                                        + " : Erro ao adicionar URL à fila: " + href);
                                            }
                                        }
                                    }
                                    siteData.links = coupleLinks.toString().replace("\n", " ");
                                    System.out.println(downloader.getTimestamp() + " : Links processados");
                                } catch (Exception e) {
                                    System.out.println(
                                            downloader.getTimestamp() + " : Erro ao processar links: "
                                                    + e.getMessage());
                                    siteData.links = ""; // Define os links como vazios em caso de erro
                                }

                                // Tenta enviar os dados processados para os barrels
                                if (!siteData.isEmpty()) {
                                    downloader.sendToBarrels(siteData);
                                }

                            } catch (org.jsoup.HttpStatusException e) {
                                System.out.println(downloader.getTimestamp() + " : A URL (" + siteData.url
                                        + ") retornou status " + e.getStatusCode());
                            } catch (java.net.MalformedURLException e) {
                                System.out.println(downloader.getTimestamp() + " : URL mal formada: " + siteData.url);
                            } catch (java.net.UnknownHostException e) {
                                System.out.println(downloader.getTimestamp() + " : Host desconhecido: " + siteData.url);
                            } catch (java.net.SocketTimeoutException e) {
                                System.out
                                        .println(downloader.getTimestamp() + " : Timeout ao acessar: " + siteData.url);
                            } catch (Exception e) {
                                System.out
                                        .println(downloader.getTimestamp() + " : Erro ao processar URL " + siteData.url
                                                + ": " + e.getMessage());
                            }

                        } catch (RemoteException e) {
                            System.out.println(
                                    downloader.getTimestamp() + " : Perdeu conexão com a gateway: " + e.getMessage());
                            gateway = null; // Reinicia o gateway em caso de perda de conexão
                            gatewayibs = null; // Reinicia o gateway IBS
                            break;
                        }
                    }
                } catch (RemoteException e) {
                    System.out.println(downloader.getTimestamp() + " : Gateway não disponível: " + e.getMessage());
                    gateway = null; // Reinicia o gateway em caso de falha
                    gatewayibs = null; // Reinicia o gateway IBS
                    try {
                        Thread.sleep(RETRY_DELAY); // Aguardar antes de tentar reconectar
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break; // Sair do loop se interrompido
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(LocalDateTime.now() + " : Erro ao carregar arquivo de propriedades: " + e.getMessage());
            e.printStackTrace(); // Imprime a pilha de chamadas para depuração
        }
    }
}