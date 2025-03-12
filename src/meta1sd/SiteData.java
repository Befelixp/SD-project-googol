package meta1sd;

public class SiteData {
    public String id = "", url = "", title = "", text = "", tokens = "", links = "";

    public SiteData() {

    }

    @Override
    public String toString() {
        return "type0\n" + url + "\n" + title + "\n" + text + "\n" + tokens + "\n" + links;
    }

}