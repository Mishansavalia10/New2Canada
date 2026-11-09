package com.new2canada.crawler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Parsed robots.txt rules for one host, as seen by one crawler user-agent.
 *
 * <p>Implements the common subset of the Robots Exclusion Protocol used by
 * major crawlers:
 * <ul>
 *   <li>A group applies to our agent if its {@code User-agent} token is a
 *       case-insensitive prefix of our product token, or is {@code *}. A
 *       named group beats {@code *}.</li>
 *   <li>Among matching {@code Allow}/{@code Disallow} rules the longest path
 *       pattern wins; on a tie {@code Allow} wins.</li>
 *   <li>Paths may use {@code *} (any run of characters) and a trailing
 *       {@code $} (end of path).</li>
 * </ul>
 *
 * <p>This class is pure Java with no I/O, so the crawler's politeness rules
 * can be unit-tested directly. Fetching the file is {@link RobotsCache}'s job.
 */
public final class RobotsPolicy {

    private record Rule(String pattern, boolean allow, Pattern regex) {}

    private final List<Rule> rules;

    private RobotsPolicy(List<Rule> rules) {
        this.rules = rules;
    }

    /** Allows everything: used when a host publishes no robots.txt. */
    public static RobotsPolicy allowAll() {
        return new RobotsPolicy(List.of());
    }

    /**
     * Disallows everything: used when robots.txt exists but cannot be read
     * (server errors, auth walls). Erring on the side of not crawling.
     */
    public static RobotsPolicy disallowAll() {
        return new RobotsPolicy(List.of(new Rule("/", false, compile("/"))));
    }

    /**
     * Parses {@code robotsTxt} for the given product token, e.g.
     * {@code "New2Canada-StudentProject"}.
     */
    public static RobotsPolicy parse(String robotsTxt, String productToken) {
        if (robotsTxt == null || robotsTxt.isBlank()) return allowAll();
        String me = productToken == null ? "" : productToken.toLowerCase(Locale.ROOT);

        // Collect groups: a run of User-agent lines followed by their rules.
        record Group(List<String> agents, List<Rule> rules) {}
        List<Group> groups = new ArrayList<>();
        List<String> agents = new ArrayList<>();
        List<Rule> current = new ArrayList<>();
        boolean readingRules = false;

        for (String raw : robotsTxt.split("\\r?\\n")) {
            String line = stripComment(raw).trim();
            if (line.isEmpty()) continue;
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String field = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();

            switch (field) {
                case "user-agent" -> {
                    if (readingRules) {               // a new group starts here
                        groups.add(new Group(new ArrayList<>(agents), current));
                        agents = new ArrayList<>();
                        current = new ArrayList<>();
                        readingRules = false;
                    }
                    agents.add(value.toLowerCase(Locale.ROOT));
                }
                case "allow", "disallow" -> {
                    readingRules = true;
                    if (agents.isEmpty()) break;      // rule with no group: ignore
                    if (value.isEmpty()) break;       // "Disallow:" with no path = allow all
                    current.add(new Rule(value, field.equals("allow"), compile(value)));
                }
                default -> { /* crawl-delay, sitemap, etc. are ignored */ }
            }
        }
        if (!agents.isEmpty()) groups.add(new Group(agents, current));

        // Prefer a named group that matches us; fall back to "*".
        List<Rule> chosen = null;
        int bestSpecificity = -1;
        for (Group g : groups) {
            for (String agent : g.agents()) {
                if (agent.equals("*")) {
                    if (bestSpecificity < 0) { chosen = g.rules(); bestSpecificity = 0; }
                } else if (!agent.isEmpty() && me.startsWith(agent) && agent.length() > bestSpecificity) {
                    chosen = g.rules();
                    bestSpecificity = agent.length();
                }
            }
        }
        return new RobotsPolicy(chosen == null ? List.of() : List.copyOf(chosen));
    }

    /**
     * True if the crawler may fetch {@code pathAndQuery} (e.g. {@code "/search/apa?page=2"}).
     */
    public boolean isAllowed(String pathAndQuery) {
        String path = (pathAndQuery == null || pathAndQuery.isEmpty()) ? "/" : pathAndQuery;
        if (!path.startsWith("/")) path = "/" + path;

        Rule best = null;
        for (Rule r : rules) {
            if (!r.regex().matcher(path).lookingAt()) continue;
            if (best == null
                    || r.pattern().length() > best.pattern().length()
                    || (r.pattern().length() == best.pattern().length() && r.allow())) {
                best = r;
            }
        }
        return best == null || best.allow();
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash < 0 ? line : line.substring(0, hash);
    }

    /** Turns a robots path pattern into an anchored-at-start regex. */
    static Pattern compile(String pattern) {
        boolean endAnchor = pattern.endsWith("$");
        String body = endAnchor ? pattern.substring(0, pattern.length() - 1) : pattern;
        StringBuilder re = new StringBuilder();
        for (String part : body.split("\\*", -1)) {
            if (re.length() > 0) re.append(".*");
            re.append(Pattern.quote(part));
        }
        if (endAnchor) re.append('$');
        return Pattern.compile(re.toString());
    }
}
