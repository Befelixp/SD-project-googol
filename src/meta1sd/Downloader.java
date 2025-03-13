package meta1sd;

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
        System.out.println("Iniciando Downloader");
        try {
            Downloader downloader = new Downloader();
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            prop.load(input);
            registryN = prop.getProperty("registryN");

            try {
                System.out.println("Tentando RMI...");
                // Parte do RMI
                RMIGatewayDownloaderInterface gateway = (RMIGatewayDownloaderInterface) Naming.lookup(registryN);
                System.out.println("Conexão RMI");
                String url = "";
                while (true) {
                    SiteData SiteData = new SiteData();
                    System.out.println("Pegando URLs");
                    SiteData.url = gateway.popqueue();
                    System.out.println("Tentando pegar queue");

                    if (SiteData.url != null) {
                        url = SiteData.url;
                        // SiteData.id = downloader.id;

                        Document doc = Jsoup.connect(url).get();

                        // Title
                        String title = doc.title();
                        SiteData.title = title.toLowerCase().replace("\n", " ");
                        System.out.println("Title: " + SiteData.title);

                        // Text
                        Elements paragraphs = doc.select("p");
                        SiteData.text = paragraphs.text().toLowerCase().replace("\n", " ");
                        System.out.println("Text: " + SiteData.text);

                        // Tokens
                        doc.select("button, .slide").remove();
                        String token = doc.text();
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
                    }
                    System.out.println("Downloader fechando");
                }
            } catch (Exception e) {
                System.out.println("Gateway Unreachable: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {

        }
    }
}