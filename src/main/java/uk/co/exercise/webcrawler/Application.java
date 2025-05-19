package uk.co.exercise.webcrawler;

import java.io.IOException;

public class Application {
    public static void main(String[] args) throws IOException, InterruptedException {
        var startUrl = args[0];
        var crawler = new WebCrawler(startUrl);
        crawler.crawl();
    }
}
