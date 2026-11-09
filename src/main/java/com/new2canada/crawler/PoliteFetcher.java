package com.new2canada.crawler;

import com.new2canada.config.AppConfig;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Single-threaded, polite headless-browser fetcher.
 *
 * <p>Many of the rental sites we scrape (Zumper, PadMapper, Liv.rent, ...)
 * render their listings with JavaScript, so a plain HTTP GET returns an
 * empty shell. This fetcher drives a headless Chrome instance and hands
 * Jsoup the fully-rendered page source instead.
 *
 * <p>Adds three guarantees that distinguish a student project from a hostile
 * scraper:
 *
 * <ul>
 *   <li>A descriptive User-Agent identifying the project and course.</li>
 *   <li>robots.txt is checked before every page; disallowed pages are skipped.</li>
 *   <li>A {@link AppConfig#FETCH_DELAY_MS} delay between hits to the
 *       <i>same host</i>, so a burst of crawler activity never floods a
 *       single site.</li>
 *   <li>A short page-load timeout — if a remote site is slow we fail fast and
 *       let the Firestore cache take over, instead of blocking a request
 *       thread.</li>
 * </ul>
 *
 * Demonstrates: respectful real-world crawling, simple per-host throttling
 * via a {@link HashMap}.
 */
public class PoliteFetcher implements AutoCloseable {

    /** host → epoch-millis of the last successful fetch. */
    private final Map<String, Long> lastFetchPerHost = new HashMap<>();

    private WebDriver driver;

    /** Honours each site's robots.txt before we open a page on it. */
    private final RobotsCache robots = new RobotsCache(AppConfig.USER_AGENT);

    public PoliteFetcher() {
        WebDriverManager.chromedriver().setup();
        this.driver = newDriver();
    }

    private static WebDriver newDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless=new",
                // NOT --disable-gpu: 4Rent and PadMapper build a WebGL map on
                // page load, and with no WebGL context the map constructor
                // throws an uncaught error that aborts the site's JS before it
                // ever fetches listings (the list sits at "Loading..." forever).
                // SwiftShader gives headless Chrome a software WebGL context.
                "--use-gl=angle",
                "--use-angle=swiftshader",
                "--enable-unsafe-swiftshader",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--window-size=1920,1080",
                "user-agent=" + AppConfig.USER_AGENT);
        // EAGER returns once the DOM is parsed instead of waiting for every last
        // subresource. The map sites stream tiles indefinitely, so waiting for
        // full "load" made driver.get() throw a renderer timeout and discard a
        // page whose listings had in fact already rendered. We wait for the
        // listings themselves (readySelector) rather than for the network.
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        WebDriver d = new ChromeDriver(options);
        d.manage().timeouts().pageLoadTimeout(
                java.time.Duration.ofMillis(AppConfig.PAGE_LOAD_TIMEOUT_MS));
        return d;
    }

    /** Fetches the URL as a parsed Jsoup {@link Document}, or {@code null} on failure. */
    public synchronized Document fetch(String url) {
        return fetch(url, null);
    }

    /**
     * Fetches the URL, first waiting (up to {@link AppConfig#RENDER_TIMEOUT_MS})
     * for {@code readyCss} to appear — pass {@code null} for server-rendered
     * pages that need no wait. A page whose listings never render still returns
     * its DOM rather than null, so the caller can fall back to the cache.
     */
    public synchronized Document fetch(String url, String readyCss) {
        if (url == null || url.isBlank()) return null;
        if (!robots.isAllowed(url)) {
            System.err.println("PoliteFetcher: robots.txt does not allow " + url + " — skipped");
            return null;
        }
        try {
            throttleSameHost(url);
            driver.get(url);
            if (readyCss != null && !readyCss.isBlank()) {
                try {
                    new WebDriverWait(driver, Duration.ofMillis(AppConfig.RENDER_TIMEOUT_MS))
                            .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(readyCss)));
                } catch (TimeoutException e) {
                    System.err.println("PoliteFetcher: " + url + " -> listings ("
                            + readyCss + ") never rendered; scraping anyway");
                }
            } else {
                // Server-rendered page: a short settle is enough.
                Thread.sleep(2000);
            }
            String html = driver.getPageSource();
            recordFetch(url);
            return Jsoup.parse(html, url);
        } catch (Exception e) {
            System.err.println("PoliteFetcher: " + url + " -> " + e.getMessage());
            // A page-load timeout or crash can leave the Chrome session dead
            // for every subsequent fetch — recreate it so later seeds in the
            // same crawl still get a chance.
            if (!isSessionAlive()) {
                try { driver.quit(); } catch (Exception ignored) {}
                try { driver = newDriver(); }
                catch (Exception ex) { System.err.println("PoliteFetcher: failed to restart Chrome -> " + ex.getMessage()); }
            }
            return null;
        }
    }

    private boolean isSessionAlive() {
        try { driver.getCurrentUrl(); return true; }
        catch (Exception e) { return false; }
    }

    private void throttleSameHost(String url) {
        String host = hostOf(url);
        Long last = lastFetchPerHost.get(host);
        if (last == null) return;
        long waitFor = AppConfig.FETCH_DELAY_MS - (System.currentTimeMillis() - last);
        if (waitFor > 0) {
            try { Thread.sleep(waitFor); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    private void recordFetch(String url) {
        lastFetchPerHost.put(hostOf(url), System.currentTimeMillis());
    }

    private static String hostOf(String url) {
        try { return new java.net.URI(url).getHost(); }
        catch (Exception e) { return url; }
    }

    /** Shuts down the underlying headless Chrome process. */
    @Override
    public void close() {
        try { driver.quit(); }
        catch (Exception ignored) {}
    }
}
