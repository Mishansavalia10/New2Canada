package com.new2canada.crawler;

import com.new2canada.config.AppConfig;
import com.new2canada.search.SearchEngine;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs the apartment crawler at boot and again every
 * {@link AppConfig#CRAWL_INTERVAL_MIN} minutes.
 *
 * <p>Wrapped in try/catch so a remote-site failure never crashes the
 * scheduler thread.
 *
 * Demonstrates: scheduled tasks, polite crawling.
 */
public class CrawlScheduler {

    private final SearchEngine engine;
    private final CrawlGate gate = new CrawlGate(AppConfig.MANUAL_CRAWL_COOLDOWN_MS);

    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "crawl-scheduler");
        t.setDaemon(true);
        return t;
    });

    public CrawlScheduler(SearchEngine engine) {
        this.engine = engine;
    }

    /**
     * Kicks off the very first crawl immediately + recurring crawls thereafter.
     * Does nothing when live crawling is disabled (see {@link AppConfig#CRAWL_ENABLED}).
     */
    public void start() {
        if (!AppConfig.CRAWL_ENABLED) {
            System.out.println("[crawler] live crawling disabled (NEW2CANADA_CRAWL=off); using cached data.");
            return;
        }
        exec.scheduleAtFixedRate(this::scheduledRun, 0, AppConfig.CRAWL_INTERVAL_MIN, TimeUnit.MINUTES);
    }

    /** Requests a crawl from a user; refused while one runs or during cooldown. */
    public CrawlGate.Decision triggerNow() {
        if (!AppConfig.CRAWL_ENABLED) return CrawlGate.Decision.DISABLED;
        CrawlGate.Decision d = gate.tryStartManual();
        if (d == CrawlGate.Decision.STARTED) exec.submit(this::runOnce);
        return d;
    }

    public void shutdown() {
        exec.shutdownNow();
    }

    private void scheduledRun() {
        if (gate.tryStartScheduled() == CrawlGate.Decision.STARTED) runOnce();
    }

    /** Runs a crawl; the caller must already hold the gate (see tryStart*). */
    private void runOnce() {
        try {
            crawl();
        } finally {
            gate.finish();
        }
    }

    private void crawl() {
        System.out.println("[crawler] starting refresh…");
        long started = System.currentTimeMillis();

        int total = 0;
        try (PoliteFetcher fetcher = new PoliteFetcher()) {
            total = new HousingCrawler(fetcher, engine::ingestApartment).crawl();
        } catch (Exception e) {
            System.err.println("HousingCrawler failed: " + e.getMessage());
        }

        engine.rebuildIndex();

        long secs = (System.currentTimeMillis() - started) / 1000;
        System.out.printf("[crawler] done in %ds — %d scraped pages%n", secs, total);
    }
}
