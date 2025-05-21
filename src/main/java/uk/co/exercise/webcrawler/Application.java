package uk.co.exercise.webcrawler;

public class Application {
    public static void main(String[] args) {
        var startUrl = args[0];
        var timeoutInSeconds = Integer.parseInt(args[1]);
        var crawler = new WebCrawler(startUrl, timeoutInSeconds);
        crawler.crawl();
    }
}
