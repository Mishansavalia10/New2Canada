package com.new2canada.crawler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Downloads and caches each host's {@code /robots.txt}.
 *
 * <p>Results are kept for {@link #TTL} so a crawl cycle reuses them; the
 * crawler runs every few hours, so a fresh check per cycle is enough.
 *
 * <p>Status handling follows the usual crawler conventions:
 * <ul>
 *   <li>2xx → parse the file.</li>
 *   <li>4xx (no file, forbidden, gone) → no restrictions published.</li>
 *   <li>5xx, timeouts or network errors → treat the host as off-limits for
 *       this cycle, since we cannot tell what the site allows.</li>
 * </ul>
 */
public class RobotsCache {

    static final Duration TTL = Duration.ofHours(1);

    private record Entry(RobotsPolicy policy, long fetchedAt) {}

    private final HttpClient http;
    private final String productToken;
    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    public RobotsCache(String productToken) {
        this(productToken, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    RobotsCache(String productToken, HttpClient http) {
        this.productToken = productToken;
        this.http = http;
    }

    /** True if the page at {@code url} may be fetched by this crawler. */
    public boolean isAllowed(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) return false;
            String key = uri.getScheme() + "://" + host + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
            String pathAndQuery = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
            return policyFor(key).isAllowed(pathAndQuery);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    RobotsPolicy policyFor(String origin) {
        long now = System.currentTimeMillis();
        Entry e = cache.get(origin);
        if (e != null && now - e.fetchedAt() < TTL.toMillis()) return e.policy();
        RobotsPolicy fresh = download(origin);
        cache.put(origin, new Entry(fresh, now));
        return fresh;
    }

    private RobotsPolicy download(String origin) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(origin + "/robots.txt"))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", productToken)
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            int code = resp.statusCode();
            if (code >= 200 && code < 300) return RobotsPolicy.parse(resp.body(), productToken);
            if (code >= 400 && code < 500) return RobotsPolicy.allowAll();
            return RobotsPolicy.disallowAll();
        } catch (IOException e) {
            return RobotsPolicy.disallowAll();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RobotsPolicy.disallowAll();
        }
    }
}
