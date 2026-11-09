package com.new2canada.crawler;

import com.new2canada.crawler.CrawlGate.Decision;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CrawlGateTest {

    private static final long TEN_MIN = 10 * 60_000L;

    @Test
    void firstManualRequestStarts() {
        CrawlGate g = new CrawlGate(TEN_MIN, new AtomicLong(0)::get);
        assertEquals(Decision.STARTED, g.tryStartManual());
        assertTrue(g.isRunning());
    }

    @Test
    void secondRequestWhileRunningIsRefused() {
        CrawlGate g = new CrawlGate(TEN_MIN, new AtomicLong(0)::get);
        g.tryStartManual();
        assertEquals(Decision.ALREADY_RUNNING, g.tryStartManual());
        assertEquals(Decision.ALREADY_RUNNING, g.tryStartScheduled());
    }

    @Test
    void manualRequestInsideCooldownIsRefusedUntilItPasses() {
        AtomicLong now = new AtomicLong(0);
        CrawlGate g = new CrawlGate(TEN_MIN, now::get);
        g.tryStartManual();
        g.finish();

        now.set(TEN_MIN - 1);
        assertEquals(Decision.COOLDOWN, g.tryStartManual());

        now.set(TEN_MIN);
        assertEquals(Decision.STARTED, g.tryStartManual());
    }

    @Test
    void scheduledRunIgnoresCooldown() {
        AtomicLong now = new AtomicLong(0);
        CrawlGate g = new CrawlGate(TEN_MIN, now::get);
        g.tryStartManual();
        g.finish();
        assertEquals(Decision.STARTED, g.tryStartScheduled());
    }

    @Test
    void finishReleasesTheGate() {
        CrawlGate g = new CrawlGate(0, new AtomicLong(0)::get);
        g.tryStartScheduled();
        g.finish();
        assertFalse(g.isRunning());
        assertEquals(Decision.STARTED, g.tryStartScheduled());
    }
}
