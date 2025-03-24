package meta1sd;

import java.io.Serializable;

public class SiteData implements Serializable {
    public String url;
    public String title;
    public String text;
    public String tokens;
    public String links;
    private boolean isPropagated;

    public SiteData() {
        // Construtor vazio necessário para RMI
        this.url = "";
        this.title = "";
        this.text = "";
        this.tokens = "";
        this.links = "";
        this.isPropagated = false;
    }

    public SiteData(String url, String tokens, String links) {
        this.url = url;
        this.title = "";
        this.text = "";
        this.tokens = tokens;
        this.links = links;
        this.isPropagated = false;
    }

    public boolean isEmpty() {
        return (title == null || title.isEmpty()) &&
                (text == null || text.isEmpty()) &&
                (tokens == null || tokens.isEmpty()) &&
                (links == null || links.isEmpty());
    }

    public boolean isPropagated() {
        return isPropagated;
    }

    public void setPropagated(boolean propagated) {
        this.isPropagated = propagated;
    }
}