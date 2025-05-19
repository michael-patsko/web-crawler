package uk.co.exercise.webcrawler;

import org.jsoup.Jsoup;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class WebCrawler {

    private static final Integer TEN_SECONDS_IN_MILLISECONDS = 10 * 1000;

    private final String startUrl;
    private final String rootDomain;
    private final LinkedList<String> urlsToVisit = new LinkedList<>();
    private final Set<String> visitedUrls = new HashSet<>();

    public WebCrawler(String startUrl) {
        this.startUrl = startUrl;
        this.rootDomain = URI.create(startUrl).getHost();
    }

    public void crawl() {
        System.out.println("\nWebCrawler\n");
        System.out.println("Crawling URL: " + startUrl);

        urlsToVisit.add(startUrl);

        while (!urlsToVisit.isEmpty()) {
            var urlToVisit = urlsToVisit.poll();
            if (visitedUrls.contains(urlToVisit)) {
                continue;
            }

            System.out.println("Visiting URL: " + urlToVisit);

            try {
                var document = Jsoup.connect(urlToVisit).timeout(TEN_SECONDS_IN_MILLISECONDS).get();
                visitedUrls.add(urlToVisit);

                var linkElements = document.select("a[href]");

                linkElements.forEach(link -> {
                    var url = normalizeUrl(link.absUrl("href"));
                    System.out.println(url);
                });
            } catch (Exception e) {
                // swallow error for now
            }
        }
    }

    private String normalizeUrl (String url) {
        var uri = URI.create(url);
        return uri.getAuthority() + uri.getPath();
        // We aren't interested in scheme or fragment
    }
}
