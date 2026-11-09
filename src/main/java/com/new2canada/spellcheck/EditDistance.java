package com.new2canada.spellcheck;

/**
 * Levenshtein edit distance between two strings, computed with the classic
 * Wagner-Fischer dynamic-programming algorithm.
 *
 * <p>An "edit" is one of:
 * <ul>
 *   <li>insert a single character,</li>
 *   <li>delete a single character,</li>
 *   <li>or substitute one character for another.</li>
 * </ul>
 *
 * <p>The DP table {@code dp[i][j]} stores the minimum edits required to
 * transform the first {@code i} characters of {@code a} into the first
 * {@code j} characters of {@code b}.
 *
 * <p><b>Time complexity</b>: O(m·n).
 * <br><b>Space complexity</b>: O(m·n) — kept full so it can be returned and
 * shown in the demo endpoint for didactic purposes.
 *
 * Demonstrates: <b>Dynamic Programming</b>, <b>Edit Distance</b>.
 */
public final class EditDistance {

    private EditDistance() {}

    /** Returns just the distance; ignores the full table. */
    public static int distance(String a, String b) {
        String x = a == null ? "" : a;
        String y = b == null ? "" : b;
        return buildTable(x, y)[x.length()][y.length()];
    }

    /** Returns the full DP table — useful for explaining the algorithm. */
    public static int[][] buildTable(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        int m = a.length();
        int n = b.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) dp[i][0] = i;   // delete all of a's prefix
        for (int j = 0; j <= n; j++) dp[0][j] = j;   // insert all of b's prefix

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];     // free: same character
                } else {
                    int del   = dp[i - 1][j];
                    int ins   = dp[i][j - 1];
                    int subst = dp[i - 1][j - 1];
                    dp[i][j] = 1 + Math.min(subst, Math.min(del, ins));
                }
            }
        }
        return dp;
    }
}
