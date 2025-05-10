package meta2sd.googol.sd.uc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import meta2sd.googol.sd.uc.controller.model.WebClient;

@Controller
public class PageController {

    @Autowired
    private WebClient client;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/index")
    public String index() {
        return "index";
    }

    @PostMapping("/index")
    public String processIndex(@RequestParam("option") String option, Model model) {
        switch (option) {
            case "0":
                return "redirect:/add-url";
            case "1":
                return "redirect:/search-page";
            case "2":
                return "redirect:/search-page?type=url";
            default:
                return "redirect:/";
        }
    }

    @GetMapping("/add-url")
    public String addUrl() {
        return "add-url";
    }

    @PostMapping("/add-url")
    public String processAddUrl(@RequestParam("url") String url, Model model) {
        boolean success = client.addURL(url);
        if (success) {
            model.addAttribute("message", "URL successfully submitted for indexing!");
            model.addAttribute("messageType", "success");
        } else {
            model.addAttribute("message", "Failed to submit URL for indexing. Please try again.");
            model.addAttribute("messageType", "error");
        }
        return "add-url";
    }

    @GetMapping("/search-page")
    public String searchPage() {
        return "search-page";
    }
}
