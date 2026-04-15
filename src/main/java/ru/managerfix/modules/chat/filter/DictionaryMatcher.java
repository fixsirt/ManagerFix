package ru.managerfix.modules.chat.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class DictionaryMatcher {

    public enum MatchType {
        EXACT,
        PATTERN,
        MASKED,
        TYPO
    }

    public static class Match {
        private final String word;
        private final MatchType type;
        private final int distance;

        public Match(String word, MatchType type) {
            this(word, type, 0);
        }

        public Match(String word, MatchType type, int distance) {
            this.word = word;
            this.type = type;
            this.distance = distance;
        }

        public String getWord() { return word; }
        public MatchType getType() { return type; }
        public int getDistance() { return distance; }
    }

    private final Set<String> exactWords;
    private final List<Pattern> rootPatterns;
    private final List<Pattern> maskedPatterns;
    private final int minWordLength;
    private final boolean checkTypos;
    private final int levenshteinMax;

    public DictionaryMatcher(List<String> exactWords,
                           List<String> rootPatterns,
                           List<String> maskedPatterns,
                           int minWordLength,
                           boolean checkTypos,
                           int levenshteinMax) {
        this.exactWords = new HashSet<>();
        if (exactWords != null) {
            for (String word : exactWords) {
                this.exactWords.add(word.toLowerCase());
            }
        }

        this.rootPatterns = new ArrayList<>();
        if (rootPatterns != null) {
            for (String pattern : rootPatterns) {
                try {
                    this.rootPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
                } catch (Exception ignored) {}
            }
        }

        this.maskedPatterns = new ArrayList<>();
        if (maskedPatterns != null) {
            for (String pattern : maskedPatterns) {
                try {
                    this.maskedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
                } catch (Exception ignored) {}
            }
        }

        this.minWordLength = minWordLength;
        this.checkTypos = checkTypos;
        this.levenshteinMax = levenshteinMax;
    }

    public List<Match> findMatches(String normalizedText) {
        List<Match> matches = new ArrayList<>();

        if (normalizedText == null || normalizedText.isEmpty()) {
            return matches;
        }

        String[] words = normalizedText.split("\\s+");

        for (String word : words) {
            if (word.length() < minWordLength) {
                continue;
            }

            // Check exact matches
            if (exactWords.contains(word.toLowerCase())) {
                matches.add(new Match(word, MatchType.EXACT));
                continue;
            }

            // Check root patterns
            for (Pattern pattern : rootPatterns) {
                if (pattern.matcher(word).find()) {
                    matches.add(new Match(word, MatchType.PATTERN));
                    break;
                }
            }

            // Check masked patterns
            for (Pattern pattern : maskedPatterns) {
                if (pattern.matcher(word).find()) {
                    matches.add(new Match(word, MatchType.MASKED));
                    break;
                }
            }

            // Check typos (Levenshtein distance)
            if (checkTypos && word.length() >= minWordLength) {
                for (String profanity : exactWords) {
                    int distance = levenshteinDistance(word, profanity);
                    if (distance > 0 && distance <= levenshteinMax) {
                        matches.add(new Match(word, MatchType.TYPO, distance));
                        break;
                    }
                }
            }
        }

        return matches;
    }

    public boolean hasMatches(String normalizedText) {
        return !findMatches(normalizedText).isEmpty();
    }

    public boolean containsProfanity(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String normalized = text.toLowerCase();

        // Check exact words
        for (String word : exactWords) {
            if (normalized.contains(word)) {
                return true;
            }
        }

        // Check patterns
        for (Pattern pattern : rootPatterns) {
            if (pattern.matcher(normalized).find()) {
                return true;
            }
        }

        // Check masked patterns
        for (Pattern pattern : maskedPatterns) {
            if (pattern.matcher(normalized).find()) {
                return true;
            }
        }

        return false;
    }

    private int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        if (len1 > len2 + levenshteinMax || len2 > len1 + levenshteinMax) {
            return Integer.MAX_VALUE;
        }

        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[len1][len2];
    }
}
