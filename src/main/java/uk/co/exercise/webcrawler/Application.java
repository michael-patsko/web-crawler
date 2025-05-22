package uk.co.exercise.webcrawler;

public class Application {
    public static void main(String[] args) {
        String startUrl;
        int timeoutInSeconds;
        boolean debugMode;

        try {
            startUrl = args[0];
            timeoutInSeconds = Integer.parseInt(args[1]);
            debugMode = Boolean.parseBoolean(args[2]);
        } catch (Exception e) {
            System.out.println("Invalid arguments. See README for correct usage.");
            return;
        }

        var crawler = new WebCrawler(startUrl, timeoutInSeconds, debugMode);
        crawler.crawl();
    }
}
