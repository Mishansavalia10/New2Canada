package com.new2canada.config;

import java.util.List;

/**
 * Centralised configuration constants.
 *
 * <p>Everything here is a plain {@code public static final} value so the whole
 * codebase reads from one place. No environment-variable hot-reloading — this
 * is a final project, not production infra.
 */
public final class AppConfig {

    private AppConfig() {}

    /** HTTP port the embedded web server listens on. */
    public static final int HTTP_PORT = 8080;

    /** Path (relative to working dir) where the Firebase service-account key is expected. */
    public static final String SERVICE_ACCOUNT_KEY_PATH = "serviceAccountKey.json";

    /** Polite delay between live HTTP requests, in milliseconds. */
    public static final long FETCH_DELAY_MS = 1500L;

    /**
     * How long to wait for a JS-rendered listings grid to appear before
     * giving up on that seed. The map-based sites (4Rent, PadMapper) build a
     * WebGL map first and only then fetch listings, so they routinely need
     * more than a couple of seconds.
     */
    public static final long RENDER_TIMEOUT_MS = 30_000L;

    /**
     * Cap on the initial page load. Generous because the software-WebGL map
     * sites are slow to parse; the crawler spends most of its wait in
     * {@link #RENDER_TIMEOUT_MS} instead, waiting for listings specifically.
     */
    public static final long PAGE_LOAD_TIMEOUT_MS = 45_000L;

    /**
     * Live crawling is on unless {@code NEW2CANADA_CRAWL=off}. Turn it off to
     * demo or develop from the cached data without touching the rental sites.
     */
    public static final boolean CRAWL_ENABLED =
            !"off".equalsIgnoreCase(System.getenv("NEW2CANADA_CRAWL"));

    /** Minimum gap between user-requested crawls, in milliseconds. */
    public static final long MANUAL_CRAWL_COOLDOWN_MS = 10 * 60_000L;

    /**
     * User-Agent the crawler announces itself with. Site operators can use
     * the contact in it to reach us; set {@code NEW2CANADA_CONTACT} to a real
     * email or URL before running the crawler.
     */
    public static final String USER_AGENT =
            "New2Canada-StudentProject/1.0 (COMP 8547 course project; contact: "
            + (System.getenv("NEW2CANADA_CONTACT") == null || System.getenv("NEW2CANADA_CONTACT").isBlank()
                    ? "not-configured" : System.getenv("NEW2CANADA_CONTACT")) + ")";

    /** How often the background crawler refreshes data, in minutes. */
    public static final long CRAWL_INTERVAL_MIN = 360L; // 6h

    /** Max number of listings kept per category. */
    public static final int MAX_ITEMS_PER_TYPE = 2000;

    /** Hard ceiling per seed URL (otherwise Craigslist/JobBank dominate one city). */
    public static final int MAX_ITEMS_PER_SEED = 20;

    /** Default top-k for autocomplete suggestions. */
    public static final int AUTOCOMPLETE_LIMIT = 10;

    /** Default top-k for spell-correction suggestions. */
    public static final int SPELLCHECK_LIMIT = 5;

    /**
     * Seed URLs for the apartment crawler — exactly one per supported rental
     * site, each already scoped to a specific Canadian city. Each seed has a
     * matching {@link com.new2canada.parser.rentals.RentalExtractor} that
     * knows that site's DOM and the city/province it targets.
     */
    public static final List<String> APARTMENT_SEEDS = List.of(
            "https://www.kijiji.ca/b-apartments-condos/canada/c37l0",
            "https://montreal.craigslist.org/search/apa",
            "https://www.zumper.com/apartments-for-rent/calgary-ab",
            "https://www.padmapper.com/apartments/ottawa-on",
            // /Listings, not /rentals/toronto: the latter is a landing page whose
            // only listings are five featured promo tiles.
            "https://www.viewit.ca/Listings?cid=14",
            "https://4rent.ca/apartments-for-rent/ab/edmonton/3352",
            "https://www.rentseeker.ca/rentals/apartments/manitoba/winnipeg",
            "https://www.realtor.ca/on/waterloo/rentals",
            "https://liv.rent/rental-listings/city/vancouver",
            "https://rentola.ca/for-rent?location=halifax"
    );

}
