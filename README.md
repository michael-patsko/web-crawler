# Web crawler

## Intro

- Java 21
- Built with Maven
- Runs in Docker container

## Setup and usage

In the project root directory, you can either:

1. run `docker compose up --build` - this will crawl the default page, https://monzo.com, or
2. run with a specific starting URL like so:
```
docker build -t web-crawler .
docker run --rm web-crawler "https://www.example.com/"
```

## Developer decisions

### BFS versus DFS

### Limitations

At the moment:

 - only works for static HTML
 - doesn't work with URLs which redirect

### Future improvements

 - Use a headless browser, such as Selenium, to cope with dynamic content