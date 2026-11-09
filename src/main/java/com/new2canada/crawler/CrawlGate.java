package com.new2canada.crawler;

import java.util.function.LongSupplier;

/**
 * Decides whether a crawl may start right now.
 *
 * <p>Two rules protect the rental sites we visit:
 * <ul>
 *   <li>Only one crawl runs at a time.</li>
 *   <li>A <em>manual</em> refresh (any signed-in user pressing the button)
 *       is refused if a crawl started less than {@code cooldownMs} ago. The
 *       scheduled 6-hourly run is not subject to the cooldown.</li>
 * </ul>
 *
 * <p>The clock is injected so the rules can be tested without sleeping.
 */
public final class CrawlGate {

    public enum Decision { STARTED, ALREADY_RUNNING, COOLDOWN, DISABLED }

    private final long cooldownMs;
    private final LongSupplier clock;
    private boolean running = false;
    private long lastStartedAt = Long.MIN_VALUE;

    public CrawlGate(long cooldownMs) {
        this(cooldownMs, System::currentTimeMillis);
    }

    public CrawlGate(long cooldownMs, LongSupplier clock) {
        this.cooldownMs = cooldownMs;
        this.clock = clock;
    }

    /** Called for a user-requested crawl. */
    public synchronized Decision tryStartManual() {
        if (running) return Decision.ALREADY_RUNNING;
        if (lastStartedAt != Long.MIN_VALUE && clock.getAsLong() - lastStartedAt < cooldownMs) {
            return Decision.COOLDOWN;
        }
        return start();
    }

    /** Called for the scheduled crawl; ignores the cooldown but not an active run. */
    public synchronized Decision tryStartScheduled() {
        if (running) return Decision.ALREADY_RUNNING;
        return start();
    }

    /** Must be called when a crawl that was started finishes, successfully or not. */
    public synchronized void finish() {
        running = false;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    private Decision start() {
        running = true;
        lastStartedAt = clock.getAsLong();
        return Decision.STARTED;
    }
}
