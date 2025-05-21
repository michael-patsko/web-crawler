package uk.co.exercise.webcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class WebCrawler {

    private static final Integer TEN_SECONDS_IN_MILLISECONDS = 10 * 1000;

    private final String startUrl;
    private final Integer timeoutInMilliseconds;
    private final String rootDomain;
    private final LinkedList<String> urlsToVisit = new LinkedList<>();
    private final Set<String> visitedUrls = new HashSet<>();

    private String normalizedStartUrl;
    private long startTime;

    private final Boolean debugMode = false;

    public WebCrawler(String startUrl, Integer timeoutInSeconds) {
        this.startUrl = startUrl;
        this.timeoutInMilliseconds = timeoutInSeconds * 1000;
        this.rootDomain = URI.create(startUrl).getHost();
    }

    public void crawl() {
        startUp();

        while (thereAreUrlsToVisitAndProcessHasExceededTimeout()) {

            var urlToVisit = urlsToVisit.poll();

            // This shouldn't happen, as we check before adding a new URL to visit, but *just in case*!
            if (visitedUrls.contains(urlToVisit)) {
                continue;
            }


            try {
                // TODO what happens if this GET fails or times out? Shouldn't add it to visited or try the rest of the loop
                var document = Jsoup.connect(urlToVisit).timeout(TEN_SECONDS_IN_MILLISECONDS).get();

                visitedUrls.add(urlToVisit);

                getLinksOnPageAndAddThemToQueue(document);
            } catch (Exception e) {
                // TODO: swallow error for now
            }

            // Force only one iteration of while loop, for debugging purposes
            if (debugMode) {
                break;
            }
        }

        System.out.println("Crawling finished!");
        System.out.println("Visited " + visitedUrls.size() + " distinct URLs");
        System.out.println("Visited URLs: ");
        visitedUrls.forEach(System.out::println);
    private void startUp() {
        try {
            normalizedStartUrl = normalizeUrl(startUrl);
        } catch (Exception e) {
            System.out.println("Starting URL is invalid. Cannot start crawling.");
        }

        urlsToVisit.add(normalizedStartUrl);
        System.out.println("Crawling URL: " + normalizedStartUrl);

        startTime = System.currentTimeMillis();
    }

    private boolean thereAreUrlsToVisitAndProcessHasExceededTimeout() {
        if (urlsToVisit.isEmpty()) return false;
        return !hasTimedOut();
    }

    private boolean hasTimedOut() {
        if (System.currentTimeMillis() - startTime > timeoutInMilliseconds) {
            System.out.println("Crawler stopping after running for " + (System.currentTimeMillis() - startTime)/1000  + " seconds");
            return true;
        }
        return false;
    }

    private void getLinksOnPageAndAddThemToQueue(Document document) {
        var linkElements = document.select("a[href]");

        linkElements.forEach(link -> {
            var url = link.absUrl("href");

            try {
                var normalizedUrl = normalizeUrl(url);

                if (hasRootDomain(normalizedUrl) && !urlsToVisit.contains(normalizedUrl)) {
                    urlsToVisit.add(normalizedUrl);
                }
            } catch (Exception e) {
                System.out.println("Could not normalize " + url + " because " + e.getMessage());
                failedUrls.put(url, e.getMessage());
            }
        });
    }

    private Boolean hasRootDomain(String url) {
        return URI.create(url).getHost().equals(rootDomain);
    }

    private String normalizeUrl(String url) throws URISyntaxException {
        var uri = URI.create(url).normalize();

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
