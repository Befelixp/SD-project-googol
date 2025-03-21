package meta1sd;

import org.jsoup.HttpStatusException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Properties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Downloader {
    private static final int RETRY_DELAY = 5000; // 5 segundos

    public static void main(String[] args) {
        String registryN;
        int maxSizeText, maxSizeTitle, maxSizeTokens;
        System.out.println("Iniciando Downloader");

        try {
            Downloader downloader = new Downloader();
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            System.out.println("Carregando arquivo de propriedades");
            prop.load(input);
            System.out.println("Arquivo de propriedades carregado");
            registryN = prop.getProperty("registryN");
            System.out.println("registryN: " + registryN);
            maxSizeText = Integer.parseInt(prop.getProperty("maxSizeText"));
            maxSizeTitle = Integer.parseInt(prop.getProperty("maxSizeTitle"));
            maxSizeTokens = Integer.parseInt(prop.getProperty("maxSizeTokens"));

            RMIGatewayDownloaderInterface gateway = null;

            while (true) { // Loop principal
                try {
                    // Se gateway é null, tenta conectar
                    if (gateway == null) {
                        System.out.println("Tentando conectar ao RMI...");
                        gateway = (RMIGatewayDownloaderInterface) Naming.lookup(registryN);
                        System.out.println("Conexão RMI estabelecida com sucesso");
                    }

                    // Loop de processamento de URLs
                    while (true) {
                        SiteData siteData = new SiteData();
                        try {
                            siteData.url = gateway.popqueue();
                            System.out.println("Tentando pegar queue: " + siteData.url);

                            if (siteData.url == null) {
                                // Se não há URLs na fila, espera um pouco
                                Thread.sleep(1000);
                                continue;
                            }

                            Document doc = Jsoup.connect(siteData.url).get();

                            // Title
                            String title = doc.title();
                            byte[] size = title.getBytes();
                            int lim = Math.min(maxSizeTitle, size.length);
                            title = new String(size, 0, lim);
                            siteData.title = title.toLowerCase().replace("\n", " ");
                            System.out.println("Title: " + siteData.title);

                            // Text
                            Elements paragraphs = doc.select("p");
                            size = paragraphs.text().getBytes();
                            lim = Math.min(maxSizeText, size.length);
                            String textCit = new String(size, 0, lim);
                            siteData.text = textCit.toLowerCase().replace("\n", " ");
                            System.out.println("Text: " + siteData.text);

                            // Tokens
                            doc.select("button, .slide").remove();
                            String token = doc.text();
                            size = token.getBytes();
                            lim = Math.min(maxSizeTokens, size.length);
                            token = new String(size, 0, lim);
                            siteData.tokens = token.toLowerCase().replace("\n", " ");
                            System.out.println("Tokens: " + siteData.tokens);

                            // Links
                            Elements links = doc.select("a[href]");
                            StringBuilder coupleLinks = new StringBuilder();
                            for (Element link : links) {
                                String href = link.attr("abs:href");
                                coupleLinks.append(href).append(" ");
                                gateway.queueUrls(href);
                            }
                            siteData.links = coupleLinks.toString().replace("\n", " ");
                            System.out.println("Links: " + siteData.links);

                        } catch (HttpStatusException e) {
                            System.out.println("A url (" + siteData.url + ") não permite indexação!");
                            continue;
                        } catch (RemoteException e) {
                            System.out.println("Perdeu conexão com a gateway: " + e.getMessage());
                            gateway = null; // Reset da conexão
                            break; // Sai do loop interno para tentar reconectar
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Gateway não disponível: " + e.getMessage());
                    gateway = null; // Garante que gateway está null
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao carregar arquivo de propriedades: " + e.getMessage());
            e.printStackTrace();
        }
    }
}