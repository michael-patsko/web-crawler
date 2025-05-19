package uk.co.exercise.webcrawler;

import org.jsoup.Jsoup;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class WebCrawler {

    private static final Integer TEN_SECONDS_IN_MILLISECONDS = 10 * 1000;

    private final String startUrl;
    private final String rootDomain;
    private final LinkedList<String> urlsToVisit = new LinkedList<>();
    private final Set<String> visitedUrls = new HashSet<>();

    private final Boolean debugMode = true;

    public WebCrawler(String startUrl) {
        this.startUrl = startUrl;
        this.rootDomain = URI.create(startUrl).getHost();
    }

    public void crawl() {
        System.out.println("Crawling URL: " + startUrl);

        // TODO: Should normalize first

        urlsToVisit.add(startUrl);

        while (!urlsToVisit.isEmpty()) {
            var urlToVisit = urlsToVisit.poll();

            // This shouldn't happen, as we check before adding a new URL to visit, but *just in case*!
            if (visitedUrls.contains(urlToVisit)) {
                continue;
            }

            System.out.println("Visiting URL: " + urlToVisit);

            try {
                var document = Jsoup.connect(urlToVisit).timeout(TEN_SECONDS_IN_MILLISECONDS).get();
                visitedUrls.add(urlToVisit);

                var linkElements = document.select("a[href]");

                linkElements.forEach(link -> {
                    var url = link.absUrl("href");

                    try {
                        var normalizedUrl = normalizeUrl(url);

                        if (hasRootDomain(url) && !urlsToVisit.contains(normalizedUrl)) {
                            System.out.println(normalizedUrl);
                            urlsToVisit.add(normalizedUrl);
                        }
                    } catch (URISyntaxException e) {
                        System.out.println("Oops!");
                    }

                });
            } catch (Exception e) {
                // TODO: swallow error for now
            }

            // Force only one iteration of while loop, for debugging purposes
            if (debugMode) {
                break;
            }
        }
    }

    private Boolean hasRootDomain(String url) {
        return URI.create(url).getHost().equals(rootDomain);
    }

    private String normalizeUrl(String url) throws URISyntaxException {
        var uri = URI.create(url);

        var scheme = uri.getScheme();
        if (scheme == null) {
            scheme = "http";
        }

        // Prevents treating example.com and example.com/ as two different URLs
        var path = uri.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        var normalizedUri = new URI(scheme, uri.getAuthority(), path, null, null);
        // We aren't interested in fragment or query

        return normalizedUri.toString();
    }
}
