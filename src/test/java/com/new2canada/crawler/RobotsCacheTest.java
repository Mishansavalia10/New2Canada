package com.new2canada.crawler;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs RobotsCache against tiny local HTTP servers, so the tests need no
 * internet access. Each server answers /robots.txt with a fixed status.
 */
class RobotsCacheTest {

    private static final String AGENT = "New2Canada-StudentProject";
    private static final List<HttpServer> servers = new ArrayList<>();
    private static final AtomicInteger okHits = new AtomicInteger();
    private static String okBase, notFoundBase, brokenBase;

    @BeforeAll
    static void start() throws IOException {
        okBase       = serve(200, "User-agent: *\nDisallow: /private\n", okHits);
        notFoundBase = serve(404, "nope", null);
        brokenBase   = serve(503, "maintenance", null);
    }

    @AfterAll
    static void stop() { servers.forEach(s -> s.stop(0)); }

    private static String serve(int status, String body, AtomicInteger counter) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/robots.txt", ex -> {
            if (counter != null) counter.incrementAndGet();
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        });
        s.start();
        servers.add(s);
        return "http://127.0.0.1:" + s.getAddress().getPort();
    }

    @Test
    void honoursDisallowRulesFromServer() {
        RobotsCache cache = new RobotsCache(AGENT);
        assertTrue(cache.isAllowed(okBase + "/listings"));
        assertFalse(cache.isAllowed(okBase + "/private/page"));
    }

    @Test
    void fetchesRobotsOncePerOriginWithinTtl() {
        RobotsCache cache = new RobotsCache(AGENT);
        int before = okHits.get();
        cache.isAllowed(okBase + "/a");
        cache.isAllowed(okBase + "/b?page=2");
        cache.isAllowed(okBase + "/c");
        assertEquals(1, okHits.get() - before);
    }

    @Test
    void missingRobotsFileMeansNoRestrictions() {
        assertTrue(new RobotsCache(AGENT).isAllowed(notFoundBase + "/anything"));
    }

    @Test
    void serverErrorFailsClosed() {
        assertFalse(new RobotsCache(AGENT).isAllowed(brokenBase + "/listings"));
    }

    @Test
    void unreachableHostFailsClosed() {
        assertFalse(new RobotsCache(AGENT).isAllowed("http://127.0.0.1:1/listings"));
    }

    @Test
    void malformedUrlIsRejected() {
        assertFalse(new RobotsCache(AGENT).isAllowed("not a url"));
    }
}
