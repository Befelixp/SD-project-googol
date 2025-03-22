package meta1sd;

import java.io.Serializable;

public class SiteData implements Serializable {
    public String id = "", url = "", title = "", text = "", tokens = "", links = "";

    public SiteData() {

    }

    @Override
    public String toString() {
        return "type0\n" + url + "\n" + title + "\n" + text + "\n" + tokens + "\n" + links;
    }

}