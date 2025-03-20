package meta1sd;

import org.jsoup.HttpStatusException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.Naming;
import java.util.Properties;

import org.jsoup.Jsoup;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Downloader {

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

            try {
                System.out.println("Tentando RMI...");
                // Parte do RMI
                RMIGatewayDownloaderInterface gateway = (RMIGatewayDownloaderInterface) Naming.lookup(registryN);
                System.out.println("Conexão RMI");
                String url = "";
                while (true) {
                    try {
                        SiteData SiteData = new SiteData();

                        SiteData.url = gateway.popqueue();
                        System.out.println("Tentando pegar queue: " + SiteData.url);
                        url = SiteData.url;
                        // SiteData.id = downloader.id;

                        Document doc = Jsoup.connect(url).get();

                        // Title
                        String title = doc.title();
                        byte[] size = title.getBytes();
                        int lim = Math.min(maxSizeTitle, size.length);
                        title = new String(size, 0, lim);
                        SiteData.title = title.toLowerCase().replace("\n", " ");
                        System.out.println("Title: " + SiteData.title);

                        // Text
                        String textCit;
                        Elements paragraphs = doc.select("p");
                        size = paragraphs.text().getBytes();
                        lim = Math.min(maxSizeText, size.length);
                        textCit = new String(size, 0, lim);
                        SiteData.text = textCit.toLowerCase().replace("\n", " ");
                        System.out.println("Text: " + SiteData.text);

                        // Tokens
                        doc.select("button, .slide").remove();

                        String token = doc.text();
                        size = token.getBytes();
                        lim = Math.min(maxSizeTokens, size.length);
                        token = new String(size, 0, lim);
                        SiteData.tokens = token.toLowerCase().replace("\n", " ");
                        System.out.println("Tokens: " + SiteData.tokens);

                        // Links
                        Elements links = doc.select("a[href]");
                        StringBuilder coupleLinks = new StringBuilder();
                        for (Element link : links) {
                            coupleLinks.append(link.attr("abs:href")).append(" ");

                            gateway.queueUrls(link.attr("abs:href"));
                        }
                        SiteData.links = coupleLinks.toString().replace("\n", " ");
                        System.out.println("Links: " + SiteData.links);
                    } catch (HttpStatusException e) {
                        System.out.println("A url (" + url + ") não permite indexação!");
                        continue;
                    }
                }

            } catch (Exception e) {
                System.out.println("Gateway Unreachable: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.out.println("Erro ao carregar arquivo de propriedades: " + e.getMessage());
            e.printStackTrace();
        }
    }
}