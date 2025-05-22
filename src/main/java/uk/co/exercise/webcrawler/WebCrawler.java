package uk.co.exercise.webcrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
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
    private final Boolean debugMode;

    private String normalizedStartUrl;
    private long startTime;


    public WebCrawler(String startUrl, Integer timeoutInSeconds, Boolean debugMode) {
        this.startUrl = startUrl;
        this.timeoutInMilliseconds = timeoutInSeconds * 1000;
        this.debugMode = debugMode;
        this.rootDomain = URI.create(startUrl).getHost();
    }

    public void crawl() {
        if (!canStartUp()) {
            System.out.println("Cannot start the web crawler.");
            return;
        }

        startUp();
        while (thereAreUrlsToVisitAndProcessHasNotExceededTimeout()) {
            var urlToVisit = urlsToVisit.poll();

            // This shouldn't happen, as we check before adding a new URL to the queue but *just in case*!
            if (urlHasBeenVisited(urlToVisit) || urlHasFailedAlready(urlToVisit)) {
                continue;
            }

            visitUrl(urlToVisit);
        }

        finish();
    }

    private Boolean canStartUp() {
        try {
            normalizedStartUrl = normalizeUrl(startUrl, startUrl);
            urlsToVisit.add(normalizedStartUrl);

        } catch (Exception e) {
            System.out.println("Starting URL is invalid. Cannot start crawling.");
            return false;
        }

        return true;
    }

    private void startUp() {
        System.out.println("Crawling URL: " + normalizedStartUrl);
        startTime = System.currentTimeMillis();
    }

    private void finish() {
        System.out.println("Crawling finished! Visited " + visitedUrls.size() + " distinct URLs:");
        visitedUrls.forEach(System.out::println);

        if (!failedUrls.isEmpty()) {
            System.out.println("\nFailed to visit URLS: ");

            if (debugMode) {
                printFailedUrls();
            } else {
                printFailedUrlsWithReason();
            }
        }
    }

    private void printFailedUrls() {
        failedUrls.keySet().forEach(System.out::println);
    }

    private void printFailedUrlsWithReason() {
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

    private void visitUrl(String url) {
        try {
            addLinksOnPageToQueue(url);
            visitedUrls.add(url);
        } catch (Exception e) {
            if (debugMode) {
                System.out.println("Failed to visit " + url + " because " + e.getMessage());
            }
            failedUrls.put(url, e.getMessage());
        }
    }

    private void addLinksOnPageToQueue(String url) throws IOException {
        var document = Jsoup.connect(url).followRedirects(true).timeout(TEN_SECONDS_IN_MILLISECONDS).get();
        var linkElements = document.select("a[href]");
        var baseUri = document.baseUri();

        linkElements.forEach(link -> tryToAddLinkToQueueIfValid(link, baseUri));
    }

    private void tryToAddLinkToQueueIfValid(Element link, String baseUri) {
        var href = link.absUrl("href");

        try {
            var normalizedUrl = normalizeUrl(href, baseUri);

            if (hasRootDomain(normalizedUrl)
                    && !urlIsInQueue(normalizedUrl)
                    && !urlHasBeenVisited(normalizedUrl)
                    && !urlHasFailedAlready(normalizedUrl)
            ) {
                urlsToVisit.add(normalizedUrl);
            }

        } catch (Exception e) {
            failedUrls.put(href, e.getMessage());
        }
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
