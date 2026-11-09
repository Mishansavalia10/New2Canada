package com.new2canada.crawler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RobotsPolicyTest {

    private static final String AGENT = "New2Canada-StudentProject";

    @Test
    void emptyOrMissingFileAllowsEverything() {
        assertTrue(RobotsPolicy.parse(null, AGENT).isAllowed("/anything"));
        assertTrue(RobotsPolicy.parse("   ", AGENT).isAllowed("/anything"));
        assertTrue(RobotsPolicy.allowAll().isAllowed("/x"));
    }

    @Test
    void disallowAllBlocksEverything() {
        assertFalse(RobotsPolicy.disallowAll().isAllowed("/"));
        assertFalse(RobotsPolicy.disallowAll().isAllowed("/listings"));
    }

    @Test
    void wildcardGroupAppliesWhenNoNamedGroup() {
        String txt = """
                User-agent: *
                Disallow: /search
                """;
        RobotsPolicy p = RobotsPolicy.parse(txt, AGENT);
        assertFalse(p.isAllowed("/search/apa"));
        assertTrue(p.isAllowed("/apartments"));
    }

    @Test
    void namedGroupOverridesWildcard() {
        String txt = """
                User-agent: *
                Disallow: /

                User-agent: New2Canada
                Disallow: /private
                """;
        RobotsPolicy p = RobotsPolicy.parse(txt, AGENT);
        assertTrue(p.isAllowed("/listings"), "named group should replace the * group");
        assertFalse(p.isAllowed("/private/x"));
    }

    @Test
    void longestMatchWinsAndAllowBreaksTies() {
        String txt = """
                User-agent: *
                Disallow: /rentals
                Allow: /rentals/toronto
                """;
        RobotsPolicy p = RobotsPolicy.parse(txt, AGENT);
        assertFalse(p.isAllowed("/rentals/calgary"));
        assertTrue(p.isAllowed("/rentals/toronto/listing-1"));

        String tie = """
                User-agent: *
                Disallow: /x
                Allow: /x
                """;
        assertTrue(RobotsPolicy.parse(tie, AGENT).isAllowed("/x"));
    }

    @Test
    void wildcardAndEndAnchorPatterns() {
        String txt = """
                User-agent: *
                Disallow: /*.json$
                Disallow: /api/*/private
                """;
        RobotsPolicy p = RobotsPolicy.parse(txt, AGENT);
        assertFalse(p.isAllowed("/data/file.json"));
        assertTrue(p.isAllowed("/data/file.json?x=1"), "$ anchors the end of the path");
        assertFalse(p.isAllowed("/api/v2/private"));
        assertTrue(p.isAllowed("/api/v2/public"));
    }

    @Test
    void commentsAndUnknownFieldsAreIgnored() {
        String txt = """
                # polite crawlers only
                User-agent: *   # everyone
                Crawl-delay: 10
                Disallow: /admin # internal
                Sitemap: https://example.com/sitemap.xml
                """;
        RobotsPolicy p = RobotsPolicy.parse(txt, AGENT);
        assertFalse(p.isAllowed("/admin"));
        assertTrue(p.isAllowed("/"));
    }

    @Test
    void emptyDisallowMeansAllowAll() {
        String txt = """
                User-agent: *
                Disallow:
                """;
        assertTrue(RobotsPolicy.parse(txt, AGENT).isAllowed("/anything"));
    }
}
