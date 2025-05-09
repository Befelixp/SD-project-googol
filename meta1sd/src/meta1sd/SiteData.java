package meta1sd;

import java.io.Serializable;

/**
 * SiteData - Classe que representa os dados de um site, incluindo URL, título,
 * texto, tokens e links. Implementa a interface Serializable para permitir a
 * transmissão de objetos via RMI.
 */
public class SiteData implements Serializable {
    public String url; // URL do site
    public String title; // Título do site
    public String text; // Texto do site
    public String tokens; // Tokens extraídos do site
    public String links; // Links encontrados no site
    private boolean isPropagated; // Indica se os dados foram propagados

    /**
     * Construtor vazio necessário para RMI.
     */
    public SiteData() {
        this.url = "";
        this.title = "";
        this.text = "";
        this.tokens = "";
        this.links = "";
        this.isPropagated = false;
    }

    /**
     * Construtor que inicializa os dados do site.
     * 
     * @param url    A URL do site.
     * @param tokens Os tokens extraídos do site.
     * @param links  Os links encontrados no site.
     */
    public SiteData(String url, String tokens, String links) {
        this.url = url;
        this.title = "";
        this.text = "";
        this.tokens = tokens;
        this.links = links;
        this.isPropagated = false;
    }

    /**
     * Verifica se o objeto SiteData está vazio.
     * 
     * @return true se todos os campos (título, texto, tokens, links) estiverem
     *         vazios; caso contrário, false.
     */
    public boolean isEmpty() {
        return (title == null || title.isEmpty()) &&
                (text == null || text.isEmpty()) &&
                (tokens == null || tokens.isEmpty()) &&
                (links == null || links.isEmpty());
    }

    /**
     * Verifica se os dados foram propagados.
     * 
     * @return true se os dados foram propagados; caso contrário, false.
     */
    public boolean isPropagated() {
        return isPropagated;
    }

    /**
     * Define o estado de propagação dos dados.
     * 
     * @param propagated O estado de propagação a ser definido.
     */
    public void setPropagated(boolean propagated) {
        this.isPropagated = propagated;
    }
}