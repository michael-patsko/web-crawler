package uk.co.exercise.webcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class WebCrawler {

    private static final Integer TEN_SECONDS_IN_MILLISECONDS = 10 * 1000;

    private final String startUrl;
    private final Integer timeoutInMilliseconds;
    private final String rootDomain;
    private final LinkedList<String> urlsToVisit = new LinkedList<>();
    private final Set<String> visitedUrls = new HashSet<>();
    private final Map<String, String> failedUrls = new HashMap<>();

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

        while (thereAreUrlsToVisitAndProcessHasNotExceededTimeout()) {
            var urlToVisit = urlsToVisit.poll();

            // This shouldn't happen, as we check before adding a new URL to the queue but *just in case*!
            if (urlHasBeenVisited(urlToVisit) || urlHasFailedAlready(urlToVisit)) {
                continue;
            }

            try {
                // TODO what happens if this GET fails or times out? Shouldn't add it to visited or try the rest of the loop
                var document = Jsoup.connect(urlToVisit).timeout(TEN_SECONDS_IN_MILLISECONDS).get();
                addLinksOnPageToQueue(document);
                visitedUrls.add(urlToVisit);
            } catch (Exception e) {
                System.out.println("Failed to visit " + urlToVisit + " because " + e.getMessage());
                failedUrls.put(urlToVisit, e.getMessage());
            }

            // Force only one iteration of while loop, for debugging purposes
            if (debugMode) {
                break;
            }
        }

        finish();
    }

    private void startUp() {
        try {
            normalizedStartUrl = normalizeUrl(startUrl, startUrl);
        } catch (Exception e) {
            System.out.println("Starting URL is invalid. Cannot start crawling.");
        }

        urlsToVisit.add(normalizedStartUrl);
        System.out.println("Crawling URL: " + normalizedStartUrl);
        startTime = System.currentTimeMillis();
    }

    private void finish() {
        System.out.println("Crawling finished! Visited " + visitedUrls.size() + " distinct URLs:");
        visitedUrls.forEach(System.out::println);
        System.out.println("\nFailed to visit URLS: ");
        failedUrls.forEach((url, failureReason) -> System.out.println(url + ", reason: " + failureReason));
    }

    private boolean thereAreUrlsToVisitAndProcessHasNotExceededTimeout() {
        if (urlsToVisit.isEmpty()) return false;
        return !hasProcessTimedOut();
    }

    private boolean hasProcessTimedOut() {
        if (System.currentTimeMillis() - startTime > timeoutInMilliseconds) {
            System.out.println("Crawler stopping after running for " + (System.currentTimeMillis() - startTime)/1000  + " seconds");
            return true;
        }
        return false;
    }

    private void addLinksOnPageToQueue(Document document) {
        var linkElements = document.select("a[href]");
        var baseUri = document.baseUri();

        linkElements.forEach(link -> {
            var url = link.absUrl("href");

            try {
                var normalizedUrl = normalizeUrl(url, baseUri);

                if (hasRootDomain(normalizedUrl)
                        && !urlIsInQueue(normalizedUrl)
                        && !urlHasBeenVisited(normalizedUrl)
                        && !urlHasFailedAlready(normalizedUrl)
                ) {
                    urlsToVisit.add(normalizedUrl);
                }

            } catch (Exception e) {
                failedUrls.put(url, e.getMessage());
            }
        });
    }

    private Boolean hasRootDomain(String url) {
        return URI.create(url).getHost().equals(rootDomain);
    }

    private Boolean urlHasBeenVisited(String url) {
        return visitedUrls.contains(url);
    }

    private Boolean urlIsInQueue(String url) {
        return urlsToVisit.contains(url);
    }

    private Boolean urlHasFailedAlready(String url) {
        return failedUrls.containsKey(url);
    }

    private String normalizeUrl(String url, String base) {
        var baseUri = URI.create(base);

        // Get rid of illegal characters in raw href (e.g. spaces)
        var encodedUrl = fixHrefEncoding(url);

        return baseUri.resolve(encodedUrl).toASCIIString();
    }

    private String fixHrefEncoding(String url) {
        try {
            var uri = URI.create(url);

            return new URI(
                    uri.getScheme() != null ? uri.getScheme() : "http",
                    uri.getAuthority(),
                    encodePath(uri.getPath()),
                    null,
                    null
            ).toASCIIString();
        } catch (Exception e) {
            return url.replace(" ", "%20");
        }
    }

    private String encodePath(String path) {
        // Prevents treating example.com and example.com/ as two different URLs
        if (path == null || path.isEmpty()) {
            return "/";
        }

        // This is an attempt to fix jsoup's encoding/decoding issues of URL paths
        // Slashes get encoded as %2F and spaces get replaced with +, but should be %20
        return Arrays.stream(path.split("/"))
                .map(segment ->
                    URLEncoder.encode(segment, StandardCharsets.UTF_8)
                        .replace("+", "%20"))
                .collect(Collectors.joining("/"));
    }

    private void encodeQuery(String query) {
        // TODO
    }
}
