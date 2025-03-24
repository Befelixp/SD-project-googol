package meta1sd;

import org.jsoup.HttpStatusException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Downloader extends UnicastRemoteObject implements RMIDownloaderIBSGateway {
    private static final int RETRY_DELAY = 5000; // 5 segundos
    private int downloaderId;
    private Map<Integer, RMIIndexStorageBarrel> barrels = new HashMap<>();

    public Downloader(int downloaderId) throws RemoteException {
        this.downloaderId = downloaderId;
    }

    public void registerIBS(int id, RMIIndexStorageBarrel barrel) throws RemoteException {
        barrels.put(id, barrel);
        System.out.println("Barrel" + id + " registada!");
        barrels.get(id).gatewaypong("Downloader" + this.downloaderId);
    }

    public void registerExistingIBS(Map<Integer, RMIIndexStorageBarrel> barrells) throws RemoteException {
        // Adiciona todas as barrels recebidas ao mapa local
        barrels.putAll(barrells);

        // Imprime todas as barrels registradas
        System.out.println("Barrels registradas no Downloader " + downloaderId + ":");
        barrels.forEach((id, barrel) -> {
            System.out.println("- Barrel ID: " + id);
        });
    }

    public int getDownloader() {
        return downloaderId;
    }

    private boolean sendToBarrels(SiteData siteData) {
        for (Map.Entry<Integer, RMIIndexStorageBarrel> entry : barrels.entrySet()) {
            int id = entry.getKey();
            RMIIndexStorageBarrel barrel = entry.getValue();

            try {
                barrel.storeSiteData(siteData);
                System.out.printf(
                        "[%s] ✅ Sucesso: SiteData enviado para Barrel %d - URL: %s%n",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        id,
                        siteData.url);
                return true; // Retorna true após enviar com sucesso para uma barrel
            } catch (RemoteException e) {
                System.err.printf(
                        "[%s] ❌ Erro: Falha ao enviar SiteData para Barrel %d - URL: %s - Erro: %s%n",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        id,
                        siteData.url,
                        e.getMessage());
            }
        }

        System.err.printf(
                "[%s] ❌ Falha: Não foi possível enviar SiteData para nenhuma barrel - URL: %s%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                siteData.url);
        return false; // Retorna false se não conseguiu enviar para nenhuma barrel
    }

    public static void main(String[] args) {
        String registryN, registryNibs;
        int maxSizeText, maxSizeTitle, maxSizeTokens;

        try {
            Downloader downloader = new Downloader(Integer.parseInt(args[0]));
            Properties prop = new Properties();
            InputStream input = new FileInputStream(args[1]);
            System.out.println(LocalDateTime.now() + " : Iniciando Downloader " + downloader.getDownloader());
            System.out.println(LocalDateTime.now() + " : Carregando arquivo de propriedades");
            prop.load(input);
            System.out.println(LocalDateTime.now() + " : Arquivo de propriedades carregado");

            registryN = prop.getProperty("registryN");
            registryNibs = prop.getProperty("registryNibs");

            System.out.println(LocalDateTime.now() + " : registryN: " + registryN);
            maxSizeText = Integer.parseInt(prop.getProperty("maxSizeText"));
            maxSizeTitle = Integer.parseInt(prop.getProperty("maxSizeTitle"));
            maxSizeTokens = Integer.parseInt(prop.getProperty("maxSizeTokens"));

            RMIGatewayDownloaderInterface gateway = null;
            RMIGatewayIBSDownloader gatewayibs = null;

            while (true) { // Loop principal
                try {
                    // Se gateway é null, tenta conectar
                    if (gateway == null || gatewayibs == null) {
                        System.out.println(LocalDateTime.now() + " : Tentando conectar ao RMI...");
                        gateway = (RMIGatewayDownloaderInterface) Naming.lookup(registryN);
                        gatewayibs = (RMIGatewayIBSDownloader) Naming.lookup(registryNibs);

                        gatewayibs.registerDownloader(Integer.parseInt(args[0]), downloader);
                        System.out.println(LocalDateTime.now() + " : Conexão RMI estabelecida com sucesso");
                    }

                    // Loop de processamento de URLs
                    while (true) {
                        SiteData siteData = new SiteData();
                        try {
                            siteData.url = gateway.popqueue();
                            System.out.println(LocalDateTime.now() + " : Tentando pegar queue: " + siteData.url);

                            if (siteData.url == null) {
                                Thread.sleep(1000);
                                continue;
                            }

                            // Validar URL antes de tentar conectar
                            if (!siteData.url.startsWith("http://") && !siteData.url.startsWith("https://")) {
                                System.out.println(LocalDateTime.now() + " : URL inválida ignorada: " + siteData.url);
                                continue;
                            }

                            try {
                                Document doc = Jsoup.connect(siteData.url)
                                        .timeout(10000) // Timeout de 10 segundos
                                        .userAgent("Mozilla/5.0") // User agent para evitar bloqueios
                                        .get();

                                // Title
                                try {
                                    String title = doc.title();
                                    byte[] size = title.getBytes();
                                    int lim = Math.min(maxSizeTitle, size.length);
                                    title = new String(size, 0, lim);
                                    siteData.title = title.toLowerCase().replace("\n", " ");
                                    System.out.println(LocalDateTime.now() + " : Title: " + siteData.title);
                                } catch (Exception e) {
                                    System.out.println(
                                            LocalDateTime.now() + " : Erro ao processar título: " + e.getMessage());
                                    siteData.title = "";
                                }

                                // Text
                                try {
                                    Elements paragraphs = doc.select("p");
                                    byte[] size = paragraphs.text().getBytes();
                                    int lim = Math.min(maxSizeText, size.length);
                                    String textCit = new String(size, 0, lim);
                                    siteData.text = textCit.toLowerCase().replace("\n", " ");
                                    System.out.println(LocalDateTime.now() + " : Text processado");
                                } catch (Exception e) {
                                    System.out.println(
                                            LocalDateTime.now() + " : Erro ao processar texto: " + e.getMessage());
                                    siteData.text = "";
                                }

                                // Tokens
                                try {
                                    doc.select("button, .slide").remove();
                                    String token = doc.text();
                                    byte[] size = token.getBytes();
                                    int lim = Math.min(maxSizeTokens, size.length);
                                    token = new String(size, 0, lim);
                                    siteData.tokens = token.toLowerCase().replace("\n", " ");
                                    System.out.println(LocalDateTime.now() + " : Tokens processados");
                                } catch (Exception e) {
                                    System.out.println(
                                            LocalDateTime.now() + " : Erro ao processar tokens: " + e.getMessage());
                                    siteData.tokens = "";
                                }

                                // Links
                                try {
                                    Elements links = doc.select("a[href]");
                                    StringBuilder coupleLinks = new StringBuilder();
                                    for (Element link : links) {
                                        String href = link.attr("abs:href");
                                        if (href != null && !href.isEmpty() &&
                                                (href.startsWith("http://") || href.startsWith("https://"))) {
                                            coupleLinks.append(href).append(" ");
                                            try {
                                                gateway.queueUrls(href);
                                            } catch (Exception e) {
                                                System.out.println(LocalDateTime.now()
                                                        + " : Erro ao adicionar URL à fila: " + href);
                                            }
                                        }
                                    }
                                    siteData.links = coupleLinks.toString().replace("\n", " ");
                                    System.out.println(LocalDateTime.now() + " : Links processados");
                                } catch (Exception e) {
                                    System.out.println(
                                            LocalDateTime.now() + " : Erro ao processar links: " + e.getMessage());
                                    siteData.links = "";
                                }

                                // Tenta enviar para as barrels
                                if (!siteData.isEmpty()) {
                                    downloader.sendToBarrels(siteData);
                                }

                            } catch (org.jsoup.HttpStatusException e) {
                                System.out.println(LocalDateTime.now() + " : A URL (" + siteData.url
                                        + ") retornou status " + e.getStatusCode());
                            } catch (java.net.MalformedURLException e) {
                                System.out.println(LocalDateTime.now() + " : URL mal formada: " + siteData.url);
                            } catch (java.net.UnknownHostException e) {
                                System.out.println(LocalDateTime.now() + " : Host desconhecido: " + siteData.url);
                            } catch (java.net.SocketTimeoutException e) {
                                System.out.println(LocalDateTime.now() + " : Timeout ao acessar: " + siteData.url);
                            } catch (Exception e) {
                                System.out.println(LocalDateTime.now() + " : Erro ao processar URL " + siteData.url
                                        + ": " + e.getMessage());
                            }

                        } catch (RemoteException e) {
                            System.out.println(
                                    LocalDateTime.now() + " : Perdeu conexão com a gateway: " + e.getMessage());
                            gateway = null;
                            break;
                        }
                    }
                } catch (RemoteException e) {
                    System.out.println(LocalDateTime.now() + " : Gateway não disponível: " + e.getMessage());
                    gateway = null;
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(LocalDateTime.now() + " : Erro ao carregar arquivo de propriedades: " + e.getMessage());
            e.printStackTrace();
        }
    }

}